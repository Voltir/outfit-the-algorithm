package algorithm

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import scalatags.JsDom._
import all._
import algorithm.partials._
import rx._
import framework.Framework._

import algorithm.screens._
import shared.models._
//import scala.concurrent.Future



object AlgorithmJS extends js.JSApp {

  val patterns: Var[Array[Pattern]] = Var(Array.empty)

  val contentTag: Rx[HtmlTag] = Rx {
    Nav.currentLink() match {
      case Some(SquadLink(cid)) => { Squads.screen }

      case Some(PreferenceLink) => div("PREF PREF PREF")(
        a(
          href:="#",
          onclick := { () => Nav.goto(SquadLink(CharacterId("Wat")))},
          "BACK TO SQUAD"
        )
      )

      case Some(CreatePatternLink) => {
        dom.console.log("Start..")
        val result = CreatePattern.screen
        dom.console.log("End")
        result
      }

      case _ => Squads.screen//Login.screen
    }
  }

  def main(): Unit = {
    //Test Pickler..
    /*
    import org.scalajs.spickling.jsany._
    import shared.AlgoPickler
    val testAssignment =
      shared.models.Pattern.Assignment(Pattern.HeavyAssault,Pattern.NoTeam,Pattern.Member,"YOLOSWAG")
    val wat: js.Any = AlgoPickler.pickle(testAssignment)
    val stored = g.JSON.stringify(wat).asInstanceOf[String]
    dom.window.localStorage.setItem("foo1",stored)
    val loaded = dom.window.localStorage.getItem("foo1").asInstanceOf[String]
    val parsed = g.JSON.parse(loaded).asInstanceOf[js.Any]
    val omg = AlgoPickler.unpickle(parsed).asInstanceOf[shared.models.Pattern.Assignment]
    println(omg)

    val test2 = Array(testAssignment)
    println("???")
    println(test2)
    val wat2 = AlgoPickler.pickle(test2)
    val stored2 = g.JSON.stringify(wat2).asInstanceOf[String]
    println(stored2)
    println("HERER WE GO")
    val omg2 = AlgoPickler.unpickle(wat2).asInstanceOf[Array[shared.models.Pattern.Assignment]]
    println(omg2)

    val testPat = Pattern("test",true,Pattern.InfantryType,test2,Option(40))
    val patPickle = AlgoPickler.pickle(testPat)
    dom.console.log(patPickle)
    val patUnpickle = AlgoPickler.unpickle(patPickle)
    println(patUnpickle)

    val test3 = Array(testPat)
    val apatPickle  = AlgoPickler.pickle(test3)
    val serialized = g.JSON.stringify(apatPickle).asInstanceOf[String]
    dom.window.localStorage.setItem("customPatterns",serialized)
    dom.console.log(apatPickle)
    val apatUnpickle = AlgoPickler.unpickle(apatPickle)
    println(apatUnpickle)
    */

    patterns() = PatternJS.loadLocal()
    println(patterns().size)
    val content = dom.document.getElementById("content")
    content.appendChild(div(contentTag).render)
  }

}