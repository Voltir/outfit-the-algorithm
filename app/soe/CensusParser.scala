package soe


import models.{MemberId, Member}

import play.api.libs.json._
import syntax._
import play.api.libs.functional.syntax._
import play.api.libs.json.extensions._

object CensusParser {

  case class SoeMemberResult(
    character_id:String,
    member_since:String,
    member_since_date:String,
    online_status:String,
    rank: String,
    rank_ordinal:String)

  implicit val formatSoeMemberResult = Json.format[SoeMemberResult]

  def parseOnlineMembers(data: JsValue): List[Member] = {
    val results = data.transform((__ \ "outfit_list" \\ "members").json.pick).flatMap(_.validate[List[SoeMemberResult]])
    results.map(_.filter(_.online_status=="1").map(soe => Member(MemberId(soe.character_id),"TodoName"))).getOrElse(List.empty)
  }
}
