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

class SquadActor(algo: ChannelRef[(AlgoRequest,Nothing) :+: TNil]) extends Actor 
  with Channels[TNil,(AlgoRequest,AlgoResult) :+: (SquadCommand,Nothing) :+: TNil] {

  var squad: Option[Squad] = None
  //Used to auto remove members from the squad
  var activity: Map[CharacterId,String] = Map()

  channel[AlgoRequest] {
    case (GetSquadData,snd) => {
      val online = squad.map {_.members.filter(m => activity.get(m.id) == Some("ACTIVE")).map(_.id)}.getOrElse(List.empty)
      snd <-!- SquadDataResult(squad,online)
    }
    case (JoinSquad(mem: MemberDetail),snd) => {
      println(s"Want to join: $mem")
      squad = squad.map { s =>
        if(s.members.size < 12)  {
          val new_squad = s.place(mem)
          s.members.foreach { chk =>
            if(new_squad.getRole(chk.id) != s.getRole(chk.id)) {
              println(s"Send new role alert! $chk")
              new_squad.getRole(chk.id).foreach { role =>
                algo <-!- RoleChange(chk.id,role)
              }
            }
          }
          println(s"Send new role alert! $mem")
          new_squad.getRole(mem.id).foreach { role =>
            algo <-!- RoleChange(mem.id,role)
          }
          snd <-!- JoinSquadResult(true)
          Some(new_squad)
        } else {
          snd <-!- JoinSquadResult(false)
          Some(s)
        }
      }.getOrElse {
        snd <-!- JoinSquadResult(true)
        val new_squad = Squad.make(FakeSquadType(),mem)
        new_squad.getRole(mem.id).foreach { role =>
          println(s"Send new role alert! $mem")
          algo <-!- RoleChange(mem.id,role)
        }
        Some(new_squad)
      }
    }
    case _ => { println("SQUAD ACTOR GOT SOME CRAZY ALGO REQUEST HALP") }
  }

  channel[SquadCommand] {
    case (InactivityCheck,snd) => {
      println("STARTED INACTIVE CHECK!!!!!!!!!!");
      squad = squad.map { s => s.members.foldLeft(s){ case (acc,mem) =>
        //parentChannel <-!- RemovedByInactivity(cid)
        if(activity.get(mem.id) == Some("INACTIVE")) { println(s"REMOVING $mem") ;acc.remove(mem.id) }
        else acc
      }}
      println(s"SQUAD MEMBERS ARE -- ${squad.get.members}")
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
      if(active) activity += (cid -> "ACTIVE")
      else {
        if(squad.exists(_.members.exists(_.id == cid))) { println("Set Inactive") ; activity += (cid -> "INACTIVE") }
        else activity += (cid -> "NOT_PARTICIPATING")
        context.system.scheduler.scheduleOnce(Duration(30,"seconds"), self, InactivityCheck)
      }
    }
  }
}

class SquadSupervisor extends Actor with Channels[TNil,TNil] {

}
