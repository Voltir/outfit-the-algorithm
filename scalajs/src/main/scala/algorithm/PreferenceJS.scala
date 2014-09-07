package algorithm

import upickle._
import forceit._
import org.scalajs.dom
import shared.models.PreferenceDefinition

import scala.util.Try

object PreferenceJS {

  def storeLocal(prefs: PreferenceDefinition): Unit = {
    val pickled = upickle.write(prefs)
    dom.window.localStorage.setItem("user-prefs",pickled)
  }

  def loadLocal(): Option[PreferenceDefinition] = {
    val loaded = dom.window.localStorage.getItem("user-prefs")
    if(loaded.isInstanceOf[String]) {
      Try {
        val parsed = upickle.read[PreferenceDefinition](loaded.asInstanceOf[String])
        Option(parsed)
      } getOrElse {
        dom.window.localStorage.removeItem("user-prefs")
        None
      }
    } else {
      None
    }
  }
}
