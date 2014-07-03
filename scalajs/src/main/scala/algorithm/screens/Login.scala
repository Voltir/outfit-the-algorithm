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

@JSExport
object Login {

  val characterId: Var[Option[String]] = Var(None)
  val characterName: Var[Option[String]] = Var(None)

  @JSExport
  def setSelected(name: String, cid: String): Unit = {
    characterId() = Option(cid)
    characterName() = Option(name)
  }

  def loginEnabled: Boolean = {
    characterId().isDefined && characterName().isDefined
  }

  val screen: HtmlTag = {

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
              val cid = CharacterId(inp.value)
              Nav.goto(SquadLink(cid))
            }
          )("Guest")
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
