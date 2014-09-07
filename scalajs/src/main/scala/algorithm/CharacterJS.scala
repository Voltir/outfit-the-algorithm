package algorithm

import shared.models.AutoLogin
import scala.scalajs.js.Dynamic.{global => g}
import org.scalajs.dom
import scala.scalajs.js
import scala.util.Try


object CharacterJS {

  def storeLocal(auto: AutoLogin) = {
    val pickled = upickle.write(auto)
    dom.window.localStorage.setItem("autologin",pickled)
  }

  def loadLocal(): Option[AutoLogin] = {
    val loaded = dom.window.localStorage.getItem("autologin")
    if(loaded.isInstanceOf[String]) {
      Try {
        val parsed = upickle.read[AutoLogin](loaded.asInstanceOf[String])
        Option(parsed)
      } getOrElse {
        dom.window.localStorage.removeItem("autologin")
        None
      }
    } else {
      None
    }
  }
}
