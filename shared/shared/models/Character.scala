package shared.models

case class CharacterId(txt: String) { override def toString() = txt }

case class Character(cid: CharacterId, name: String)

case class AutoLogin(character: Character, enabled: Boolean)
