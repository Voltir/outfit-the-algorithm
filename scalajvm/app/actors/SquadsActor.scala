package actors

import akka.actor._
import play.api.Play.current
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.{Map => MutableMap, ArrayBuffer, Set => MutableSet}
import shared.models._
import shared.commands._
import rx._
import squad.{Preference, Assign, Context}

case class State(character: Character, location: Option[CharacterId])

case class JoinSquadAkka(lid: CharacterId, memId: CharacterId)
case class DisbandSquadAkka(lid: CharacterId)
case class UnassignSelfAkka(cid: CharacterId)
case class SetPatternAkka(lid: CharacterId, pattern: Pattern)
case class LoadInitialAkka(cid: CharacterId, pref: PreferenceDefinition)
case class AddPinAkka(cid: CharacterId, pin: PinAssignment)
case class RemovePinAkka(cid: CharacterId, lid: CharacterId, pattern: String)

case class Pin(mid: CharacterId, pattern: String, idx: Int)

class SquadsActor extends Actor {

  val playerRefs: MutableSet[ActorRef] = MutableSet.empty

  val squads: MutableMap[CharacterId,Var[Squad]] = MutableMap.empty

  val players: MutableMap[CharacterId,State] = MutableMap.empty 

  val pins: MutableMap[CharacterId,List[Pin]] = MutableMap.empty

  val preferences: MutableMap[CharacterId,Preference] = MutableMap.empty withDefaultValue(Preference(Map.empty.withDefaultValue(0)))

  def todoAssign(squad: Squad, player: Character): Option[AssignedRole] = {
    if(squad.roles.size < 12) {
      val all = (0 until 12).toSet
      val used = squad.roles.map(_.idx).toSet
      val avail = all.diff(used)
      avail.toList.headOption.map { idx => AssignedRole(idx,player) }
    } else {
      None
    }
  }

  def unassigned() = {
    players.toList.filter(_._2.location == None).map(_._2.character)
  }

  def updateUnassigned() = {
    playerBroadcast(Unassigned(unassigned()))
  }

  private def assignSquad(lid: CharacterId, squad: Squad, update: Pattern): Unit = {
    val ctx = Context(
      update=update,
      pins=pins
        .getOrElse(lid,List.empty)
        .filter(pin => pin.pattern == update.name)
        .foldLeft(Map.empty[CharacterId,Int]){ case (acc,pin) => acc + (pin.mid -> pin.idx)},
      preference=preferences
    )
    squads.get(lid).map { prev =>
      prev() = Assign(squad, ctx)
    }
  }

  private def removeFromOldSquad(state: State): Boolean = {
    if(state.location != None) {
      squads.get(state.location.get).foreach { oldSquad =>
        if(oldSquad().leader.cid == state.character.cid) {
          self ! DisbandSquadAkka(state.character.cid)
        } else {
          val updated = oldSquad().copy(roles = oldSquad().roles.filter(_.character.cid != state.character.cid))
          if (updated.roles.isEmpty) {
            squads.remove(updated.leader.cid)
            pins.remove(updated.leader.cid)
            playerBroadcast(LoadInitialResponse(squads.toList.map(_._2.now), unassigned()))
          } else {
            assignSquad(oldSquad().leader.cid,updated,updated.pattern)
          }
        }
      }
      true
    } else {
      false
    }
  }
  private def moveToSquad(lid: CharacterId, mid: CharacterId) = {
    var modifiedUnassigned = true
    players.get(mid).map { player =>
      modifiedUnassigned  = !removeFromOldSquad(player)
      squads.get(lid).map { squad =>
        val assignment = todoAssign(squad(),player.character) //<------ Update this here
        assignment.map { a =>
          assignSquad(lid,squad().copy(roles = a :: squad().roles),squad().pattern)
          players.put(mid,player.copy(location=Option(lid)))
        } getOrElse {
          players.put(mid,player.copy(location=None))
          modifiedUnassigned = true
        }
      }
      if(modifiedUnassigned) { updateUnassigned() }
    }
  }

  def receive = {

    case Terminated(ref) => {
      playerRefs -= ref
      val cid = CharacterId(ref.path.name)
      players.get(cid).map { player =>
        removeFromOldSquad(player)
        players.remove(cid)
        updateUnassigned()
      }
    }

    case NewPlayer(ref: ActorRef, character: Character) => {
      context.watch(ref)
      playerRefs += ref
      if(!players.contains(character.cid)) {
        players.put(character.cid,State(character,None))
        updateUnassigned()
      }
    }

    case CreateSquad(leader,pattern,pref) => {
      def squadObs(s: Var[Squad]) = {
        Obs(s) {
          println(s"OBS CHECK -- ${leader}'s Squad Changed")
          playerBroadcast(SquadUpdate(s.now))
        }
      }
      players.get(leader.cid).map { player =>
        removeFromOldSquad(player)
      }
      val newSquad: Var[Squad] = Var(Squad(leader,pref,pattern,List(AssignedRole(0,leader))))
      squads.put(leader.cid,newSquad)
      players.get(leader.cid).foreach { p =>
        players.put(p.character.cid,p.copy(location=Option(leader.cid)))
        updateUnassigned()
      }
      squadObs(newSquad)
    }

    case JoinSquadAkka(lid,mid) => {
      moveToSquad(lid,mid)
    }

    case UnassignSelfAkka(cid: CharacterId) => {
      players.get(cid).map { player =>
        if(removeFromOldSquad(player)) {
          players.put(cid,player.copy(location=None))
          updateUnassigned()
        }
      }
    }

    case DisbandSquadAkka(lid: CharacterId) => {
      squads.get(lid).map { squad =>
        squad().roles.map(_.character.cid).foreach { cid =>
          players.get(cid).filter(_.location == Some(lid)).map { state =>
            players.put(cid,state.copy(location=None))
          }
        }
      }
      squads.remove(lid)
      pins.remove(lid)
      playerBroadcast(LoadInitialResponse(squads.toList.map(_._2.now),unassigned()))
    }

    case SetPatternAkka(lid,pattern) => {
      squads.get(lid).map { squad =>
        assignSquad(lid,squad(),pattern)
      }
    }

    case LoadInitialAkka(cid,pref) => {
      val elem = pref.values.foldLeft(Map.empty[Pattern.Role,Int].withDefaultValue(0)) { case (acc,(role,score)) =>
        acc + (role->score)
      }
      preferences.put(cid,Preference(elem))
      sender() ! LoadInitialResponse(squads.toList.map(_._2.now),unassigned())
    }

    case AddPinAkka(cid,pin) => {
      if(pin.assignment >= 0 && pin.assignment < 12) {
        val updated = Pin(cid,pin.pattern,pin.assignment) :: pins.getOrElse(pin.lid,List.empty).filter(pin => pin.mid!=cid)
        pins.put(pin.lid, updated)
        squads.get(pin.lid).foreach { squad =>
          assignSquad(pin.lid,squad(),squad().pattern)
        }
      }
    }

    case RemovePinAkka(cid,lid,pattern) => {
      val updated = pins.getOrElse(lid,List.empty).filter(pin => pin.mid==cid && pin.pattern==pattern)
      pins.put(lid,updated)
      squads.get(lid).foreach { squad =>
        assignSquad(lid,squad(),squad().pattern)
      }
    }

    case TestIt => {
      if(squads.size == 0) {
        Squad.fake.foreach { s =>
          self ! CreateSquad(s.leader,DefaultPatterns.basic, Squad.InfantryPreference)
        }
      } else {
        val fakeid = squads.toList.head._1
        val squad = squads(fakeid)
        squad() = squad().copy(roles = List(AssignedRole(0,Character(CharacterId("fake"),"Fakerson"))))
        }
    }
  }

  def playerBroadcast(response: shared.commands.Response) = {
    playerRefs.foreach { ref => 
      ref ! response
    }
  }
}
