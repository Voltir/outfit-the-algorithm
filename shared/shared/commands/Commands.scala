package shared.commands

import shared.models._

sealed trait Commands
case object LoadInitial extends Commands
case object CreateSquad extends Commands
case class Unassign(cid: CharacterId) extends Commands
case class SetCurrentSquad(cid: CharacterId, idx: Int) extends Commands
case class PinAssignment(cid: CharacterId, assignment: Int) extends Commands

sealed trait Response
case class LoadInitialResponse(squads: List[Squad], unassigned: List[Character]) extends Response
case class SquadUpdate(index: Int, squad: Squad) extends Response
case class Unassigned(unassigned: List[Character]) extends Response
case class CurrentSquadResponse(index: Int, squad: Squad) extends Response

object CommandsRegister {
  import shared.AlgoPickler
  AlgoPickler.register(LoadInitial)
  AlgoPickler.register(CreateSquad)
  AlgoPickler.register[Unassign]
  //AlgoPickler.register[SetCurrentSquad]
  //AlgoPickler.register[PinAssignment]
  AlgoPickler.register[LoadInitialResponse]
  //AlgoPickler.register[SquadUpdate]
  //AlgoPickler.register[Unassigned]
  //AlgoPickler.register[CurrentSquadResponse]
}
