package soe


import models._

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

  case class SoeCharacterRef(
    name:SoeCharName,
    character_id: String
  ) {
    def asCharRef = CharacterRef(CharacterId(character_id),name.first)
  }

  case class SoeCurrency(
    aerospace: String,
    infantry: String,
    mechanized: String
  )

  case class SoeCurrencyResult(
    character_id: String,
    currency: SoeCurrency
  )

  implicit val formatSoeCharName = Json.format[SoeCharName]
  implicit val formatSoeMemberResult = Json.format[SoeMemberResult]
  implicit val formatSoeCharacterRef = Json.format[SoeCharacterRef]
  implicit val formatSoeCurrency = Json.format[SoeCurrency]
  implicit val formatSoeCurrencyResult = Json.format[SoeCurrencyResult]

  def parseOnlineCharacter(data: JsValue): Set[CharacterId] = {
    val asList = data.transform((__ \ "outfit_list").json.pick[JsArray]).flatMap(_(0).transform((__ \ "members").json.pick[JsArray]))
    val result = asList.map { 
      _.value.toList.map(_.asOpt[SoeMemberResult]).flatten.filter(_.online_status == "1").map(_.asCharId)
    }.getOrElse(List.empty)
    result.toSet
  }

  def parseLookupCharacters(data: JsValue): List[CharacterRef] = {
    val js_values = data.transform((__ \ "character_name_list").json.pick[JsArray]).map(_.value.toList).getOrElse(List.empty)
    js_values.map(_.asOpt[SoeCharacterRef]).flatten.map(_.asCharRef)
  }

  def parseValidateName(data: JsValue): Option[String] = {
    val foo = data.transform((__ \ "character_list").json.pick[JsArray])
    val stabmyhead = foo.map(_.value.flatMap(_.transform((__ \ "character_id").json.pick[JsString]).asOpt).toList.headOption).map(_.map(_.value)).getOrElse(None)
    stabmyhead
  }

  def parseCurrency(data: JsValue): Map[CharacterId,Resources] = {
    val js_values = data.transform((__ \ "character_list").json.pick[JsArray]).map(_.value.toList).getOrElse(List.empty)
    js_values.map(_.asOpt[SoeCurrencyResult]).flatten.foldLeft(Map[CharacterId,Resources]()) { case (acc,result) =>
      val res = Resources(result.currency.infantry.toInt,result.currency.mechanized.toInt,result.currency.aerospace.toInt)
      acc + (CharacterId(result.character_id) -> res)
    }
  }
}
