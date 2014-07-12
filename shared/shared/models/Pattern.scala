package shared.models

object Pattern {

  trait PatternType
  case object InfantryType extends PatternType
  case object AirType extends PatternType
  case object ArmorType extends PatternType
  case object MixedType extends PatternType

  trait Fireteam
  case object NoTeam extends Fireteam
  case object FireteamOne extends Fireteam
  case object FireteamTwo extends Fireteam
  case object FireteamThree extends Fireteam

  sealed trait Role

  case object Unassigned extends Role

  trait InfantryRole extends Role
  case object HeavyAssault extends InfantryRole
  case object LightAssault extends InfantryRole
  case object Medic extends InfantryRole
  case object Engineer extends InfantryRole
  case object MAX extends InfantryRole
  case object Infiltraitor extends InfantryRole

  trait ArmorRole extends Role
  case object MBTDriver extends ArmorRole
  case object MBTGunner extends ArmorRole
  case object Lightning extends ArmorRole
  case object HarasserDriver extends ArmorRole
  case object HarasserGunner extends ArmorRole
  case object SundererDriver extends ArmorRole
  case object SundererGunner extends ArmorRole

  trait AirRole extends Role
  case object Scythe extends AirRole
  case object LiberatorPilot extends AirRole
  case object LiberatorGunner extends AirRole
  case object GalaxyPilot extends AirRole
  case object GalaxyGunner extends AirRole

  trait AssignmentType
  case object Member extends AssignmentType
  case object TeamLead extends AssignmentType

  case class Assignment(
    role: Role,
    team: Fireteam,
    `type`: AssignmentType,
    details: String = ""
  )

  def asString(role: Role) = role match {
    case Unassigned => "Unassigned"
    case HeavyAssault => "Heavy Assault"
    case LightAssault => "Light Assault"
    case Medic => "Medic"
    case Engineer => "Engineer"
    case MAX => "MAX"
    case Infiltraitor => "Infiltraitor"
    case MBTDriver => "Magrider Driver"
    case MBTGunner => "Magrider Gunner"
    case Lightning => "Lightning"
    case HarasserDriver => "Harasser Driver"
    case HarasserGunner => "Harasser Gunner"
    case SundererDriver => "Sunderer Driver"
    case SundererGunner => "Sunderer Gunner"
    case Scythe => "Scythe"
    case LiberatorPilot => "Liberator Pilot"
    case LiberatorGunner => "Liberator Gunner"
    case GalaxyPilot => "Galaxy Pilot"
    case GalaxyGunner => "Galaxy Gunner"
  }

  def iconUrl(role: Role) = role match {
    case HeavyAssault => "http://wiki.planetside-universe.com/ps/images/c/c3/Icon_black_heavyAssault_64.png"
    case LightAssault => "http://wiki.planetside-universe.com/ps/images/9/9d/Icon_black_lightAssault_64.png"
    case Medic => "http://wiki.planetside-universe.com/ps/images/5/51/Icon_black_medic_64.png"
    case Engineer => "http://wiki.planetside-universe.com/ps/images/8/8c/Icon_black_engineer_64.png"
    case MAX => "http://wiki.planetside-universe.com/ps/images/f/f9/Icon_black_max_64.png"
    case Infiltraitor => "http://wiki.planetside-universe.com/ps/images/7/72/Icon_black_infiltrator_64.png"
    case _ => "http://placehold.it/64x64"
  }
}

case class Pattern(
  name: String,
  custom: Boolean,
  assignments: Array[Pattern.Assignment],
  cooldown: Option[Int]
)

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
  AlgoPickler.register(MBTDriver)
  AlgoPickler.register(MBTGunner)
  AlgoPickler.register(Lightning)
  AlgoPickler.register(HarasserDriver)
  AlgoPickler.register(HarasserGunner)
  AlgoPickler.register(SundererDriver)
  AlgoPickler.register(SundererGunner)
  AlgoPickler.register(Scythe)
  AlgoPickler.register(LiberatorPilot)
  AlgoPickler.register(LiberatorGunner)
  AlgoPickler.register(GalaxyPilot)
  AlgoPickler.register(GalaxyGunner)
  AlgoPickler.register(Member)
  AlgoPickler.register(TeamLead)
  AlgoPickler.register[Assignment]
  AlgoPickler.register[Pattern]
}

object DefaultPatterns {

  import Pattern._

  val basic = Pattern("Basic", false, Array(
    Assignment(HeavyAssault,NoTeam,Member),
    Assignment(HeavyAssault,NoTeam,Member),
    Assignment(HeavyAssault,NoTeam,Member),
    Assignment(Medic,NoTeam,Member),
    Assignment(Medic,NoTeam,Member),
    Assignment(Engineer,NoTeam,Member),
    Assignment(HeavyAssault,NoTeam,Member),
    Assignment(HeavyAssault,NoTeam,Member),
    Assignment(HeavyAssault,NoTeam,Member),
    Assignment(Medic,NoTeam,Member),
    Assignment(Medic,NoTeam,Member),
    Assignment(Infiltraitor,NoTeam,Member)
  ),Option(10))

  val standard = Pattern("Standard", false, Array(
    Assignment(HeavyAssault,FireteamOne,TeamLead),
    Assignment(HeavyAssault,FireteamOne,Member),
    Assignment(HeavyAssault,FireteamOne,Member),
    Assignment(Medic,FireteamOne,Member),
    Assignment(Medic,FireteamOne,Member),
    Assignment(Engineer,FireteamThree,TeamLead),
    Assignment(HeavyAssault,FireteamTwo,TeamLead),
    Assignment(HeavyAssault,FireteamTwo,Member),
    Assignment(HeavyAssault,FireteamTwo,Member),
    Assignment(Medic,FireteamTwo,Member),
    Assignment(Medic,FireteamTwo,Member),
    Assignment(Infiltraitor,FireteamThree,Member)
  ),Option(10))

  val crash = Pattern("Crash",false,Array(
    Assignment(MAX,NoTeam,Member),
    Assignment(MAX,NoTeam,Member),
    Assignment(MAX,NoTeam,Member),
    Assignment(Engineer,NoTeam,Member),
    Assignment(Engineer,NoTeam,Member),
    Assignment(Medic,NoTeam,Member),
    Assignment(MAX,NoTeam,Member),
    Assignment(MAX,NoTeam,Member),
    Assignment(MAX,NoTeam,Member),
    Assignment(MAX,NoTeam,Member),
    Assignment(Engineer,NoTeam,Member),
    Assignment(Medic,NoTeam,Member)
  ),Option(10))

  val patterns: Array[Pattern] = Array(basic,standard,crash)

  lazy val names = patterns.map(_.name).toSet
}
