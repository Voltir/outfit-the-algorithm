package algorithm.screens

import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js
import scalatags.JsDom.all._
import scalatags.JsDom.tags2
import org.scalajs.dom
import algorithm.partials.{VoiceTestLink, Nav}
import algorithm.framework.Framework._
import rx._
import algorithm.AlgorithmJS

object VoiceTest {

  val lastCmd: Var[String] = Var("No voice command was issued. There is a 1-2 second delay while processing the command.")

  val voiceInfo: Rx[HtmlTag] = Rx {
    div(cls:="col-md-7")(
      tags2.style("media".attr:="screen", `type`:="text/css")(
        """
          .voice-command {
            color: #d58512;
            font-style:italic;
          }
        """
      ),
      h5("Available Voice Commands (requires Chrome):"),
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
        }.toSeq ++ {
        if(AlgorithmJS.isSquadLeader() || Nav.currentLink() == Option(VoiceTestLink)) {
          AlgorithmJS.commandHelp.filter(_._2.leaderOnly).toList.sortBy(_._1).grouped(5).map {
            chunk =>
              div(float := "left", marginRight := 3.rem)(
                chunk.map(help => h5(
                  cls := "voice-command",
                  "data-toggle".attr := "tooltip",
                  "data-placement".attr := "top",
                  cursor := "pointer",
                  "title".attr := help._2.description,
                  help._1
                ))
              )
          }.toSeq
        } else {
          Seq.empty
        }}
      )
    )
  }

  val jumbo: Rx[HtmlTag] = Rx {
    div(`class`:="jumbotron row")(
      div(cls:="col-md-5")(
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
