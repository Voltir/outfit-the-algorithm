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
    (0,Character(CharacterId("100"),"Foo")),
    (1,Character(CharacterId("100"),"Filler")),
    (2,Character(CharacterId("100"),"AREALLYLONGNAAAAAAAAAAAAAAAAAAAAAAAME")),
    (3,Character(CharacterId("101"),"Bar")),
    (4,Character(CharacterId("102"),"Baz")),
    (5,FakeLeader1)
  )

  val fake: ArrayBuffer[Squad] = ArrayBuffer(
    Squad(FakeLeader1,Squad.InfantryPreference,DefaultPatterns.basic,FakeMembers),
    Squad(FakeLeader2,Squad.ArmorPreference,DefaultPatterns.basic,List.empty),
    Squad(FakeLeader3,Squad.AirPreference,DefaultPatterns.basic,List.empty)
  )
}

case class Squad(
  leader: Character,
  preference: Squad.PatternTypePreference,
  pattern: Pattern,
  roles: List[(Int,Character)]
)
//case class Leader(
//  character: Character,
//  preference: Squad.PatternTypePreference
//)

//case class Squad(
//  leader: Leader,
//  pattern: Pattern,
//  members: List[Character]
//)

/*
object PatternRegister {
  import shared.AlgoPickler
  import Pattern._
  AlgoPickler.register(InfantryType)
  AlgoPickler.register(AirType)
  AlgoPickler.register(ArmorType)
  AlgoPickler.register(MixedType)
  AlgoPickler.register(NoTeam)
  AlgoPickler.register(FireteamOne)
  AlgoPickler.register(FireteamTwo)
  AlgoPickler.register(FireteamThree)
  AlgoPickler.register(Unassigned)
  AlgoPickler.register(HeavyAssault)
  AlgoPickler.register(LightAssault)
  AlgoPickler.register(Medic)
  AlgoPickler.register(Engineer)
  AlgoPickler.register(MAX)
  AlgoPickler.register(Infiltraitor)
  AlgoPickler.register(Member)
  AlgoPickler.register(TeamLead)
  AlgoPickler.register[Assignment]
  AlgoPickler.register[Pattern]
}
*/
