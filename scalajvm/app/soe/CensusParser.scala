package soe

import play.api.libs.json._
import play.api.libs.functional.syntax._

object CensusParser {

  case class SoeCharName(first: String, first_lower: String)

  case class SoeCharacterRef(
    name:SoeCharName,
    character_id: String
  )

  implicit val formatSoeCharName = Json.format[SoeCharName]
  implicit val formatSoeCharacterRef = Json.format[SoeCharacterRef]

  def parseLookupCharacters(data: JsValue): List[SoeCharacterRef] = {
    val js_values = data.transform((__ \ "character_name_list").json.pick[JsArray]).map(_.value.toList).getOrElse(List.empty)
    js_values.map(_.asOpt[SoeCharacterRef]).flatten
  }
}
