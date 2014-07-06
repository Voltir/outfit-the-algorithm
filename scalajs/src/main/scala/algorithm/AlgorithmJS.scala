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

  var user: Option[Character] = Option(Character(CharacterId("testid"),"Testerson"))

  val contentTag: Rx[HtmlTag] = Rx {
    Nav.currentLink() match {
      case Some(SquadLink(cid)) => Squads.screen

      case Some(PreferenceLink) => div("PREF PREF PREF")(
        a(
          href:="#",
          onclick := { () => Nav.goto(SquadLink(CharacterId("Wat")))},
          "BACK TO SQUAD"
        )
      )

      case Some(CreatePatternLink) => CreatePattern.screen

      case _ => Squads.screen//Login.screen
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
    println("OPENED!")
    send(LoadInitial)
  }

  private def setupSocket(cid: CharacterId) = {
    algosocket = new WebSocket(s"ws://localhost:9000/ws/$cid")
    algosocket.asInstanceOf[js.Dynamic].onmessage = onAlgoMessage _
    algosocket.asInstanceOf[js.Dynamic].onopen = onAlgoOpen _
  }

  def send(cmd: Commands) = {
    val msg = g.JSON.stringify((AlgoPickler.pickle(cmd))).asInstanceOf[String]
    println(s"Msg: $msg")
    algosocket.send(msg)
  }

  def main(): Unit = {

    setupSocket(user.get.cid)
    
    patterns() = PatternJS.loadLocal()
    println(patterns().size)
    val content = dom.document.getElementById("content")
    content.appendChild(div(contentTag).render)
  }

}
