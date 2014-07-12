package shared.models

import shared.models.Pattern.Role

case class Pref(role: Role, score: Int)
case class PreferenceDefinition(values: List[Pref])

object PreferenceDefinitionRegister {
  import shared.AlgoPickler
  AlgoPickler.register[Pref]
  AlgoPickler.register[PreferenceDefinition]
}
