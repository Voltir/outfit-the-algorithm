package algorithm

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import org.scalajs.dom
import org.scalajs.spickling.jsany._
import shared.models.Pattern
import shared.AlgoPickler
import shared.models.Pattern.Assignment

object PatternJS {

  def storeLocal(patterns: Array[Pattern]): Unit = {
    val pickled = AlgoPickler.pickle(patterns.filter(_.custom))
    val serialized = g.JSON.stringify(pickled).asInstanceOf[String]
    dom.window.localStorage.setItem("customPatterns",serialized)
  }

  def loadLocal(): Array[Pattern] = {
    val loaded = dom.window.localStorage.getItem("customPatterns")
    if(loaded.isInstanceOf[String]) {
      val parsed = g.JSON.parse(loaded).asInstanceOf[js.Any]
      DefaultPatterns.patterns ++ AlgoPickler.unpickle(parsed).asInstanceOf[Array[Pattern]]
    } else {
      DefaultPatterns.patterns
    }
  }
}

object DefaultPatterns {
  import Pattern._

  val basic = Pattern("Basic", false, InfantryType, Array(
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

  val standard = Pattern("Standard", false, InfantryType, Array(
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

  val patterns: Array[Pattern] = Array(basic,standard)

  lazy val names = patterns.map(_.name).toSet
}
