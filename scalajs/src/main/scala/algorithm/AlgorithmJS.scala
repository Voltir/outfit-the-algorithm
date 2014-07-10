package algorithm

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import scalatags.JsDom._
import all._
import algorithm.partials._
import rx._
import framework.Framework._
import framework.WebSocket

import algorithm.screens._
import shared.models._
import shared.commands._
import org.scalajs.spickling.jsany._
import shared.AlgoPickler
//import scala.concurrent.Future

object AlgorithmJS extends js.JSApp {

  private var algosocket: WebSocket = _

  val patterns: Var[Array[Pattern]] = Var(Array.empty)

  var user: Var[Option[Character]] = Var(None)

  val contentTag: Rx[HtmlTag] = Rx {
    Nav.currentLink() match {
      case Some(SquadLink) => Squads.screen

      case Some(PreferenceLink) => div("PREF PREF PREF")(
        a(
          href:="#",
          onclick := { () => Nav.goto(SquadLink)},
          "BACK TO SQUAD"
        )
      )

      case Some(CreatePatternLink) => CreatePattern.screen

      case _ => Login.screen
    }
  }

  private def onAlgoMessage(event: js.Any): Unit = {
    println("~~~ RECIEVED ~~~")
    dom.console.log(event.asInstanceOf[js.Dynamic].data)
    val response = AlgoPickler.unpickle(g.JSON.parse(event.asInstanceOf[js.Dynamic].data).asInstanceOf[js.Any])
    response match {
      case LoadInitialResponse(squads,unassigned) => Squads.reload(squads,unassigned)
      case SquadUpdate(squad) => Squads.update(squad)
      case Unassigned(unassigned) => Squads.update(unassigned)
      case unk => println("UNKNOWN MESSAGE RECEIVED: ",unk)
    }
  }

  private def onAlgoOpen(event: js.Any): Unit = {
    send(LoadInitial)
  }

  private def onAlgoClose(event: js.Any): Unit = {
    AlgorithmJS.user().map { user =>
      CharacterJS.storeLocal(AutoLogin(user,false))
      Nav.goto(LoginLink)
    }
  }

  def setupSocket(character: Character) = {
    algosocket = new WebSocket(s"ws://localhost:9000/ws/${character.cid}/${character.name}")
    algosocket.asInstanceOf[js.Dynamic].onmessage = onAlgoMessage _
    algosocket.asInstanceOf[js.Dynamic].onopen = onAlgoOpen _
    algosocket.asInstanceOf[js.Dynamic].onerror = onAlgoClose _
    algosocket.asInstanceOf[js.Dynamic].onclose = onAlgoClose _
  }

  def send(cmd: Commands) = {
    val msg = g.JSON.stringify((AlgoPickler.pickle(cmd))).asInstanceOf[String]
    algosocket.send(msg)
  }

  def main(): Unit = {
    patterns() = PatternJS.loadLocal()
    val content = dom.document.getElementById("content")
    content.appendChild(div(contentTag).render)
  }

}
