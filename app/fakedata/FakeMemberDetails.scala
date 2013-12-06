package fakedata

import models._

object FakeMember {
  def apply(id: String, prefs: Map[String,Int] = Map.empty) = {
    Member(
      id=CharacterId(id),
      name=id,
      tendency=Tendency.INFANTRY,
      leadership=Leadership.NEVER,
      canMentor=false,
      point="TODO",
      prefs=prefs)
  }
}

object FakeLeader {
  def apply(id: String, prefs: Map[String,Int] = Map.empty) = {
    Member(
      id=CharacterId(id),
      name=id,
      tendency=Tendency.INFANTRY,
      leadership=Leadership.HIGH,
      canMentor=true,
      point="TODO",
      prefs=prefs)
  }
}
