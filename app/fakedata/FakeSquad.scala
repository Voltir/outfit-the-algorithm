package fakedata

import models._

object FakeSquadType {
  def apply() = {
    SquadType("Standard",Array(
      Roles.HA,
      Roles.MEDIC,
      Roles.HA,
      Roles.MEDIC,
      Roles.ENGY,
      Roles.HA,
      Roles.HA,
      Roles.MEDIC,
      Roles.HA,
      Roles.MEDIC,
      Roles.INF,
      Roles.HA)
    )
  }
}
