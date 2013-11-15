package soe


import models.{CharacterId, MemberId, Member}

import play.api.libs.json._
import play.api.libs.functional.syntax._

object CensusParser {

  case class SoeCharName(first: String, first_lower: String)

  case class SoeMemberResult(
    name: SoeCharName,
    character_id:String,
    member_since:String,
    member_since_date:String,
    online_status:String,
    rank: String,
    rank_ordinal:String) {

    def asCharId = CharacterId(character_id)
  }

  implicit val formatSoeCharName = Json.format[SoeCharName]
  implicit val formatSoeMemberResult = Json.format[SoeMemberResult]

  def parseOnlineCharacter(data: JsValue): List[CharacterId] = {
    val results = data.transform((__ \ "outfit_list").json.pick[JsArray]).flatMap(_(0).transform((__ \ "members").json.pick[JsArray]))
    results.map(_.value.toList.map(_.asOpt[SoeMemberResult]).flatten.filter(_.online_status == "1").map(_.asCharId)).getOrElse(List.empty)
  }
}
