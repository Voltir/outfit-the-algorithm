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
case class SetPatternAkka(requster: CharacterId, pattern: Pattern, lid: CharacterId)
case class LoadInitialAkka(cid: CharacterId, pref: PreferenceDefinition)
case class AddPinAkka(cid: CharacterId, pin: PinAssignment)
case class RemovePinAkka(cid: CharacterId, lid: CharacterId, pattern: String)
case class SetPreferenceAkka(cid: CharacterId, pref: PreferenceDefinition)
case class VolunteerFCAkka(fc: Character)
case class StepDownFCAkka(fc: Character)
case class MakeLeaderAkka(requestor: CharacterId, lid: CharacterId, target: CharacterId)

case class Pin(mid: CharacterId, pattern: String, idx: Int)

class SquadsActor extends Actor {

  var forceCommander: Option[Character] = None

  val playerRefs: MutableSet[ActorRef] = MutableSet.empty

  val squads: MutableMap[CharacterId,(Var[Squad],Obs)] = MutableMap.empty

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
    squads.get(lid).map { case (prev,_) =>
      prev() = Assign(squad, ctx)
    }
  }

  private def removeFromOldSquad(state: State): Boolean = {
    if(state.location != None) {
      squads.get(state.location.get).foreach { case (oldSquad,_) =>
        if(oldSquad().leader.cid == state.character.cid) {
          self ! DisbandSquadAkka(state.character.cid)
        } else {
          val updated = oldSquad().copy(roles = oldSquad().roles.filter(_.character.cid != state.character.cid))
          if (updated.roles.isEmpty) {
            squads.remove(updated.leader.cid)
            pins.remove(updated.leader.cid)
            playerBroadcast(LoadInitialResponse(squads.toList.map(_._2._1.now), unassigned(),forceCommander))
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
      squads.get(lid).map { case (squad,_) =>
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
      def squadObs(s: Var[Squad]): Obs = {
        Obs(s) {
          playerBroadcast(SquadUpdate(s.now))
        }
      }
      players.get(leader.cid).map { player =>
        removeFromOldSquad(player)
      }
      val newSquad: Var[Squad] = Var(Squad(leader,pref,pattern,List(AssignedRole(0,leader))))
      squads.put(leader.cid,(newSquad,squadObs(newSquad)))
      players.get(leader.cid).foreach { p =>
        players.put(p.character.cid,p.copy(location=Option(leader.cid)))
        updateUnassigned()
      }
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
      squads.get(lid).map { case (squad,_) =>
        squad().roles.map(_.character.cid).foreach { cid =>
          players.get(cid).filter(_.location == Some(lid)).map { state =>
            players.put(cid,state.copy(location=None))
          }
        }
      }
      squads.remove(lid)
      pins.remove(lid)
      playerBroadcast(LoadInitialResponse(squads.toList.map(_._2._1.now),unassigned(),forceCommander))
    }

    case SetPatternAkka(requester,pattern,lid) => {
      println("SET PATTERN SQUAD RECEIVED!")
      squads.get(lid).map { case (squad,_) =>
        if(requester == lid || Option(requester) == forceCommander.map(_.cid)) {
          assignSquad(lid, squad(), pattern)
        }
      }
    }

    case LoadInitialAkka(cid,pref) => {
      val elem = pref.values.foldLeft(Map.empty[Pattern.Role,Int].withDefaultValue(0)) { case (acc,pref) =>
        acc + (pref.role->pref.score)
      }
      preferences.put(cid,Preference(elem))
      sender() ! LoadInitialResponse(squads.toList.map(_._2._1.now),unassigned(),forceCommander)
    }

    case SetPreferenceAkka(cid,pref) => {
      val elem = pref.values.foldLeft(Map.empty[Pattern.Role,Int].withDefaultValue(0)) { case (acc,pref) =>
        acc + (pref.role->pref.score)
      }
      preferences.put(cid,Preference(elem))
      players.get(cid).map { state =>
        state.location.foreach { lid =>
          squads.get(lid).foreach { case (squad,_) =>
            assignSquad(lid,squad(),squad().pattern)
          }
        }
      }
    }

    case AddPinAkka(cid,pin) => {
      if(pin.assignment >= 0 && pin.assignment < 12) {
        val updated = Pin(cid,pin.pattern,pin.assignment) :: pins.getOrElse(pin.lid,List.empty).filter(pin => pin.mid!=cid)
        pins.put(pin.lid, updated)
        squads.get(pin.lid).foreach { case (squad,_) =>
          assignSquad(pin.lid,squad(),squad().pattern)
        }
      }
    }

    case RemovePinAkka(cid,lid,pattern) => {
      val updated = pins.getOrElse(lid,List.empty).filter(pin => !(pin.mid==cid && pin.pattern==pattern))
      pins.put(lid,updated)
      squads.get(lid).foreach { case (squad,_) =>
        assignSquad(lid,squad(),squad().pattern)
      }
    }

    case VolunteerFCAkka(fc) => {
      if(forceCommander.isEmpty) {
        forceCommander = Option(fc)
        playerBroadcast(UpdateFC(Some(fc)))
      }
    }

    case StepDownFCAkka(fc) => {
      forceCommander.foreach { current =>
        if(current.cid == fc.cid) {
          forceCommander = None
          playerBroadcast(UpdateFC(None))
        }
      }
    }

    case MakeLeaderAkka(requestor,lid,target) => {
      if(lid != target && (requestor == lid || Some(requestor) == forceCommander.map(_.cid))) {
        squads.remove(lid).map { case (squad,obs) =>
          pins.remove(lid).foreach { p =>
            pins.put(target, p)
          }
          squad.now.roles.find(_.character.cid == target).map(_.character).map { newLeader =>
            squad.updateSilent(squad.now.copy(leader=newLeader))
            squads.put(newLeader.cid,(squad,obs))
            squad.now.roles.foreach { role =>
              players.get(role.character.cid).map { state =>
                players.put(role.character.cid,state.copy(location = Option(newLeader.cid)))
              }
            }
          }.getOrElse {
            squad.now.roles.foreach { role =>
              players.get(role.character.cid).map { state =>
                players.put(role.character.cid,state.copy(location = None))
              }
            }
            updateUnassigned()
          }
        }
      }
      playerBroadcast(LoadInitialResponse(squads.toList.map(_._2._1.now),unassigned(),forceCommander))
    }
  }

  def playerBroadcast(response: shared.commands.Response) = {
    playerRefs.foreach { ref => 
      ref ! response
    }
  }
}
