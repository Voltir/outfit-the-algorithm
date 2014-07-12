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
import scala.collection.mutable.{Map => MutableMap}

case class CommandHelp(
  description: String,
  leaderOnly: Boolean
)

object AlgorithmJS extends js.JSApp {

  private var algosocket: WebSocket = _

  val patterns: Var[Array[Pattern]] = Var(Array.empty)

  var user: Var[Option[Character]] = Var(None)

  val commandHelp: MutableMap[String,CommandHelp] = MutableMap.empty

  val isSquadLeader: Var[Boolean] = Var(false)

  val contentTag: Rx[HtmlTag] = Rx {
    Nav.currentLink() match {
      case Some(SquadLink) => Squads.screen
      case Some(PreferenceLink) => EditPreferences.screen
      case Some(CreatePatternLink) => CreatePattern.screen
      case Some(VoiceTestLink) => VoiceTest.screen
      case _ => Login.screen
    }
  }

  val annyang = g.annyang.asInstanceOf[js.Dynamic]

  private def setVoiceCommands = {

    def checkVoiceTest(cmd: String)(f: () => Unit) = { () =>
      if(Nav.currentLink.now == Option(VoiceTestLink)) {
        VoiceTest.lastCmd() = cmd
      } else {
        f()
      }
    }

    commandHelp.clear()

    if(annyang.isInstanceOf[js.Object]) {
      annyang.debug()
      annyang.removeCommands()
      val defaults = js.Dynamic.literal(
        "help roll"->checkVoiceTest("Help Role"){() => Squads.sayAssignment}
      )
      commandHelp ++= Seq(
        "Help Role" -> CommandHelp("Repeats current role",false)
      )
      dom.console.log(defaults)
      annyang.addCommands(defaults)
      patterns().map { pattern =>
        annyang.addCommands(js.Dynamic.literal(s"pattern ${pattern.name.toLowerCase}" -> checkVoiceTest(s"Pattern ${pattern.name}"){ () =>
          send(SetPattern(pattern))
        }))
        commandHelp.put(s"Pattern ${pattern.name}",CommandHelp(s"Assigns current squad to Pattern ${pattern.name}",true))
      }
    }
  }

  val patternObs = Obs(patterns) {
    setVoiceCommands
  }

  private def onAlgoMessage(event: js.Any): Unit = {
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
    send(LoadInitial(PreferenceDefinition(List.empty)))
  }

  private def onAlgoClose(event: js.Any): Unit = {
    AlgorithmJS.user().map { user =>
      CharacterJS.storeLocal(AutoLogin(user,false))
      Nav.goto(LoginLink)
    }
  }

  def setupSocket(character: Character) = {
    algosocket = new WebSocket(g.jsRoutes.controllers.Application.ws(character.cid.txt,character.name).webSocketURL(true).asInstanceOf[String])
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
    annyang.start()
  }

}
