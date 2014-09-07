package algorithm

import upickle._
import forceit._
import org.scalajs.dom
import shared.models.Pattern
import shared.models.DefaultPatterns

import scala.util.Try

object PatternJS {
  def storeLocal(patterns: Array[Pattern]): Unit = {
    val pickled = upickle.write(patterns.filter(_.custom))
    dom.window.localStorage.setItem("customPatterns",pickled)
  }

  def loadLocal(): Array[Pattern] = {
    val loaded = dom.window.localStorage.getItem("customPatterns")
    if(loaded.isInstanceOf[String]) {
      Try {
        val parsed = upickle.read[Array[Pattern]](loaded.asInstanceOf[String])
        DefaultPatterns.patterns ++ parsed
      } getOrElse {
        dom.window.localStorage.removeItem("customPatterns")
        DefaultPatterns.patterns
      }
    } else {
      DefaultPatterns.patterns
    }
  }
}
