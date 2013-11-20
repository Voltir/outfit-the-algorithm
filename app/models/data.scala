package models

import play.api.libs.json._

object Roles {
  val HA = "Heavy Assualt"
  val MEDIC = "Medic"
  val ENGY = "Engineer"
  val LA = "Light Assault"
  val INF = "Infiltraitor"
  val MAX = "MAX"
}

case class MemberId(id: String)

case class CharacterId(id: String)

case class CharacterRef(
  cid: CharacterId,
  name: String
)

case class Member (
  id: MemberId,
  name: String
)

case class MemberDetail(
  id: CharacterId,
  name: String,
  totalTime: Double,
  leadTime: Double,
  desire: String,
  preferences: Map[String,Int]
) {
  def desireScore: Double = desire match {
    case "HIGH" => 13.0
    case "MED" => 5.0
    case _ => 1.0
  }
}

object Format {
  implicit val FormatCharId = Json.format[CharacterId]
  implicit val FormatCharRef = Json.format[CharacterRef]
  implicit val FormatMemberId = Json.format[MemberId]
  implicit val FormatMember = Json.format[Member]
}
