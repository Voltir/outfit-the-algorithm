package models

import play.api.libs.json.Json
import org.joda.time.DateTime
import models.Format._

case class Assignment(
 role: String,
 fireteam: String,
 additional: Set[String] = Set()
)

case class SquadType(name: String, assignments: Array[Assignment])

object SquadTypes {

  val STANDARD = SquadType("Standard",Array(
    Assignment(Roles.HA,Fireteams.ONE,Set(Special.POINT)),
    Assignment(Roles.MEDIC,Fireteams.ONE),
    Assignment(Roles.HA,Fireteams.TWO,Set(Special.POINT)),
    Assignment(Roles.MEDIC,Fireteams.TWO),
    Assignment(Roles.ENGY,Fireteams.THREE),
    Assignment(Roles.HA,Fireteams.ONE),
    Assignment(Roles.HA,Fireteams.TWO),
    Assignment(Roles.INF,Fireteams.THREE),
    Assignment(Roles.MEDIC,Fireteams.ONE),
    Assignment(Roles.MEDIC,Fireteams.TWO),
    Assignment(Roles.HA,Fireteams.ONE),
    Assignment(Roles.HA,Fireteams.TWO)
  ))

  val SUPPORT = SquadType("Support",Array(
    Assignment(Roles.HA,Fireteams.ONE,Set(Special.POINT)),
    Assignment(Roles.MEDIC,Fireteams.ONE),
    Assignment(Roles.MAX,Fireteams.TWO,Set(Special.POINT)),
    Assignment(Roles.ENGY,Fireteams.TWO),
    Assignment(Roles.MAX,Fireteams.TWO),
    Assignment(Roles.HA,Fireteams.ONE),
    Assignment(Roles.MAX,Fireteams.TWO),
    Assignment(Roles.MEDIC,Fireteams.ONE),
    Assignment(Roles.ENGY,Fireteams.TWO),
    Assignment(Roles.MAX,Fireteams.TWO),
    Assignment(Roles.HA,Fireteams.ONE),
    Assignment(Roles.INF,Fireteams.THREE)
  ))

  val JETPACK = SquadType("Jetpack",Array(
    Assignment(Roles.LA,Fireteams.ONE),
    Assignment(Roles.LA,Fireteams.TWO),
    Assignment(Roles.LA,Fireteams.ONE),
    Assignment(Roles.LA,Fireteams.TWO),
    Assignment(Roles.LA,Fireteams.ONE),
    Assignment(Roles.LA,Fireteams.TWO),
    Assignment(Roles.LA,Fireteams.ONE),
    Assignment(Roles.LA,Fireteams.TWO),
    Assignment(Roles.LA,Fireteams.ONE),
    Assignment(Roles.LA,Fireteams.TWO),
    Assignment(Roles.LA,Fireteams.ONE),
    Assignment(Roles.LA,Fireteams.TWO)
  ))
}

case class Squad(
  stype: SquadType, 
  leader: MemberDetail,
  members: Set[MemberDetail],
  joined: Map[CharacterId,DateTime],
  fireteams: Boolean,
  assignments: Map[CharacterId,Assignment]) {

  def place(new_member: MemberDetail) = {
    val updated_members = members + new_member
    val updated_fireteams = (updated_members.size >= 8) || (fireteams && updated_members.size > 5)
    val updated_joined = joined + (new_member.id -> DateTime.now)
    copy(
      members=updated_members,
      fireteams=updated_fireteams,
      joined=updated_joined,
      assignments=Squad.doAssignments(stype,leader,updated_members,updated_joined))
  }

  def remove(cid: CharacterId) = {
    val updated_members = members.filter(_.id == cid)
    val updated_fireteams = (updated_members.size >= 8) || (fireteams && updated_members.size > 5)
    val updated_joined = joined.filter { case (id,join) => id == cid }
    if(leader.id == cid) { 
      copy(
        leader=updated_members.head,
        members=updated_members,
        fireteams=updated_fireteams,
        joined=updated_joined,
        assignments=Squad.doAssignments(stype,updated_members.head,updated_members,updated_joined))
    }
    else {
      copy(
        members=updated_members,
        fireteams=updated_fireteams,
        joined=updated_joined,
        assignments=Squad.doAssignments(stype,leader,updated_members,updated_joined))
    }
  }
  
  def getAssignment(cid: CharacterId): Option[Assignment] = assignments.get(cid)
}

object Squad {
  def make(stype: SquadType,leader: MemberDetail): Squad = {
    val joined = Map(leader.id->DateTime.now)
    Squad(stype,leader,Set(leader),joined,false,doAssignments(stype,leader,Set(leader),joined))
  }

  def score(member: MemberDetail, is_leader: Boolean,role: String): Int = {
    var amt = member.prefs.get(role).getOrElse(0)
    if(is_leader) amt *= 2;
    amt
  }

  def doAssignments(
    stype: SquadType,
    leader: MemberDetail, 
    input: Set[MemberDetail],
    joined: Map[CharacterId,DateTime]): Map[CharacterId,Assignment] = {
   
    implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

    var unassigned = input.toList
    stype.assignments.take(input.size).foldLeft(Map[CharacterId,Assignment]()) { case (acc,assignment) =>
      val ordered = unassigned.map(m => (score(m,m == leader,assignment.role),m)).sortBy(_._1).reverse
      val equiv = ordered.filter(a => a._1 == ordered.head._1)
      val (_,selected) = equiv.map { case (s,m) => (joined(m.id),m) }.sortBy(_._1).head
      unassigned = ordered.filter(_._2 != selected).map(_._2)
      acc + (selected.id->assignment)
    }
  }
}

object JSFormat {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  //implicit val CharacterIdFormat = Json.format[CharacterId]
  implicit val MemberDetailFormat = Json.format[MemberDetail]
  implicit val TupleFormat = (__(0).format[MemberDetail] and __(1).format[String]).tupled
  implicit val AssignmentFormat = Json.format[Assignment]
  implicit val SquadTypeFormat = Json.format[SquadType]
  //implicit val SquadFormat = Json.format[Squad]
}
