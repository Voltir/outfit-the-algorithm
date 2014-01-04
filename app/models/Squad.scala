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
    Assignment(Roles.MAX,Fireteams.TWO),
    Assignment(Roles.ENGY,Fireteams.THREE),
    Assignment(Roles.HA,Fireteams.ONE),
    Assignment(Roles.MEDIC,Fireteams.ONE),
    Assignment(Roles.MAX,Fireteams.TWO),
    Assignment(Roles.ENGY,Fireteams.TWO),
    Assignment(Roles.MEDIC,Fireteams.TWO),
    Assignment(Roles.HA,Fireteams.ONE),
    Assignment(Roles.INF,Fireteams.THREE)
  ))

  val CRASH = SquadType("Support",Array(
    Assignment(Roles.MAX,Fireteams.ONE,Set(Special.POINT)),
    Assignment(Roles.ENGY,Fireteams.ONE),
    Assignment(Roles.MAX,Fireteams.ONE),
    Assignment(Roles.MEDIC,Fireteams.ONE),
    Assignment(Roles.MAX,Fireteams.ONE),
    Assignment(Roles.ENGY,Fireteams.ONE),
    Assignment(Roles.MAX,Fireteams.ONE),
    Assignment(Roles.ENGY,Fireteams.ONE),
    Assignment(Roles.MAX,Fireteams.ONE),
    Assignment(Roles.MEDIC,Fireteams.ONE),
    Assignment(Roles.MAX,Fireteams.ONE),
    Assignment(Roles.MAX,Fireteams.ONE)
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

  val MAGRIDER = SquadType("Magrider",Array(
    Assignment(Roles.MAG,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER),
    Assignment(Roles.MAG,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER),
    Assignment(Roles.MAG,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER),
    Assignment(Roles.MAG,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER),
    Assignment(Roles.MAG,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER),
    Assignment(Roles.MAG,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER)
  ))

  val BUGGY = SquadType("Buggy",Array(
    Assignment(Roles.HARASSER,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER),
    Assignment(Roles.HARASSER,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER),
    Assignment(Roles.HARASSER,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER),
    Assignment(Roles.HARASSER,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER),
    Assignment(Roles.HARASSER,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER),
    Assignment(Roles.HARASSER,Fireteams.DRIVER),
    Assignment(Roles.ENGY,Fireteams.GUNNER)
  ))

  val LIGHTNING = SquadType("Lightning",Array(
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER),
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER),
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER),
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER),
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER),
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER),
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER),
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER),
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER),
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER),
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER),
    Assignment(Roles.LIGHTNING,Fireteams.DRIVER)
  ))
}

case class Squad(
  id: Int,
  stype: SquadType, 
  leader: Member,
  members: Set[Member],
  joined: Map[CharacterId,DateTime],
  fireteams: Boolean,
  assignments: Map[CharacterId,Assignment]) {

  def place(new_member: Member) = {
    val updated_members = members + new_member
    val updated_fireteams = (updated_members.size >= 8) || (fireteams && updated_members.size > 5)
    val updated_joined = joined + (new_member.id -> DateTime.now)
    if(full) this
    else {
      copy(
        members=updated_members,
        fireteams=updated_fireteams,
        joined=updated_joined,
        assignments=Squad.doAssignments(stype,leader,updated_members,updated_joined))
    }
  }

  def remove(cid: CharacterId) = {
    val updated_members = members.filter(_.id != cid)
    val updated_fireteams = (updated_members.size >= 8) || (fireteams && updated_members.size > 5)
    val updated_joined = joined.filter { case (id,join) => id != cid }
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

  def full = members.size >= 12

  override def hashCode = id.hashCode() + 19

  override def equals(other: Any) = {
    other match {
      case s: Squad => s.id == id
      case _ => false
    }
  }
}

object Squad {
  def make(stype: SquadType, id: Int, leader: Member): Squad = {
    val joined = Map(leader.id->DateTime.now)
    Squad(id,stype,leader,Set(leader),joined,false,doAssignments(stype,leader,Set(leader),joined))
  }

  def score(member: Member, is_leader: Boolean,role: String): Int = {
    var amt = member.prefs.get(role).getOrElse(0)
    if(is_leader) amt *= 2;
    amt
  }

  def doAssignments(
   stype: SquadType,
   leader: Member,
   input: Set[Member],
   joined: Map[CharacterId,DateTime]): Map[CharacterId,Assignment] = {

    def backtrack(inp: Map[CharacterId,Assignment],picked: Member): Map[CharacterId,Assignment] = {
      var maybeSwap: Option[CharacterId] = None
      var best_assignment = inp(picked.id)
      var best_current_score = score(picked,picked == leader, best_assignment.role)
      inp.foreach { case (id,assignment) =>
        val mem = input.find(_.id == id).get
        val current_score = score(mem,mem == leader,assignment.role)
        val potential_score = score(mem,mem == leader,best_assignment.role)
        val potential_picked = score(picked,picked==leader,assignment.role)
        if(current_score + best_current_score < potential_score + potential_picked) {
          best_current_score = potential_picked
          best_assignment = assignment
          maybeSwap = Some(id)
        }
      }
      maybeSwap.map { swap_cid => 
        val a = swap_cid->inp(picked.id)
        val b = picked.id->best_assignment
        backtrack(inp + a + b,input.find(_.id == swap_cid).get)
      }.getOrElse(inp)
    }

    var result = Map[CharacterId,Assignment]()
    var remaining = for {
      i <- 0 until input.size
      m <- input
    } yield {
      val s = score(m,m == leader,stype.assignments(i).role)
      (s,i,m)
    }
    remaining = remaining.sortBy(_._1).reverse
    while(remaining.nonEmpty) {
      val (s,i,picked) = remaining.foldLeft(remaining.head) { case (acc,value) =>
        if(acc._1 == value._1 && acc._3.id == value._3.id && value._2 < acc._2)  value
        else if(acc._1 == value._1 && acc._3.id != value._3.id && joined(value._3.id).isBefore(joined(acc._3.id))) value
        else acc
      }
      result += (picked.id->stype.assignments(i))
      result = backtrack(result,picked)
      remaining = remaining.filter(c => c._3 != picked && c._2 != i)
    }
    result
  }
}

object JSFormat {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  //implicit val CharacterIdFormat = Json.format[CharacterId]
  implicit val MemberDetailFormat = Json.format[Member]
  implicit val TupleFormat = (__(0).format[Member] and __(1).format[String]).tupled
  implicit val AssignmentFormat = Json.format[Assignment]
  implicit val SquadTypeFormat = Json.format[SquadType]
  //implicit val SquadFormat = Json.format[Squad]
}
