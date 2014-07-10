package shared.commands

import shared.models._
import shared.models.Squad.PatternTypePreference

sealed trait Commands
case object LoadInitial extends Commands
case object TestIt extends Commands
case object Logout extends Commands
case class CreateSquad(leader: Character, pattern: Pattern, pref: PatternTypePreference) extends Commands
case object DisbandSquad extends Commands
case class JoinSquad(lid: CharacterId) extends Commands
case class Unassign(cid: CharacterId) extends Commands
case object UnassignSelf extends Commands
case class PinAssignment(cid: CharacterId, assignment: Int) extends Commands

sealed trait Response
case class LoadInitialResponse(squads: List[Squad], unassigned: List[Character]) extends Response
case class SquadUpdate(squad: Squad) extends Response
case class Unassigned(unassigned: List[Character]) extends Response

object CommandsRegister {
  import shared.AlgoPickler
  AlgoPickler.register(LoadInitial)
  AlgoPickler.register(TestIt)
  AlgoPickler.register(Logout)
  AlgoPickler.register[CreateSquad]
  AlgoPickler.register(DisbandSquad)
  AlgoPickler.register[JoinSquad]
  AlgoPickler.register[Unassign]
  AlgoPickler.register(UnassignSelf)
  //AlgoPickler.register[PinAssignment]
  AlgoPickler.register[LoadInitialResponse]
  AlgoPickler.register[SquadUpdate]
  AlgoPickler.register[Unassigned]
}
