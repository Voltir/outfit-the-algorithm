package shared.models

import shared.models.Pattern.Role

case class Pref(role: Role, score: Int)

case class PreferenceDefinition(values: List[Pref])