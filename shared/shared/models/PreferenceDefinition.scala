package shared.models

import shared.models.Pattern.Role

case class PreferenceDefinition(values: List[(Role,Int)])

object PreferenceDefinitionRegister {
  import shared.AlgoPickler
  AlgoPickler.register[PreferenceDefinition]
}
