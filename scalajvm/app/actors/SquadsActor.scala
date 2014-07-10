package actors

import akka.actor._
import play.api.Play.current
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.{Map => MutableMap, ArrayBuffer, Set => MutableSet}
import shared.models._
import shared.commands._
import rx._

case class State(character: Character, location: Option[CharacterId])

case class JoinSquadAkka(lid: CharacterId, memId: CharacterId)
case class DisbandSquadAkka(lid: CharacterId)
case class UnassignSelfAkka(cid: CharacterId)

class SquadsActor extends Actor {

  val playerRefs: MutableSet[ActorRef] = MutableSet.empty

  val squads: MutableMap[CharacterId,Var[Squad]] = MutableMap.empty

  val players: MutableMap[CharacterId,State] = MutableMap.empty 

  def todoAssign(squad: Squad, player: Character): Option[AssignedRole] = {
    if(squad.roles.size < 12) {
      val all = (0 until 12).toSet
      val used = squad.roles.map(_.idx).toSet
      val avail = all.diff(used)
      println(s"Available roles: ${(avail)}")
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

  private def removeFromOldSquad(state: State): Boolean = {
    if(state.location != None) {
      squads.get(state.location.get).foreach { oldSquad =>
        oldSquad() = oldSquad().copy(roles = oldSquad().roles.filter(_.character.cid != state.character.cid))
      }
      true
    } else {
      false
    }
  }
  private def moveToSquad(lid: CharacterId, mid: CharacterId) = {
    var wasUnassigned = true
    players.get(mid).map { player =>
      wasUnassigned  = !removeFromOldSquad(player)
      squads.get(lid).map { squad =>
        val assignment = todoAssign(squad(),player.character) //<------ Update this here
        assignment.map { a =>
          squad() = squad().copy(roles = a :: squad().roles)
          players.put(mid,player.copy(location=Option(lid)))
          if(wasUnassigned) { updateUnassigned() }
        } getOrElse {
          players.put(mid,player.copy(location=None))
          updateUnassigned()
        }
      }
    }
  }

  def receive = {

    case Terminated(ref) => {
      playerRefs -= ref
    }

    case NewPlayer(ref: ActorRef, character: Character) => {
      context.watch(ref)
      playerRefs += ref
      players.put(character.cid,State(character,None))
      updateUnassigned()
    }

    case CreateSquad(leader,pattern,pref) => {
      def squadObs(s: Var[Squad]) = {
        Obs(s) {
          println(s"OBS CHECK -- ${leader}'s Squad Changed")
          playerBroadcast(SquadUpdate(s.now))
        }
      }
      val newSquad: Var[Squad] = Var(Squad(leader,pref,pattern,List(AssignedRole(0,leader))))
      squads.put(leader.cid,newSquad)
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

    //case Move(start: CharacterId, end: CharacterId, player: Character) => {
    //}

    case DisbandSquadAkka(cid: CharacterId) => {
      squads.get(cid).map { squad =>
        squad().roles.map(_.character.cid).foreach { cid =>
          players.get(cid).map { state =>
            players.put(cid,state.copy(location=None))
          }
        }
      }
      squads.remove(cid)
      playerBroadcast(LoadInitialResponse(squads.toList.map(_._2.now),unassigned()))
    }

    case LoadInitial => {
      sender() ! LoadInitialResponse(squads.toList.map(_._2.now),unassigned())
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
