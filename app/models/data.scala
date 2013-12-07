package models

import play.api.libs.json._

object Roles {
  val HA = "Heavy Assault"
  val MEDIC = "Medic"
  val ENGY = "Engineer"
  val LA = "Light Assault"
  val INF = "Infiltrator"
  val MAX = "MAX"
  val MAG = "Magrider"
  val HARASSER = "Harasser"
  val LIGHTNING = "Lightning"
  val SUNDERER = "Sunderer"
  val GALAXY = "Galaxy"
  val SCYTHE = "Scythe"
  val LIB = "Liberator"
}

object Fireteams {
  val ONE = "Fireteam One"
  val TWO = "Fireteam Two"
  val THREE = "Fireteam Three"
  val DRIVER = "Driver Team"
  val GUNNER = "Gunner Team"
}

object Leadership {
  val HIGH = "HIGH"
  val LOW = "LOW"
  val NEVER = "NEVER"
  val MENTOR = "MENTOR"
}

object Tendency {
  val INFANTRY = "INFANTRY"
  val AIR = "AIR"
  val ARMOR  = "ARMOR"
}

object Special {
  val POINT = "Pointman"
}

case class CharacterId(id: String)

case class CharacterRef(
  cid: CharacterId,
  name: String
)

case class Resources(
  infantry: Int,
  armor: Int,
  air: Int
)

case class PreferenceData(
  cid:String,
  name:String,
  leader:Option[String],
  point:Option[String],
  ha:Option[Int],
  medic:Option[Int],
  engy:Option[Int],
  la:Option[Int],
  inf:Option[Int],
  MAX: Option[Int],
  magrider:Option[Int],
  harasser:Option[Int],
  sunderer:Option[Int],
  lightning:Option[Int],
  galaxy:Option[Int],
  scythe:Option[Int],
  liberator:Option[Int]
)

case class Member(
  id: CharacterId,
  name: String,
  tendency: String,
  leadership: String,
  canMentor: Boolean,
  point: String,
  prefs: Map[String,Int]
) {
  override def hashCode = id.hashCode() + 17

  override def equals(other: Any) = {
    other match {
      case m: Member => m.id == id
      case _ => false
    }
  }
}

object Format {
  implicit val FormatCharId = Json.format[CharacterId]
  implicit val FormatCharRef = Json.format[CharacterRef]
  implicit val FormatResources = Json.format[Resources]
  implicit val prefFormat = Json.format[PreferenceData]
}
