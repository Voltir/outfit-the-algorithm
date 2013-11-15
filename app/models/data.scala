package models

import play.api.libs.json._

case class MemberId(id: String)

case class CharacterId(id: String)

case class Member (
  id: MemberId,
  name: String
)

case class Preference(
  name: String,
  value: Int
)

case class MemberDetail(
  id: MemberId,
  totalTime: Double,
  leadTime: Double,
  desire: String,
  capabilities: List[String],
  preferences: List[Preference]
) {

  def desireScore: Double = desire match {
    case "BREAK" => 0.0
    case "HIGH" => 13.0
    case "MED" => 5.0
    case _ => 1.0
  }

}

object Format {
  implicit val FormatCharId = Json.format[CharacterId]
  implicit val FormatMemberId = Json.format[MemberId]
  implicit val FormatMember = Json.format[Member]
}
