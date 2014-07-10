package algorithm

import shared.models.AutoLogin
import shared.AlgoPickler
import scala.scalajs.js.Dynamic.{global => g}
import org.scalajs.dom
import scala.scalajs.js
import org.scalajs.spickling.jsany._


object CharacterJS {

  def storeLocal(auto: AutoLogin) = {
    val pickled = AlgoPickler.pickle(auto)
    val serialized = g.JSON.stringify(pickled).asInstanceOf[String]
    dom.window.localStorage.setItem("autologin",serialized)
  }

  def loadLocal(): Option[AutoLogin] = {
    val loaded = dom.window.localStorage.getItem("autologin")
    if(loaded.isInstanceOf[String]) {
      val parsed = g.JSON.parse(loaded).asInstanceOf[js.Any]
      Option(AlgoPickler.unpickle(parsed).asInstanceOf[AutoLogin])
    } else {
      None
    }
  }
}
