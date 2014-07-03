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
  `type`: Pattern.PatternType,
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
  AlgoPickler.register(Member)
  AlgoPickler.register(TeamLead)
  AlgoPickler.register[Assignment]
  AlgoPickler.register[Pattern]
}