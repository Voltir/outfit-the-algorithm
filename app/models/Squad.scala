package models

import play.api.libs.json.Json
import org.joda.time.DateTime

//case class SquadType(name: String, roles: Array[String])

case class Assignment(
 role: String,
 fireteam: String,
 special: List[String] = List.empty
)

case class SquadType(name: String, assignments: Array[Assignment])

object SquadTypes {

  val STANDARD = SquadType("Standard",Array(
    Assignment(Roles.HA,Fireteams.ONE,List(Special.POINT)),
    Assignment(Roles.MEDIC,Fireteams.ONE),
    Assignment(Roles.HA,Fireteams.TWO,List(Special.POINT)),
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
    Assignment(Roles.HA,Fireteams.ONE,List(Special.POINT)),
    Assignment(Roles.MEDIC,Fireteams.ONE),
    Assignment(Roles.MAX,Fireteams.TWO,List(Special.POINT)),
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

  /*
  val STANDARD = SquadType("Standard",Array(
    Roles.HA,
    Roles.MEDIC,
    Roles.HA,
    Roles.MEDIC,
    Roles.ENGY,
    Roles.HA,
    Roles.INF,
    Roles.HA,
    Roles.MEDIC,
    Roles.HA,
    Roles.MEDIC,
    Roles.HA)
  )

  val SUPPORT = SquadType("Support",Array(
    Roles.MAX,
    Roles.ENGY,
    Roles.MAX,
    Roles.ENGY,
    Roles.MEDIC,
    Roles.MAX,
    Roles.HA,
    Roles.MEDIC,
    Roles.HA,
    Roles.ENGY,
    Roles.HA,
    Roles.INF)
  )

  val JETPACK = SquadType("Jetpack",Array(
    Roles.LA,
    Roles.LA,
    Roles.LA,
    Roles.LA,
    Roles.LA,
    Roles.LA,
    Roles.LA,
    Roles.LA,
    Roles.LA,
    Roles.LA,
    Roles.LA,
    Roles.LA)
  )*/
}

//case class Assignment(
//  name: String,
//  given: DateTime)

case class Squad(
  stype: SquadType, 
  leader: MemberDetail,
  members: Set[MemberDetail],
  assignments: Map[CharacterId,Assignment]) {

  def place(new_member: MemberDetail) = {
    val updated_members = members + new_member
    println(s"Wat? $updated_members")
    copy(members=updated_members,assignments=Squad.doAssignments(stype,updated_members))
  }

  def remove(cid: CharacterId) = {
    val updated_members = members.find(_.id == cid).map(members - _).getOrElse(members)
    if(leader.id == cid) copy(leader=updated_members.head,members=updated_members,assignments=Squad.doAssignments(stype,updated_members))
    else copy(members=updated_members,assignments=Squad.doAssignments(stype,updated_members))
  }
  
  def getAssignment(cid: CharacterId): Option[Assignment] = assignments.get(cid)
}

object Squad {
  def make(stype: SquadType,leader: MemberDetail): Squad = {
    Squad(stype,leader,Set(leader),doAssignments(stype,Set(leader)))
  }

  def score(member: MemberDetail, role: String): Int = {
    member.preferences.get(role).getOrElse(0)
  }

  def doAssignments(stype: SquadType,input: Set[MemberDetail]): Map[CharacterId,Assignment] = {
    var unassigned = input.toList
    stype.assignments.take(input.size).foldLeft(Map[CharacterId,Assignment]()) { case (acc,assignment) =>
      val ordered = unassigned.map(m => (score(m,assignment.role),m)).sortBy(_._1).reverse
      val (_,selected) = ordered.head
      unassigned = ordered.tail.map(_._2)
      acc + (selected.id->assignment)
    }
  }
}

object JSFormat {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  implicit val CharacterIdFormat = Json.format[CharacterId]
  implicit val MemberDetailFormat = Json.format[MemberDetail]
  implicit val TupleFormat = (__(0).format[MemberDetail] and __(1).format[String]).tupled
  implicit val AssignmentFormat = Json.format[Assignment]
  implicit val SquadTypeFormat = Json.format[SquadType]
  //implicit val SquadFormat = Json.format[Squad]
}
