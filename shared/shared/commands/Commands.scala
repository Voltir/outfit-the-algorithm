package shared.commands

import shared.models._
import shared.models.Squad.PatternTypePreference

sealed trait Commands
case class LoadInitial(pref: PreferenceDefinition) extends Commands
case object Logout extends Commands
case class CreateSquad(leader: Character, pattern: Pattern, pref: PatternTypePreference) extends Commands
case object DisbandSquad extends Commands
case class JoinSquad(lid: CharacterId) extends Commands
case class MoveToSquad(lid: CharacterId, cid: CharacterId) extends Commands
case class SetPattern(pattern: Pattern, lid: CharacterId) extends Commands
case class Unassign(cid: CharacterId) extends Commands
case object UnassignSelf extends Commands
case class PinAssignment(lid: CharacterId, pattern: String, assignment: Int) extends Commands
case class UnpinAssignment(lid: CharacterId, pattern: String) extends Commands
case class SetPreference(pref: PreferenceDefinition) extends Commands
case class VolunteerFC(fcid: CharacterId) extends Commands
case object StepDownFC extends Commands
case class MakeLeader(lid: CharacterId, target: CharacterId) extends Commands

sealed trait Response
case class LoadInitialResponse(squads: List[Squad], unassigned: List[Character], fc: Option[Character]) extends Response
case class SquadUpdate(squad: Squad) extends Response
case class Unassigned(unassigned: List[Character]) extends Response
case class UpdateFC(character: Option[Character]) extends Response
case object ELBKeepAlive extends Commands with Response