package squad

import scala.collection.mutable.{Map => MutableMap, ArrayBuffer}
import shared.models._

case class Preference(score: Map[Pattern.Role,Int])

case class Context(
  update: Pattern,
  pins: Map[CharacterId,Int],
  preference: MutableMap[CharacterId,Preference]
)

object Assign {

  def apply(squad: Squad, ctx: Context): Squad = {
    var members = squad.roles.map(_.character).toSet
    var availableRoles = (0 until 12).toSet
    val newRoles: ArrayBuffer[AssignedRole] = ArrayBuffer.empty

    //do pins
    ctx.pins.foreach {
      case (cid, idx) =>
        members.find(_.cid == cid).foreach {
          member =>
            newRoles += AssignedRole(idx, member)
            members = members - member
            availableRoles = availableRoles - idx
        }
    }

    //do auto
    def backtrack(current: ArrayBuffer[AssignedRole]): Unit = {
      val last = current.last
      val lastRoleScore = ctx.preference(last.character.cid).score(ctx.update.assignments(last.idx).role)
      var bestScore = -1
      var bestBufferIdx = -1
      var bestRoleIdx = -1
      current.take(current.size - 1).zipWithIndex.foreach {
        case (role, idx) =>
          val roleScore = ctx.preference(role.character.cid).score(ctx.update.assignments(role.idx).role)
          val swappedScore1 = ctx.preference(last.character.cid).score(ctx.update.assignments(role.idx).role)
          val swappedScore2 = ctx.preference(role.character.cid).score(ctx.update.assignments(last.idx).role)
          val currentValue = roleScore + lastRoleScore
          val swappedValue = swappedScore1 + swappedScore2
          if (swappedValue > currentValue && swappedValue > bestScore && !ctx.pins.contains(role.character.cid)) {
            bestScore = swappedValue
            bestBufferIdx = idx
            bestRoleIdx = role.idx
          }
      }
      if (bestScore > 0) {
        current(bestBufferIdx) = current(bestBufferIdx).copy(idx = last.idx)
        current(current.size - 1) = last.copy(idx = bestRoleIdx)
        backtrack(current)
      }
    }

    (0 until members.size).foreach {
      i =>
        val nextRoleIdx: Int = availableRoles.min
        val nextRole = ctx.update.assignments(nextRoleIdx).role
        val bestMember = members.maxBy {
          m =>
            ctx.preference(m.cid).score(nextRole)
        }
        newRoles += AssignedRole(nextRoleIdx, bestMember)
        members = members - bestMember
        availableRoles = availableRoles - nextRoleIdx
        backtrack(newRoles)
    }

    //Return Updated Squad
    squad.copy(roles = newRoles.toList, pattern = ctx.update)
  }

}