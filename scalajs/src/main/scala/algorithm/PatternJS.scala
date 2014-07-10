package algorithm

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import org.scalajs.dom
import org.scalajs.spickling.jsany._
import shared.models.Pattern
import shared.AlgoPickler
import shared.models.Pattern.Assignment
import shared.models.DefaultPatterns

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
