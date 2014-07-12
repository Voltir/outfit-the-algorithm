package algorithm

import shared.models.PreferenceDefinition
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import org.scalajs.dom
import org.scalajs.spickling.jsany._
import shared.AlgoPickler

object PreferenceJS {

  def storeLocal(prefs: PreferenceDefinition): Unit = {
    val pickled = AlgoPickler.pickle(prefs)
    val serialized = g.JSON.stringify(pickled).asInstanceOf[String]
    dom.window.localStorage.setItem("user-prefs",serialized)
  }

  def loadLocal(): Option[PreferenceDefinition] = {
    val loaded = dom.window.localStorage.getItem("user-prefs")
    if(loaded.isInstanceOf[String]) {
      val parsed = g.JSON.parse(loaded).asInstanceOf[js.Any]
      Option(AlgoPickler.unpickle(parsed).asInstanceOf[PreferenceDefinition])
    }else{
      None
    }
  }
}
