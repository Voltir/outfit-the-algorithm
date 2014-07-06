package shared.models
import scala.collection.mutable.ArrayBuffer

object Squad {
  sealed trait PatternTypePreference
  case object InfantryPreference extends PatternTypePreference
  case object ArmorPreference extends PatternTypePreference
  case object AirPreference extends PatternTypePreference

  val FakeLeader1 = Character(CharacterId("1"),"Fake Infantry Leader")
  val FakeLeader2 = Character(CharacterId("2"),"Fake Armor Leader")
  val FakeLeader3 = Character(CharacterId("3"),"Fake Air Leader")

  val FakeMembers = List(
    AssignedRole(0,Character(CharacterId("100"),"Foo")),
    AssignedRole(1,Character(CharacterId("100"),"Filler")),
    AssignedRole(2,Character(CharacterId("100"),"AREALLYLONGNAAAAAAAAAAAAAAAAAAAAAAAME")),
    AssignedRole(3,Character(CharacterId("101"),"Bar")),
    AssignedRole(4,Character(CharacterId("102"),"Baz")),
    AssignedRole(5,FakeLeader1)
  )

  val fake: List[Squad] = List(
    Squad(FakeLeader1,Squad.InfantryPreference,DefaultPatterns.basic,FakeMembers),
    Squad(FakeLeader2,Squad.ArmorPreference,DefaultPatterns.basic,List.empty),
    Squad(FakeLeader3,Squad.AirPreference,DefaultPatterns.basic,List.empty)
  )
}

case class AssignedRole(
  idx: Int,
  character: Character
)

case class Squad(
  leader: Character,
  preference: Squad.PatternTypePreference,
  pattern: Pattern,
  roles: List[AssignedRole]
)

object SquadRegister {
  import shared.AlgoPickler
  import Squad._

  AlgoPickler.register(InfantryPreference)
  AlgoPickler.register(ArmorPreference)
  AlgoPickler.register(AirPreference)
  AlgoPickler.register[CharacterId]
  AlgoPickler.register[Character]
  AlgoPickler.register[AssignedRole]
  AlgoPickler.register[Squad]
}
