package models

import org.joda.time.DateTime

class Squads {
  var squads: Set[Squad] = Set()
  var unassigned: Map[Member,DateTime] = Map()

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  def availableSquads = squads.filter(!_.full).size

  def bestSquad(member: Member): Option[Squad] = {
    val available = squads.toList.filter(!_.full)
    val preferred = available.filter(_.leader.tendency == member.tendency)
    if(!preferred.isEmpty) preferred.sortBy(_.members.size).headOption
    else available.sortBy(_.members.size).headOption
  }

  def assign(member: Member) = {
    bestSquad(member).fold {
      if(member.leadership == Leadership.HIGH) {
        var new_squad = Squad.make(SquadTypes.STANDARD,squads.size,member)
        unassigned.toList.sortBy(_._2).take(11).foreach { case (mem,time) =>
          unassigned -= mem
          new_squad = new_squad.place(mem)
        }
        squads += new_squad
      } else unassigned += (member->DateTime.now)
    } { squad =>
      squads = (squads - squad) + squad.place(member)
    }
  }

  def makeSquad(leader: Member) = {
    squads += Squad.make(SquadTypes.STANDARD,squads.size,leader)
  }

  def numSquads: Int = squads.size
}

object Squads {

}
