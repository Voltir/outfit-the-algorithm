package shared.models
import scala.collection.mutable.ArrayBuffer

object Squad {
  sealed trait PatternTypePreference
  case object InfantryPreference extends PatternTypePreference
  case object ArmorPreference extends PatternTypePreference
  case object AirPreference extends PatternTypePreference

  def patternTypeIcon(pref:PatternTypePreference): String = pref match {
    case InfantryPreference => "http://img2.wikia.nocookie.net/__cb20121015012350/planetside2/images/c/c8/Icon_resource_alloys_128_Infantry.png"
    case ArmorPreference => "http://img2.wikia.nocookie.net/__cb20121015012335/planetside2/images/7/70/Icon_resource_catalysts_128_Mechanized.png"
    case AirPreference => "http://img4.wikia.nocookie.net/__cb20121015012319/planetside2/images/b/b2/Icon_resource_polymers_128_Aerospace.png"
  }

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