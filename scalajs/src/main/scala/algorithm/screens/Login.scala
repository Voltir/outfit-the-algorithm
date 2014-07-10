package algorithm.screens

import scalatags.JsDom.all._
import shared.models._
import algorithm.partials._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import rx._
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport
import algorithm.framework.Framework._
import algorithm.{CharacterJS, AlgorithmJS}

@JSExport
object Login {

  @JSExport
  def setSelected(name: String, cid: String): Unit = {
    AlgorithmJS.user() = Option(Character(CharacterId(cid),name))
  }

  def loginEnabled: Boolean = {
    AlgorithmJS.user().isDefined
  }

  val screen: HtmlTag = {
    CharacterJS.loadLocal().map { auto =>
      if(auto.enabled) {
        setSelected(auto.character.name,auto.character.cid.txt)
        AlgorithmJS.setupSocket(AlgorithmJS.user().get)
        dom.setTimeout(() => Nav.goto(SquadLink),10)
      }
    }

    val inp = input(
      id:="lookup"
    ).render

    div(`class`:="jumbotron row")(
      div(cls:="col-xs-6")(
        h1("The Algorithm"),
        div(`class`:="ui-widget")(
          label(`for`:="lookup", "Select Character:"),
          p(inp),
          a(
            id:="register",
            `class`:= Rx { s"btn btn-lg btn-primary ${if(!loginEnabled) "disabled" else ""}" },
            href:="#",
            onclick := { () =>
              CharacterJS.storeLocal(AutoLogin(AlgorithmJS.user().get,true))
              AlgorithmJS.setupSocket(AlgorithmJS.user().get)
              Nav.goto(SquadLink)
            }
          )(Rx { if(loginEnabled) "Sign In" else "Select User From Dropdown" })
        )
      ),
      div(cls:="col-xs-6")(
        p("Welcome to The Algorithm. This is a Planetside 2 Squad Management tool that utilizes voice activated commands and audio alerts to coordinate roles and assignments across multiple squads."),
        p("Simply type in your ingame character name (Connery Vanu only) hit guest and leave this page open in the background."),
        p("If this is your first time using The Algorithm, you will need to fill out your individual player preference (see next screen).")

      )
    )
  }


}
