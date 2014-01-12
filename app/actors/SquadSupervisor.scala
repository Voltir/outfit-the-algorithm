package actors

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.Actor
import akka.channels._
import models._
import scala.concurrent.duration.Duration

sealed trait SquadCommand
case class RemoveMember(cid: CharacterId) extends SquadCommand
case class UnassignMember(cid: CharacterId) extends SquadCommand
case class CreateSquad(cid: CharacterId) extends SquadCommand
case class JoinSpecificSquad(cid: CharacterId, sid: Integer) extends SquadCommand
case object ResetSquads extends SquadCommand
case object RandomizeLeader extends SquadCommand
case class MakeLeader(cid: CharacterId) extends SquadCommand
case object InactivityCheck extends SquadCommand
case class SetActivity(cid: CharacterId, active: Boolean) extends SquadCommand
case class SetSquadResources(resources: Map[CharacterId,Resources]) extends SquadCommand
case class SetSquadType(stype: SquadType, cid: CharacterId) extends SquadCommand

trait SquadResult
case class NewMemberResult(success: Boolean) extends SquadResult
//case class SquadDataResult(squad: Option[Squad], online: List[CharacterId])

trait SquadSupervisorRequest
case class RemovedByInactivity(cid: CharacterId) extends SquadSupervisorRequest

class SquadActor(algo: ChannelRef[(AlgoRequest,Nothing) :+: TNil]) extends Actor 
  with Channels[TNil,(AlgoRequest,AlgoResult) :+: (SquadCommand,Nothing) :+: TNil] {

  //var squad: Option[Squad] = None
  val squads = new Squads()

  var resources: Map[CharacterId,Resources] = Map()
  //Used to auto remove members from the squad
  var activity: Map[CharacterId,String] = Map()

  channel[AlgoRequest] {

    /* TODO -- Remove
    case (GetSquadData,snd) => {
      val online = squad.map {
        _.members.filter(m => activity.get(m.id) == Some("ACTIVE")).map(_.id)
      }.getOrElse(Set.empty[CharacterId])
      snd <-!- SquadDataResult(squad,online,resources)
    }
    */
    case (GetSquads,snd) => {
      val online = activity.foldLeft(Set.empty[CharacterId]) { case (acc,elem) =>
        if(elem._2 == "ACTIVE") acc + elem._1
        else acc
      }
      snd <-!- SquadsResult(squads,online,resources)
    }

    /* TODO -- Remove
    case (JoinSquad(mem: Member),snd) => {
      squad = squad.map { s =>
        if(s.members.size < 12)  {
          val new_squad = s.place(mem)
          s.members.foreach { chk =>
            if(new_squad.getAssignment(chk.id) != s.getAssignment(chk.id)) {
              new_squad.getAssignment(chk.id).foreach { assignment =>
                algo <-!- RoleChange(chk.id,assignment)
              }
            }
          }
          new_squad.getAssignment(mem.id).foreach { assignment => algo <-!- RoleChange(mem.id,assignment) }
          snd <-!- JoinSquadResult(true)
          Some(new_squad)
        } else {
          snd <-!- JoinSquadResult(false)
          Some(s)
        }
      }.getOrElse {
        snd <-!- JoinSquadResult(true)
        val new_squad = Squad.make(SquadTypes.STANDARD,0,mem)
        new_squad.getAssignment(mem.id).foreach { assignment =>
          algo <-!- RoleChange(mem.id,assignment)
        }
        Some(new_squad)
      }
    }
    */

    case (JoinSquad(mem: Member),snd) => {
      val role_changes = squads.assign(mem)
      role_changes.foreach { cid =>
        squads.getAssignment(cid).foreach { assignment =>
          algo <-!-RoleChange(cid,assignment)
        }
      }
      snd <-!- JoinSquadResult(true)
      //TODO??: if squads.unassigned(mem.id) != None algo <-!- Unassigned(cid)
    }
    case _ => { println("SQUAD ACTOR GOT SOME CRAZY ALGO REQUEST HALP") }
  }

  channel[SquadCommand] {
    
    case (InactivityCheck,snd) => {
      /*squad = squad.map { s => s.members.foldLeft(s){ case (acc,mem) =>
        if(activity.get(mem.id) == Some("INACTIVE")) { println(s"REMOVING $mem") ;acc.remove(mem.id) }
        else acc
      }}*/
      activity.foreach { case (mem,a) => 
        if(a == "INACTIVE") {
          squads.remove(mem).foreach { cid =>
            squads.getAssignment(cid).foreach { assignment =>
              algo <-!-RoleChange(cid,assignment)
            }
          }
        }
      }
    }
    
    case (ResetSquads,snd) => squads.reset()

    /*
    case (RemoveMember(cid),snd) => squad = squad.flatMap { s =>
      if(s.members.size > 1) {
        val new_squad = s.remove(cid)
        s.members.filter(_.id != cid).foreach { chk =>
          if(new_squad.getAssignment(chk.id) != s.getAssignment(chk.id)) {
            new_squad.getAssignment(chk.id).foreach { assignment =>
              algo <-!- RoleChange(chk.id,assignment)
            }
          }
        }
        Some(new_squad)
      }
      else None
    }*/
    case (CreateSquad(cid),snd) => {
      val role_changes = squads.createSquad(cid)
      role_changes.foreach { cid =>
        squads.getAssignment(cid).foreach { assignment =>
          algo <-!-RoleChange(cid,assignment)
        }
      }
    }

    case (RemoveMember(cid),snd) => {
      val role_changes = squads.remove(cid)
      role_changes.foreach { cid =>
        squads.getAssignment(cid).foreach { assignment =>
          algo <-!-RoleChange(cid,assignment)
        }
      }
    }

    case (UnassignMember(cid),snd) => {
      val role_changes = squads.unassign(cid)
      role_changes.foreach { cid =>
        squads.getAssignment(cid).foreach { assignment =>
          algo <-!-RoleChange(cid,assignment)
        }
      }
    }

    case (JoinSpecificSquad(cid,sid),snd) => {
      squads.unassign(cid)
      val role_changes = squads.joinSpecific(cid,sid)
      role_changes.foreach { cid =>
        squads.getAssignment(cid).foreach { assignment =>
          algo <-!-RoleChange(cid,assignment)
        }
      }
    }
    /* TODO
    case (RandomizeLeader,snd) => {
      import scala.util.Random
      squad = squad.map { old =>
        val new_leader = Random.shuffle(old.members).head
        old.copy(leader=new_leader)
      }
    }
    */

    /* TODO -- Remove
    case (MakeLeader(cid),snd) => {
      squad = squad.map { old =>
        val new_leader = old.members.find(_.id == cid).getOrElse(old.leader)
        old.copy(leader=new_leader)
      }
    }
    */
    case (MakeLeader(cid),snd) => squads.makeLeader(cid)

    /* TODO -- Remove
    case (SetSquadType(stype,cid),snd) => {
      squad = squad.map { old =>
        if(old.leader.id == cid) {
          val result = old.copy(stype=stype,assignments=Squad.doAssignments(stype,old.leader,old.members,old.joined))
          result.members.foreach { mem => 
            result.getAssignment(mem.id).foreach { assignment =>
              algo <-!- RoleChange(mem.id,assignment) 
            }
          }
          result
        }
        else {
          old
        }
      }
    }
    */
    case (SetSquadType(stype,cid),snd) => {
      val role_changes = squads.setSquadType(stype,cid)
      role_changes.foreach { cid =>
        squads.getAssignment(cid).foreach { assignment =>
          algo <-!-RoleChange(cid,assignment)
        }
      }
    }
    /*TODO -- Remove
    case (SetActivity(cid: CharacterId, active: Boolean),snd) => {
      if(active) activity += (cid -> "ACTIVE")
      else {
        if(squad.exists(_.members.exists(_.id == cid))) { activity += (cid -> "INACTIVE") }
        else activity += (cid -> "NOT_PARTICIPATING")
        context.system.scheduler.scheduleOnce(Duration(180,"seconds"), self, InactivityCheck)
      }
    }
    */
    case (SetActivity(cid: CharacterId, active: Boolean),snd) => {
      if(active) activity += (cid -> "ACTIVE")
      else {
        if(squads.tracking(cid)) { activity += (cid -> "INACTIVE") }
        else activity += (cid -> "NOT_PARTICIPATING")
        context.system.scheduler.scheduleOnce(Duration(180,"seconds"), self, InactivityCheck)
      }
    }

    case (SetSquadResources(r),snd) => resources = r
  }
}

class SquadSupervisor extends Actor with Channels[TNil,TNil] {

}
