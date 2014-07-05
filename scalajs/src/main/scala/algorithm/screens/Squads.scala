package algorithm.screens

import scalatags.JsDom._
import all._
import shared.models._
import algorithm.partials._
import rx._
import scala.collection.mutable.ArrayBuffer
import algorithm.framework.Framework._

object Squads {

  val squads: Var[ArrayBuffer[Squad]] = Var(Squad.fake)
  
  val unassigned: Var[List[Character]] = Var(List.empty)

  val selected: Var[Int] = Var(-1)

  val jumbo: HtmlTag = {
    div(`class`:="jumbotron")(
      h1("Squad Info Here"),
      p("The Squad You Are In")
    )
  }

  def squadSummary(squad: Squad, idx: Int): HtmlTag = {
    div(cls:="clearfix",cursor:="pointer")(
      onclick := { () => selected() = idx },
      div(cls:="row")(
        img(src:="http://placehold.it/32x32", float:="right",marginRight:=10.px),
        div(cls:="squad-info",float:="left",marginLeft:=10.px)(
          div(h4(s"${squad.leader.name}'s Squad")),
          div(h5(s"Pattern ${squad.pattern.name}"))
        )
      ),
      div(cls:="row")(
        div(cls:="squad-members")(
          ul(squad.roles.sortBy(_._1).map { case (idx,char) =>
            val assignment = squad.pattern.assignments(idx)
            li(cls:="squad-member clearfix")(
              img(src:=Pattern.iconUrl(assignment.role),float:="left",height:=16.px,width:=16.px,marginRight:=2.px),
              char.name.take(5)
            )
          })
        )
      )
    )
  }

  val available: Rx[HtmlTag] = Rx {
    div(cls:="squads-available")(
      h3("Squads"),
      ul(cls:="list-group")(squads().zipWithIndex.map { case (s,idx) =>
        val prefcls = s.preference match {
          case Squad.InfantryPreference   => "squad-infantry"
          case Squad.ArmorPreference      => "squad-armor"
          case Squad.AirPreference        => "squad-air"
        }
        li(cls:=s"list-group-item squad-summary $prefcls")(squadSummary(s,idx))
      })
    )
  }

  val temp: HtmlTag = {
    div("Temporary Buttons -- ")(
      p(a(
        href:="#",
        "Add to squad",
        onclick := { () =>
          squads().append(Squad(Squad.FakeLeader2,Squad.InfantryPreference,DefaultPatterns.basic,List.empty))
          squads.propagate()
          false
        }
      )),
      p(a(
        href:="#",
        "Remove squad",
        onclick := { () =>
          squads() = squads().tail
        }
      ))
    )
  }

  def selectedDetails(squad: Squad): HtmlTag = {
    div(
      h4(s"Pattern ${squad.pattern.name}"),
      div(cls:="detail-assignments")(
        ul(cls:="list-group",squad.pattern.assignments.zipWithIndex.map { case (assignment,idx) =>
          val member = squad.roles.find(_._1 == idx).map(_._2)
          li(cls:="list-group-item view-assignmentm clearfix")(
            div(cls:="row")(
              img(src:=Pattern.iconUrl(assignment.role),float:="left",margin:=5.px),
              div(cls:="infoblock",float:="left")(
                div(member.map { m =>
                  h4(s"${m.name.take(25)}")
                }.getOrElse {
                  h4("Unassigned")(button(cls:="btn-warning btn-xs",margin:=5.px)("Pin"))
                }),
                div(h5(s"${Pattern.asString(assignment.role)}")),
                //div(p(fireteam)),
                div(p(assignment.details))
              )
            )
          )
        })
      )
    )
  }

  val selectedTag: Rx[HtmlTag] = Rx {
    if(squads().isDefinedAt(selected())) { 
      val squad = squads()(selected())
      div(cls:="selected-squad")(
        h3(s"${squad.leader.name}'s Squad"),
        selectedDetails(squad)
      )
    } else {
      div(cls:="selected-squad")(
        h3("No Squad Selected")
      )
    }
  }

  //TODO -- create "styles" partial
  val styles: HtmlTag = tags2.style("media".attr:="screen", `type`:="text/css")(
    """
      .fireteam-one { border: 5px solid red; }
      .fireteam-two { border: 5px solid blue; }
      .fireteam-three { border: 5px solid green; }
      .squad-summary { border: 2px solid; }
      .squad-infantry {
        border-color: rgb(0,0,0);
        background: rgba(127,127,127,1.0);
        color: white;
      }
      .squad-armor {
        border-color: rgb(0,0,0);
        background: rgba(0,0,0,1.0);
        color: white;
      }
      .squad-air {
        border-color: rgb(0,0,0);
        background: rgba(255,255,255,1.0);
        color: black;
      }

      .squad-member {
        display: inline-block;
        vertical-align: top;
        width: 23%;
        height: 2em;
        overflow: hidden;
        margin: 0.2em;
      }
    """)

  val screen: HtmlTag = {
    div(
      styles,
      Nav.header,
      jumbo,
      div(cls:="col-xs-4")(Rx { available }),
      div(cls:="col-xs-5")(Rx { selectedTag }),
      div(cls:="col-xs-3")(temp)
    )
  }
}
