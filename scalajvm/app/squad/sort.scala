#!/usr/bin/env scala
import scala.collection.mutable.ArrayBuffer

case class CharacterId(txt: String)

case class Character(cid: CharacterId, name: String)

case class AssignedRole(
    idx: Int,
    character: Character
)

object Pattern {
  sealed trait Role

  case object Unassigned extends Role

  trait InfantryRole extends Role
  case object HeavyAssault extends InfantryRole
  case object LightAssault extends InfantryRole
  case object Medic extends InfantryRole
  case object Engineer extends InfantryRole
  case object MAX extends InfantryRole
  case object Infiltraitor extends InfantryRole

  case class Assignment(role: Role)
}

case class Pattern(
  assignments: Array[Pattern.Assignment]
)

case class Squad(
  leader: Character,
  pattern: Pattern,
  roles: List[AssignedRole]
)

case class Preference(
  score: Map[Pattern.Role,Int] = Map.empty withDefaultValue 0
)

case class Context(
  update: Pattern,
  pins: Map[CharacterId,Int],
  preference: Map[CharacterId,Preference]
)

def assign(squad: Squad, ctx: Context): Squad = {
  var members = squad.roles.map(_.character).toSet
  var availableRoles = (0 until 12).toSet
  val newRoles: ArrayBuffer[AssignedRole] = ArrayBuffer.empty

  //do pins
  ctx.pins.foreach { case (cid,idx) =>
    members.find(_.cid == cid).foreach { member =>
      newRoles += AssignedRole(idx,member)
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
    current.take(current.size-1).zipWithIndex.foreach { case (role,idx) =>
      val roleScore = ctx.preference(role.character.cid).score(ctx.update.assignments(role.idx).role)
      val swappedScore1 = ctx.preference(last.character.cid).score(ctx.update.assignments(role.idx).role)
      val swappedScore2 = ctx.preference(role.character.cid).score(ctx.update.assignments(last.idx).role)
      val currentValue = roleScore + lastRoleScore
      val swappedValue = swappedScore1 + swappedScore2
      if(swappedValue > currentValue && swappedValue > bestScore && !ctx.pins.contains(role.character.cid)) {
        bestScore = swappedValue
        bestBufferIdx = idx
        bestRoleIdx = role.idx
      }
    }
    if(bestScore > 0) {
      current(bestBufferIdx) = current(bestBufferIdx).copy(idx=last.idx)
      current(current.size-1) = last.copy(idx=bestRoleIdx)
      backtrack(current)
    }
  }

  (0 until members.size).foreach { i =>
    val nextRoleIdx: Int = availableRoles.min
    val nextRole = ctx.update.assignments(nextRoleIdx).role
    val bestMember = members.maxBy { m => 
      ctx.preference(m.cid).score(nextRole)
    }
    newRoles += AssignedRole(nextRoleIdx,bestMember)
    members = members - bestMember
    availableRoles = availableRoles - nextRoleIdx
    backtrack(newRoles)
  }

  //Return Updated Squad
  squad.copy(roles=newRoles.toList,pattern=ctx.update)
}

object Fakez {
  import Pattern._
  val basic = Pattern(Array(
    Assignment(HeavyAssault),
    Assignment(HeavyAssault),
    Assignment(HeavyAssault),
    Assignment(Medic),
    Assignment(Medic),
    Assignment(Engineer),
    Assignment(HeavyAssault),
    Assignment(HeavyAssault),
    Assignment(HeavyAssault),
    Assignment(Medic),
    Assignment(Medic),
    Assignment(Infiltraitor)
  ))

  val m1 = Character(CharacterId("Ha1"),"Ha1")
  val m2 = Character(CharacterId("HaMe2"),"HaMe2")
  val m3 = Character(CharacterId("Ha3"),"Ha3")
  val m4 = Character(CharacterId("Ha4"),"Ha4")
  val m5 = Character(CharacterId("Medic1"),"Medic1")
  val m6 = Character(CharacterId("Engy11"),"Engy11")
 
  val prefz: Map[CharacterId,Preference] = Map(
    (m1.cid -> Preference(Map[Role,Int](HeavyAssault -> 5) withDefaultValue 0)),
    (m2.cid -> Preference(Map[Role,Int](HeavyAssault -> 5, Medic -> 5) withDefaultValue 0)),
    (m3.cid -> Preference(Map[Role,Int](HeavyAssault -> 5) withDefaultValue 0)),
    (m4.cid -> Preference(Map[Role,Int](HeavyAssault -> 4) withDefaultValue 0)),
    (m5.cid -> Preference(Map[Role,Int](Medic -> 5) withDefaultValue 0)),
    (m6.cid -> Preference(Map[Role,Int](Engineer -> 5) withDefaultValue 0))
  )

  val pinz: Map[CharacterId,Int] = Map(
    //m4.cid -> 3
    m6.cid -> 2
  )

  val init = Squad(m1,basic,List(
    AssignedRole(11,m1),
    AssignedRole(11,m2),
    AssignedRole(11,m3),
    AssignedRole(11,m4),
    AssignedRole(11,m5),
    AssignedRole(11,m6)
  ))

  val ctx = Context(basic,pinz,prefz)

  def nicePrint(squad: Squad) = {
    squad.roles.foreach { r =>
      val assignment = squad.pattern.assignments(r.idx)
      println(s"${r.character.name} has role ${assignment.role} (${r.idx})")
    }
  }

  def doIt() = {
    println("Starting...")
    nicePrint(init)
    println("~~~~~~")
    nicePrint(assign(init,ctx))
  }
}

Fakez.doIt()
