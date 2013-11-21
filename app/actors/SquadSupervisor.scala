package actors

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.Actor
import akka.channels._
import models._
import fakedata.FakeSquadType
import scala.concurrent.duration.Duration

sealed trait SquadCommand
case object ResetSquad extends SquadCommand
case object RandomizeLeader extends SquadCommand
case object InactivityCheck extends SquadCommand
case class SetActivity(cid: CharacterId, active: Boolean) extends SquadCommand

trait SquadResult
case class NewMemberResult(success: Boolean) extends SquadResult
//case class SquadDataResult(squad: Option[Squad], online: List[CharacterId])

trait SquadSupervisorRequest
case class RemovedByInactivity(cid: CharacterId) extends SquadSupervisorRequest

class SquadActor extends Actor with Channels[TNil,(AlgoRequest,AlgoResult) :+: (SquadCommand,Nothing) :+: TNil] {

  var squad: Option[Squad] = None
  //Used to auto remove members from the squad
  var inactive: Set[CharacterId] = Set.empty

  channel[AlgoRequest] {
    case (GetSquadData,snd) => {
      val online = squad.map {_.members.filter(m => inactive.contains(m.id)).map(_.id)}.getOrElse(List.empty)
      snd <-!- SquadDataResult(squad,online)
    }
    case (JoinSquad(mem: MemberDetail),snd) => {
      println(s"Want to join: $mem")
      squad.map { s =>
        if(s.members.size < 12)  {
          squad = Some(s.place(mem))
          snd <-!- JoinSquadResult(true)
        } else {
          snd <-!- JoinSquadResult(false)
        }
      }.getOrElse {
        squad = Some(Squad.make(FakeSquadType(),mem))
        snd <-!- JoinSquadResult(true)
      }
    }
    case _ => { println("SQUAD ACTOR GOT SOME CRAZY ALGO REQUEST HALP") }
  }

  channel[SquadCommand] {
    case (InactivityCheck,snd) => {
      squad = inactive.foldLeft(squad){ case (acc,cid) =>
        acc.map { s =>
          //parentChannel <-!- RemovedByInactivity(cid)
          s.remove(cid)
        }
      }
    }

    case (ResetSquad,snd) => squad = None

    case (RandomizeLeader,snd) => {
      import scala.util.Random
      squad = squad.map { old =>
        println("CHANGE!")
        val new_leader = Random.shuffle(old.members).head
        old.copy(leader=new_leader)
      }
    }

    case (SetActivity(cid: CharacterId, active: Boolean),snd) => {
      if(active) inactive = inactive - cid
      else {
        inactive = inactive + cid
        context.system.scheduler.scheduleOnce(Duration(120,"seconds"), self, InactivityCheck)
      }
    }
  }
}

class SquadSupervisor extends Actor with Channels[TNil,TNil] {

}
