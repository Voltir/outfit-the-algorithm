package algorithm.screens

import scalatags.JsDom._
import all._
import shared.models._
import algorithm.partials._

object Squads {

  val jumbo: HtmlTag = {
    div(`class`:="jumbotron")(
      h1("Squad Info Here"),
      p("The Squad You Are In")
    )
  }

  def squadSummary: HtmlTag = {
    div(cls:="clearfix")(
      div(cls:="row"),
      img(src:="http://placehold.it/32x32", float:="left",marginRight:=10.px),
      div(cls:="squad-info",float:="left")(
        div(p("Squad Name -- Pattern Type")),
        div(p("Squad Leader"))
      )
    )
  }

  def available: HtmlTag = {
    div(cls:="squads-available")(
      h3("Squads"),
      ul(cls:="list-group")(
        li(cls:="list-group-item squad-summary squad-infantry")(squadSummary),
        li(cls:="list-group-item squad-summary squad-armor")(squadSummary),
        li(cls:="list-group-item squad-summary squad-air")(squadSummary)
      )
    )
  }

  //TODO -- create "styles" partial
  val styles: HtmlTag = tags2.style("media".attr:="screen", `type`:="text/css")(
    """
      .fireteam-one { border: 5px solid red; }
      .fireteam-two { border: 5px solid blue; }
      .fireteam-three { border: 5px solid green; }
      .squad-summary { border: 2px solid; }
      .squad-infantry {
        //rgb(75,0,130)
        //rgb(86,60,92)
        border-color: rgb(75,0,130);
        background: rgba(75,0,130,0.5);
      }
      .squad-armor {
        border-color: rgb(0,0,0);
        background: rgba(0,0,0,0.3);
      }
      .squad-air {
        border-color: rgb(0,204,204);
        background: rgba(0,204,204,0.3);
      }
    """)

  val screen: HtmlTag = {
    div(
      styles,
      Nav.header,
      jumbo,
      div(cls:="col-xs-4")(available),
      div(cls:="col-xs-5")(div("PUT COLUMNS HERE")),
      div(cls:="col-xs-3")(div("PUT COLUMNS HERE"))
    )
  }
}
