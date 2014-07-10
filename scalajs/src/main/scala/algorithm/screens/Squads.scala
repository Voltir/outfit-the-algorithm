package algorithm.screens

import scalatags.JsDom.all._
import scalatags.JsDom.tags2
import shared.models._
import shared.commands._
import algorithm.partials._
import rx._
import scala.collection.mutable.ArrayBuffer
import algorithm.framework.Framework._
import algorithm.AlgorithmJS
import scala.collection.mutable.{Map => MutableMap}
import scala.scalajs.js
import org.scalajs.dom.{
  onclick => _,
  onchange => _,
  name => _,
  _
}
import org.scalajs.dom
import shared.commands.CreateSquad
import shared.commands.JoinSquad
import shared.models.Character
import shared.models.AssignedRole
import shared.models.CharacterId
import shared.models.Squad.PatternTypePreference

case class MyAssignment(
  lid: CharacterId,
  assignment: Pattern.Assignment
)

case class CreateSquadContext(
  pattern: Pattern,
  pref: PatternTypePreference
)

object Squads {

  val squads: Var[MutableMap[CharacterId,Squad]] =  Var(MutableMap.empty)

  val unassigned: Var[List[Character]] = Var(List.empty)

  val selected: Var[Option[CharacterId]] = Var(None)

  val current: Var[Option[MyAssignment]] = Var(None)

  val createSquadContext: Var[CreateSquadContext] = Var(CreateSquadContext(DefaultPatterns.basic,Squad.InfantryPreference))

  def checkForAssignment(squad: Squad) = {
    squad.roles.find { r => 
      Option(r.character.cid) == AlgorithmJS.user().map(_.cid)
    }.map { role =>
      val assignment = squad.pattern.assignments(role.idx)
      current() = Option(MyAssignment(squad.leader.cid,assignment))
      if(selected() == None) {
        selected() = Option(squad.leader.cid)
      }
    }
  }

  val unassignedCheck = Obs(unassigned) {
    unassigned().foreach { player =>
      if(Option(player.cid) == AlgorithmJS.user().map(_.cid)) {
        current() = None
      }
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
        img(
          src:=Squad.patternTypeIcon(squad.preference),
          float:="right",
          width:=48.px,
          height:=48.px,
          marginRight:=4.px
        ),
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
        val wat = li(
          cls:=s"list-group-item squad-summary",
          "ondragover".attr := { (event:DragEvent) =>
            event.preventDefault()
            false
          },
          "ondrop".attr := { {(jsThis:HTMLElement,event: DragEvent) =>
            val txt = event.dataTransfer.getData("cid")
            val cid = CharacterId(txt)
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

  def setInitialPattern: js.ThisFunction0[HTMLSelectElement,Boolean] = { (select: HTMLSelectElement) =>
    AlgorithmJS.patterns().find(_.name == select.value).foreach { p =>
      createSquadContext() = createSquadContext().copy(pattern=p)
    }
    true
  }

  val unassignedTag: Rx[HtmlTag] = Rx {
    div(cls:="unassigned")(
      if(current().map(_.lid) != AlgorithmJS.user().map(_.cid)) {
        div(cls:="row")(
          h3("Create Squad"),
          label(`for`:="create-pattern","Initial Pattern"),
            select(
            id:="create-pattern",
            cls:="form-control",
            onchange := setInitialPattern
          )(
            option("Select Squad Pattern", value:=""),
            AlgorithmJS.patterns().toList.sortBy(_.name).map { pattern =>
              option(
                s"Pattern ${pattern.name} ${if(!pattern.custom) "(Default)" else ""}",
                value := pattern.name,
                if(pattern.name == createSquadContext().pattern.name) { "selected".attr:="true" } else {}
              )
            }
          ),
          label(`for`:="squad-pref-kind","Pattern Type Preference",marginTop:=10.px),
          div(id:="squad-pref-kind",cls:="btn-group")(
            label(cls:="btn btn-default")(
              input(`type`:="radio", name:="squad-kind", value:="inf",
                if(createSquadContext().pref == Squad.InfantryPreference) "checked".attr:="true" else {},
                onchange := { () =>
                  createSquadContext() = createSquadContext().copy(pref=Squad.InfantryPreference)
                }
              ),
              "Infantry"
            ),
            label(cls:="btn btn-default")(
              input(`type`:="radio", name:="squad-kind", value:="arm",
                if(createSquadContext().pref == Squad.ArmorPreference) "checked".attr:="true" else {},
                onchange := { () =>
                  createSquadContext() = createSquadContext().copy(pref=Squad.ArmorPreference)
                }
              ),
              "Armor"
            ),
            label(cls:="btn btn-default")(
              input(`type`:="radio", name:="squad-kind", value:="air",
                if(createSquadContext().pref == Squad.AirPreference) "checked".attr:="true" else {},
                onchange := { () =>
                  createSquadContext() = createSquadContext().copy(pref=Squad.AirPreference)
                }
              ),
              "Air"
            )
          ),
          button(
            "Create Squad",
            cls := "btn btn-warning btn-xs",
            marginTop := 10.px,
            onclick := {
              () =>
                AlgorithmJS.user().foreach { user =>
                  AlgorithmJS.send(CreateSquad(user, createSquadContext().pattern, createSquadContext().pref))
                }
                false
            }
          )
        )
      } else {
        div(cls:="row")(
          h3("Squad Actions"),
          button(
            "Disband Squad",
            cls := "btn btn-danger btn-xs",
            marginTop := 10.px,
            onclick := { () =>
              AlgorithmJS.send(DisbandSquad)
              false
            }
          )
        )
      },
      div(cls:="row")(
        h3("Temporary Buttons"),
        p(a(
          href:="#",
          "Add to squad",
          onclick := { () => 
            AlgorithmJS.send(CreateSquad(Squad.FakeLeader2,DefaultPatterns.standard, Squad.InfantryPreference))
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

      .squad-summary {
        border: 2px solid;
        border-color: rgb(0,0,0);
        background: #555;
        color: white;
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
