package algorithm.screens

import scalatags.JsDom._
import all._
import shared.models._
import shared.commands._
import algorithm.partials._
import rx._
import scala.collection.mutable.ArrayBuffer
import algorithm.framework.Framework._
import algorithm.AlgorithmJS
import scala.collection.mutable.{Map => MutableMap}
import scala.scalajs.js
import org.scalajs.dom.{DragEvent, Event, HTMLElement, MouseEvent}
import org.scalajs.dom

case class MyAssignment(
  lid: CharacterId,
  assignment: Pattern.Assignment
)

object Squads {

  val squads: Var[MutableMap[CharacterId,Squad]] =  Var(MutableMap.empty)

  val unassigned: Var[List[Character]] = Var(List.empty)

  val selected: Var[Option[CharacterId]] = Var(None)

  val current: Var[Option[MyAssignment]] = Var(None)

  def checkForAssignment(squad: Squad) = {
    squad.roles.find { r => 
      Option(r.character.cid) == AlgorithmJS.user.map(_.cid)
    }.map { role =>
      val assignment = squad.pattern.assignments(role.idx)
      current() = Option(MyAssignment(squad.leader.cid,assignment))
    }
  }

  def reload(squadData: List[Squad], unassignedData: List[Character]) = {
    squads().clear
    squadData.foreach { s =>
      squads().put(s.leader.cid,s)
      checkForAssignment(s)
    }
    squads.propagate()
    unassigned() = unassignedData
  }

  def update(squad: Squad) = {
    checkForAssignment(squad)
    squads().put(squad.leader.cid,squad)
    squads.propagate()
  }

  def update(updated: List[Character]) = {
    unassigned() = updated
  }
  
  val jumbo: Rx[HtmlTag] = Rx {
    div(`class`:="jumbotron")(
      h1("Squad Info Here"),
      p(s"${current()}")
    )
  }

  def squadSummary(squad: Squad): HtmlTag = {
    div(cls:="clearfix",cursor:="pointer")(
      onclick := { () => selected() = Option(squad.leader.cid) },
      div(cls:="row")(
        img(src:="http://placehold.it/32x32", float:="right",marginRight:=10.px),
        if(Option(squad.leader.cid) != current().map(_.lid)) {
          button(
            "Join",
            cls := "btn btn-warning btn-xs",
            float := "right",
            marginRight := 10.px,
            onclick := {
              () =>
                AlgorithmJS.send(JoinSquad(squad.leader.cid))
                false
            }
          )
        } else {
          span()
        },
        div(cls:="squad-info",float:="left",marginLeft:=10.px)(
          div(h4(s"${squad.leader.name}'s Squad")),
          div(h5(s"Pattern ${squad.pattern.name}"))
        )
      ),
      div(cls:="row")(
        div(cls:="squad-members")(
          ul(squad.roles.sortBy(_.idx).map { case AssignedRole(idx,char) =>
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
      ul(cls:="list-group")(squads().toList.map(_._2).map { s =>
        val prefcls = s.preference match {
          case Squad.InfantryPreference   => "squad-infantry"
          case Squad.ArmorPreference      => "squad-armor"
          case Squad.AirPreference        => "squad-air"
        }
        val wat = li(
          cls:=s"list-group-item squad-summary $prefcls",
          "ondragover".attr := { (event:DragEvent) =>
            event.preventDefault()
            false
          },
          "ondrop".attr := { {(jsThis:HTMLElement,event: DragEvent) =>
            val txt = event.dataTransfer.getData("cid")
            val cid = CharacterId(txt)
            println(s"I WAS DROPPED: ${s.leader.name}")
            println(s"NICE ${cid}")
            jsThis.classList.remove("over")
            true
          }: js.ThisFunction1[HTMLElement,DragEvent,Boolean] }
        )(squadSummary(s)).render
        dom.setTimeout(() => {
          dom.console.log(js.Dynamic.newInstance(js.Dynamic.global.Dragster)(wat))
        },100)
        wat
      })
    )
  }

  dom.document.addEventListener("dragster:enter",(event:Event) => {
    event.target.asInstanceOf[HTMLElement].classList.add("over")
  },false)

  dom.document.addEventListener("dragster:leave",(event:Event) => {
    event.target.asInstanceOf[HTMLElement].classList.remove("over")
  },false)

  val unassignedTag: Rx[HtmlTag] = Rx {
    div(cls:="unassigned")(
      div(cls:="row")(
        h3("Temporary Buttons"),
        p(a(
          href:="#",
          "Add to squad",
          onclick := { () => 
            AlgorithmJS.send(CreateSquad(Squad.FakeLeader2))
            false
          }
        )),
        p(a(
          href:="#",
          "Remove squad",
          onclick := { () =>
            squads() = squads().tail
          }
        )),
        p(a(href:="#","Unassign", onclick := { () =>
          AlgorithmJS.send(UnassignSelf)
          false
        })),
        p(a(href:="#","TestIt", onclick := { () =>
          AlgorithmJS.send(TestIt)
          false
        }))
      ),
      div(cls:="row")(
        ul(cls:="todo")(unassigned().map { character =>
          li(character.name)
        })
      )
    )
  }

  def onDragStartMember(member: Character): js.ThisFunction1[HTMLElement,DragEvent,Boolean] = { (jsThis:HTMLElement,event:DragEvent) =>
    event.dataTransfer.setData("cid",member.cid.txt)
    jsThis.style.opacity = "0.4"
    true
  }

  def onDragEndMember(member: Character): js.ThisFunction1[HTMLElement,DragEvent,Boolean] = { (jsThis:HTMLElement,event:DragEvent) =>
    jsThis.style.opacity = "1.0"
    false
  }

  def selectedDetails(squad: Squad): HtmlTag = {
    div(
      h4(s"Pattern ${squad.pattern.name}"),
      div(cls:="detail-assignments")(
        ul(cls:="list-group",squad.pattern.assignments.zipWithIndex.map { case (assignment,idx) =>
          val member = squad.roles.find(_.idx == idx).map(_.character)
          li(
            cls:=s"list-group-item view-assignmentm clearfix ${if(member.isDefined) "role-assigned" else "role-unassigned"}",
            if(member.isDefined) { Seq(
              "draggable".attr := "true",
              "ondragstart".attr := onDragStartMember(member.get),
              "ondragend".attr := onDragEndMember(member.get)
            )} else { Seq(
              "draggable".attr := "false"
            )}
          )(
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
    selected().flatMap(squads().get).map { squad => 
      div(cls:="selected-squad")(
        h3(s"${squad.leader.name}'s Squad"),
        selectedDetails(squad)
      )
    } getOrElse {
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

      .role-assigned {
        border: 2px solid;
        background: #eee;
      }

      .
      .role-unassigned {
        border: 2px dashed;
      }

      .over {
        border: 3px dashed #d58512;
      }

      li[draggable="true"] {
        cursor: move;
      }
    """)

  val screen: HtmlTag = {
    div(
      styles,
      Nav.header,
      Rx { jumbo },
      div(cls:="col-xs-4")(Rx { available }),
      div(cls:="col-xs-5")(Rx { selectedTag }),
      div(cls:="col-xs-3")(Rx { unassignedTag })
    )
  }
}
