package algorithm.screens

import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js
import scalatags.JsDom.all._
import scalatags.JsDom.tags2
import org.scalajs.dom
import algorithm.partials.Nav
import algorithm.framework.Framework._
import rx._
import algorithm.AlgorithmJS

object VoiceTest {

  val lastCmd: Var[String] = Var("No voice command was issued. There is a 1-2 second delay while processing the command.")

  val voiceInfo: Rx[HtmlTag] = Rx {
    println(Nav.currentLink())
    div(cls:="col-xs-7")(
      tags2.style("media".attr:="screen", `type`:="text/css")(
        """
          .voice-command {
            color: #d58512;
            font-style:italic;
          }
        """
      ),
      h5("Available Voice Commands:"),
      div(cls:="clearfix")(
        AlgorithmJS.commandHelp.filter(!_._2.leaderOnly).toList.sortBy(_._1).grouped(5).map { chunk =>
          div(float:="left", marginRight:=3.rem)(
            chunk.map( help => h5(
              cls:="voice-command",
              "data-toggle".attr:="tooltip",
              "data-placement".attr:="top",
              cursor:="pointer",
              "title".attr:=help._2.description,
              help._1
            ))
          )
        }.toSeq ++
        AlgorithmJS.commandHelp.filter(_._2.leaderOnly).toList.sortBy(_._1).grouped(5).map { chunk =>
          div(float:="left", marginRight:=3.rem)(
            chunk.map( help => h5(
              cls:="voice-command",
              "data-toggle".attr:="tooltip",
              "data-placement".attr:="top",
              cursor:="pointer",
              "title".attr:=help._2.description,
              help._1
            ))
          )
        }.toSeq
      )
    )
  }

  val jumbo: Rx[HtmlTag] = Rx {
    div(`class`:="jumbotron row")(
      div(cls:="col-xs-5")(
        h3("Test Voice Commands:"),
        p(lastCmd())
      ),
      voiceInfo
    )
  }

  val screen: HtmlTag = {
    div(
      Nav.header,
      Rx { jumbo }
    )
  }
}
