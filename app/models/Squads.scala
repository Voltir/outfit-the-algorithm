package models

import org.joda.time.DateTime

class Squads {
  var squads: Set[Squad] = Set()
  var unassigned: Map[Member,DateTime] = Map()
  var assignments: Map[CharacterId,Int] = Map()

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  def availableSquads = squads.filter(!_.full).size

  def bestSquad(member: Member): Option[Squad] = {
    val available = squads.toList.filter(!_.full)
    val preferred = available.filter(_.leader.tendency == member.tendency)
    if(!preferred.isEmpty) preferred.sortBy(_.members.size).headOption
    else available.sortBy(_.members.size).headOption
  }

  def getRoleChanges(oldSquad: Squad, newSquad: Squad): Set[CharacterId] = {
    oldSquad.members.foldLeft(Set.empty[CharacterId]) { case (acc,chk) =>
      if(newSquad.getAssignment(chk.id) != oldSquad.getAssignment(chk.id)) {
        acc + chk.id
      } else acc
    }
  }

  def assign(member: Member): Set[CharacterId] = {
    var changed: Set[CharacterId] = Set()
    bestSquad(member).fold {
      if(member.leadership == Leadership.HIGH) {
        var new_squad = Squad.make(SquadTypes.STANDARD,squads.size,member)
        unassigned.toList.sortBy(_._2).take(11).foreach { case (mem,time) =>
          unassigned -= mem
          changed += mem.id
          assignments += (mem.id -> new_squad.id)
          new_squad = new_squad.place(mem)
        }
        squads += new_squad
      } else unassigned += (member->DateTime.now)
    } { squad =>
      val new_squad = squad.place(member)
      changed = getRoleChanges(squad,new_squad)
      assignments += (member.id -> new_squad.id)
      squads = (squads - squad) + new_squad
    }
    changed
  }

  def remove(cid: CharacterId): Set[CharacterId] = ???

  def makeSquad(leader: Member) = {
    squads += Squad.make(SquadTypes.STANDARD,squads.size,leader)
  }

  def makeLeader(cid: CharacterId): Unit = {
    assignments.get(cid).foreach { sid =>
      squads.find(_.id == sid).foreach { old_squad =>
        val new_leader = old_squad.members.find(_.id == cid).getOrElse(old_squad.leader)
        val new_squad = old_squad.copy(leader=new_leader)
        squads = (squads - old_squad) + new_squad
      }
    }
  }

  def setSquadType(stype: SquadType, cid: CharacterId): Set[CharacterId] = {
    var results = Set.empty[CharacterId]
    assignments.get(cid).foreach { sid =>
      squads.find(_.id == sid).foreach { old_squad =>
        if(old_squad.leader.id == cid) {
          val new_squad = old_squad.copy(
            stype=stype,
            assignments=Squad.doAssignments(stype,old_squad.leader,old_squad.members,old_squad.joined)
          )
          squads = (squads - old_squad) + new_squad
          results = getRoleChanges(old_squad,new_squad)
        }
      }
    }
    results
  }

  def getSquad(cid: CharacterId): Option[Squad] = {
    assignments.get(cid).flatMap(sid => squads.find(_.id == sid))
  }

  def getAssignment(cid: CharacterId): Option[Assignment] = {
    assignments.get(cid).flatMap {
      sid => squads.find(_.id == sid).flatMap(_.getAssignment(cid))
    }
  }

  def tracking(cid: CharacterId): Boolean = {
    val is_unassigned = unassigned.exists { case (id,_) => id == cid }
    val is_assigned = assignments.get(cid).map(_ => true).getOrElse(false)
    is_assigned || is_unassigned
  }

  def numSquads: Int = squads.size

  def reset() = {
    squads = Set()
    unassigned = Map()
    assignments = Map()
  }

}

object Squads {

}