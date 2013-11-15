package models

import play.api.libs.json._

case class MemberId(id: String)

case class CharacterId(id: String)

case class Member (
  id: MemberId,
  name: String
)

object Format {
  implicit val FormatCharId = Json.format[CharacterId]
  implicit val FormatMemberId = Json.format[MemberId]
  implicit val FormatMember = Json.format[Member]
}