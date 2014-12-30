package algorithm.screens

import scalatags.JsDom.all._
import scalatags.JsDom.tags2
import shared.models._
import shared.models.Squad.PatternTypePreference
import shared.commands._
import algorithm.partials._
import rx._
import scala.collection.mutable.ArrayBuffer
import algorithm.framework.Framework._
import algorithm.{CharacterJS, AlgorithmJS}
import scala.collection.mutable.{Map => MutableMap}
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import org.scalajs.dom.{
  onclick => _,
  onchange => _,
  name => _,
  _
}
import org.scalajs.dom
import shared.models.Pattern._


case class MyAssignment(
  leader: Character,
  assignment: Pattern.Assignment,
  patternName: String
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

  def roleSound(role: Role, phrase: js.Array[js.Any]): Unit = role match {
    case HeavyAssault => phrase.push(g.sounds.phrases.ha)
    case Medic => phrase.push(g.sounds.phrases.medic)
    case Engineer => phrase.push(g.sounds.phrases.engy)
    case LightAssault => phrase.push(g.sounds.phrases.la)
    case Infiltraitor => phrase.push(g.sounds.phrases.inf)
    case MAX => phrase.push(g.sounds.phrases.max)
    case MBTDriver => phrase.push(g.sounds.phrases.magrider)
    case MBTGunner => { phrase.push(g.sounds.phrases.magrider) ; phrase.push(g.sounds.phrases.gunner) }
    case Lightning => phrase.push(g.sounds.phrases.lightning)
    case HarasserDriver => phrase.push(g.sounds.phrases.harasser)
    case HarasserGunner => { phrase.push(g.sounds.phrases.harasser) ; phrase.push(g.sounds.phrases.gunner) }
    case SundererDriver => phrase.push(g.sounds.phrases.sundy)
    case SundererGunner => { phrase.push(g.sounds.phrases.sundy) ; phrase.push(g.sounds.phrases.gunner) }
    case Scythe => phrase.push(g.sounds.phrases.scythe)
    case LiberatorPilot => phrase.push(g.sounds.phrases.lib)
    case LiberatorGunner => { phrase.push(g.sounds.phrases.lib) ; phrase.push(g.sounds.phrases.gunner) }
    case GalaxyPilot => phrase.push(g.sounds.phrases.galaxy)
    case GalaxyGunner => { phrase.push(g.sounds.phrases.galaxy) ; phrase.push(g.sounds.phrases.gunner) }
    case _ => phrase.push(g.sounds.phrases.elephant)
  }

  def teamSound(fireteam: Fireteam, phrase: js.Array[js.Any]): Unit = fireteam match {
    case FireteamOne => phrase.push(g.sounds.phrases.team1)
    case FireteamTwo => phrase.push(g.sounds.phrases.team2)
    case FireteamThree => phrase.push(g.sounds.phrases.team3)
    case NoTeam => Unit
  }

  def sayAssignment = {
    val toSay = js.Array[js.Any]()
    current().map { assignment =>
      toSay.push(g.sounds.phrases.new_role)
      roleSound(assignment.assignment.role, toSay)
      teamSound(assignment.assignment.team, toSay)
    }
    g.sounds.say(toSay)
  }

  def checkForAssignment(squad: Squad) = {
    squad.roles.find(r => Option(r.character.cid) == AlgorithmJS.user().map(_.cid)).map { role =>
      val assignment = squad.pattern.assignments(role.idx)
      val toSay = js.Array[js.Any]()

      current() match {
        case None => {
          toSay.push(g.sounds.phrases.new_leader,g.sounds.phrases.new_role)
          roleSound(assignment.role,toSay)
          teamSound(assignment.team,toSay)
        }

        case Some(MyAssignment(leader,prev,_)) => {
          if(leader.cid != squad.leader.cid) toSay.push(g.sounds.phrases.new_leader)
          if(prev.role != assignment.role || prev.team != assignment.team) {
            toSay.push(g.sounds.phrases.new_role)
            roleSound(assignment.role, toSay)
            teamSound(assignment.team,toSay)
          }
        }
      }
      g.sounds.say(toSay)

      current() = Option(MyAssignment(squad.leader,assignment,squad.pattern.name))

      AlgorithmJS.isSquadLeader() = Option(squad.leader.cid) == AlgorithmJS.user().map(_.cid)

      if(selected() == None) {
        selected() = Option(squad.leader.cid)
      }
    }
  }

  val unassignedCheck = Obs(unassigned) {
    unassigned().foreach { player =>
      if(Option(player.cid) == AlgorithmJS.user().map(_.cid)) {
        AlgorithmJS.isSquadLeader() = false
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
    div(`class`:="jumbotron row")(current().map { assignment =>
      div(cls:="col-md-5")(
        h3("Your Role: ")(b(s"${Pattern.asString(assignment.assignment.role)}")),
        h3("Your Leader: ")(b(s"${assignment.leader.name}")),
        assignment.assignment.team match {
          case FireteamOne => h3("Your Fireteam: ")(b("Fireteam One"))
          case FireteamTwo => h3("Your Fireteam: ")(b("Fireteam Two"))
          case FireteamThree => h3("Your Fireteam: ")(b("Fireteam Three"))
          case _ => span()
        }
      )
      } getOrElse {
        div(cls:="col-md-5")(
          h3("You are not in a squad.")
        )
      },
      VoiceTest.voiceInfo
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
        if(Option(squad.leader.cid) != current().map(_.leader.cid)) {
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
          div(h5(i(s"Pattern ${squad.pattern.name}")))
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
            AlgorithmJS.send(MoveToSquad(s.leader.cid,cid))
            jsThis.classList.remove("over")
            true
          }: js.ThisFunction1[HTMLElement,DragEvent,Boolean] }
        )(squadSummary(s)).render
        dom.setTimeout(() => {
          js.Dynamic.newInstance(js.Dynamic.global.Dragster)(wat)
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

  def changePattern: js.ThisFunction0[HTMLSelectElement,Boolean] = { (select:HTMLSelectElement) =>
    AlgorithmJS.patterns().find(_.name == select.value).foreach { pattern =>
      AlgorithmJS.send(SetPattern(pattern))
    }
    true
  }

  val unassignedTag: Rx[HtmlTag] = Rx {
    div(cls:="unassigned")(
      if(current().map(_.leader.cid) != AlgorithmJS.user().map(_.cid)) {
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
          h3("Squad Commands"),
          select(
            id:="load-pattern",
            cls:="form-control",
            onchange := changePattern
          )(
            option("Change Squad Pattern", value:=""),
            AlgorithmJS.patterns().toList.sortBy(_.name).map { pattern =>
              option(
                s"Pattern ${pattern.name} ${if(!pattern.custom) "(Default)" else ""}",
                value := pattern.name,
                if(current().exists(_.patternName == pattern.name)) { "selected".attr := "selected"} else { }
              )
            }
          )
        )
      },
      div(cls:="row")(
        h3("Actions"),
        if(current().map(_.leader.cid) == AlgorithmJS.user().map(_.cid)) {
          button(
            "Disband Squad",
            cls := "btn btn-danger btn-xs",
            marginTop := 10.px,
            onclick := { () =>
              AlgorithmJS.send(DisbandSquad)
              false
            }
          )
        } else {}
      ),
      div(cls:="row")(
        button(
          "Log Out",
          cls := "btn btn-danger btn-xs",
          marginTop := 10.px,
          onclick := { () =>
            AlgorithmJS.send(Logout)
            false
          }
        )
      ),
      div(cls:="row")(
        button(
          "Unassign",
          cls := "btn btn-warning btn-xs",
          marginTop := 10.px,
          onclick := { () =>
            AlgorithmJS.send(UnassignSelf)
            false
          }
        )
      ),
      div(cls:="row")(
        button(
          "Unpin",
          cls := "btn btn-warning btn-xs",
          marginTop := 10.px,
          onclick := { () =>
            current().foreach { assignment =>
                AlgorithmJS.send(UnpinAssignment(assignment.leader.cid,assignment.patternName))
            }
            false
          }
        )
      ),
      div(cls:="row")(
        h3("Unassigned"),
        ul(cls:="list-group", marginTop:=10.px)(unassigned().map { character =>
          li(cls:="list-group-item", div(cls:="clearfix")(
            h4(float:="left")(character.name),
            if(current().isDefined) {
              button(
                "Take",
                cls := "btn btn-success btn-xs",
                float:="right",
                onclick := { () =>
                  AlgorithmJS.send(MoveToSquad(current().get.leader.cid,character.cid))
                }
              )
            } else { span() }
          ))
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
            if(member.isDefined && AlgorithmJS.isSquadLeader()) { Seq(
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
                  h4("Unassigned")(
                    button(
                      cls:="btn-warning btn-xs",
                      margin:=5.px,
                      onclick := { () => AlgorithmJS.send(PinAssignment(squad.leader.cid,squad.pattern.name,idx))}
                    )("Pin")
                  )
                }),
                div(h5(s"${Pattern.asString(assignment.role)}")),
                if(assignment.team != NoTeam) {
                  val txt = assignment.team match {
                    case FireteamOne => "Fireteam One"
                    case FireteamTwo => "Fireteam Two"
                    case FireteamThree => "Fireteam Three"
                    case NoTeam => ""
                  }
                  div(p(txt))
                } else {
                  div()
                },
                div(p(assignment.details))
              ),
              if(AlgorithmJS.isSquadLeader() && member.isDefined) {
                div(float := "right")(
                  button(
                    cls := "btn-warning btn-xs",
                    margin := 5.px,
                    onclick := {
                      () => AlgorithmJS.send(Unassign(member.get.cid))
                    }
                  )("Unassign")
                )
              } else {
                div()
              }
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

  lazy val screen: HtmlTag = {
    g.sounds.say(g.sounds.phrases.welcome)
    div(
      styles,
      Nav.header,
      Rx { jumbo },
      div(cls:="col-md-4")(Rx { available }),
      div(cls:="col-md-5")(Rx { selectedTag }),
      div(cls:="col-md-3")(Rx { unassignedTag })
    )
  }
}
