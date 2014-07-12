package algorithm.screens

import scalatags.JsDom.all._
import scalatags.JsDom.tags2
import shared.models._
import algorithm.partials._
import shared.models.Pattern._
import algorithm.framework.Framework._
import rx._
import scala.collection.mutable.ArrayBuffer
import org.scalajs.dom.{HTMLSelectElement, HTMLInputElement, KeyboardEvent}
import scala.scalajs.js
import org.scalajs.dom.extensions.KeyCode
import algorithm.{PatternJS, AlgorithmJS}
import org.scalajs.dom

object CreatePattern {

  val assignments: Var[ArrayBuffer[Assignment]] = Var(ArrayBuffer.empty)
  val patternName: Var[String] = Var("")

  def addRoleButton(role: Role): HtmlTag = {
    dom.console.log("Add role button..")
    button(
      `type`:="button",
      cls:=Rx { s"btn btn-success ${if(assignments().size >= 12)"disabled"}" },
      onclick := { () =>
        assignments() = assignments() :+ Assignment(role,Pattern.NoTeam,Pattern.Member)
      }
    )(asString(role))
  }

  val construction: HtmlTag = {
    dom.console.log("construction..")
    div(
      h3("Add Infantry Roles"),
      div(cls:="btn-group-vertical", width:=12.em)(
        addRoleButton(HeavyAssault),
        addRoleButton(LightAssault),
        addRoleButton(Medic),
        addRoleButton(Engineer),
        addRoleButton(MAX),
        addRoleButton(Infiltraitor)
      ),
      h3("Add Armor Roles"),
      div(cls:="btn-group-vertical", width:=12.em)(
        addRoleButton(MBTDriver),
        addRoleButton(MBTGunner),
        addRoleButton(Lightning),
        addRoleButton(HarasserDriver),
        addRoleButton(HarasserGunner),
        addRoleButton(SundererDriver),
        addRoleButton(SundererGunner)
      ),
      h3("Add Air Roles"),
      div(cls:="btn-group-vertical", width:=12.em)(
        addRoleButton(Scythe),
        addRoleButton(LiberatorPilot),
        addRoleButton(LiberatorGunner),
        addRoleButton(GalaxyPilot),
        addRoleButton(GalaxyGunner)
      )
    )
  }

  def setFireteamCallback(team: Fireteam, idx: Int) = { () =>
    if(assignments().isDefinedAt(idx)) {
      assignments().update(idx,assignments()(idx).copy(team=team))
      assignments.propagate()
    }
  }

  def updateTeamleadCallback(assignment: Assignment, idx: Int) = { () =>
    if(assignments().isDefinedAt(idx)) {
      val prev = assignments()(idx)
      val updated =
        if(prev.`type` == TeamLead) prev.copy(`type`=Member)
        else prev.copy(`type`=TeamLead)
      assignments().update(idx,updated)
      assignments.propagate()
    }
  }

  def renderEditAssignment(assignment: Assignment, idx: Int): HtmlTag = {
    dom.console.log("Render Edit...")
    val fireteam = assignment.team match {
      case NoTeam => "No Fireteam Assigned"
      case FireteamOne => "Fireteam One"
      case FireteamTwo => "Fireteam Two"
      case FireteamThree => "Fireteam Three"
    }

    val leader =
      if(assignment.`type` == TeamLead) "Team Leader"
      else "Member"

    div(cls:="edit-role clearfix")(
      div(cls:="row")(
        img(src:=iconUrl(assignment.role),float:="left",margin:=5.px),
        div(cls:="infoblock",float:="left")(
          div(p(s"${asString(assignment.role)} -- $leader")),
          div(p(fireteam)),
          div(p(assignment.details))
        ),
        div(
          float:="right",
          margin:="0 10px 0 0",
          cursor:="pointer",
          raw("&times"),
          onclick := { () =>
            println(s"Remove! ${idx}")
            assignments().remove(idx)
            assignments.propagate()
          }
        )
      ),
      div(cls:="row")(
        div(cls:="col-xs-6")(
          label(`for`:=s"set-fireteam-$idx", "Set Fireteam: ")
        ),
        div(cls:="col-xs-6")(
          div(id:=s"set-fireteam-$idx", cls:="btn-group")(
            button(`type`:="button", cls:="btn btn-primary btn-xs", onclick := setFireteamCallback(Pattern.NoTeam,idx))("None"),
            button(`type`:="button", cls:="btn btn-primary btn-xs", onclick := setFireteamCallback(Pattern.FireteamOne,idx))("One"),
            button(`type`:="button", cls:="btn btn-primary btn-xs", onclick := setFireteamCallback(Pattern.FireteamTwo,idx))("Two"),
            button(`type`:="button", cls:="btn btn-primary btn-xs", onclick := setFireteamCallback(Pattern.FireteamThree,idx))("Three")
          )
        )
      ),
      div(cls:="row")(
        div(cls:="col-xs-6")(
          label(`for`:=s"details-$idx", "Additional Details: ")
        ),
        div(cls:="col-xs-6")(
          input(
            id:=s"details-$idx",
            `type`:="text",
            value:=assignment.details,
            "onkeyup".attr := { { (jsThis: HTMLInputElement, event: KeyboardEvent) =>
              assignments().update(idx,assignments()(idx).copy(details=jsThis.value))
              if(event.keyCode == KeyCode.enter) { assignments.propagate() }
              true
            }:js.ThisFunction1[HTMLInputElement,KeyboardEvent,Boolean]},
            onblur := { () => assignments.propagate() }
          )
        )
      ),
      div(cls:="row")(
        div(cls:="col-xs-6")(
          label(`for`:=s"toggle-leader-$idx","Mark as Team Lead: ")
        ),
        div(cls:="col-xs-6")(
          input(
            id:=s"toggle-leader-$idx",
            `type`:="checkbox",
            onchange := updateTeamleadCallback(assignment,idx),
            if(assignment.`type` == TeamLead) checked := "checked"
          )
        )
      )
    )
  }

  val currentPattern: Rx[HtmlTag] = Rx {
    dom.console.log("current...")
    div(cls:="current-pattern")(
      if(assignments().size > 0) {
        println("CURRENT -- 2")
        div(
          h3(s"Roles remaining: ${12 - assignments().size}"),
          ul(cls:="list-group",assignments().zipWithIndex.map { case (assignment,idx) =>
            println("CURRENT -- 3")
            val classes = "list-group-item " + (assignment.team match {
              case FireteamOne => "fireteam-one"
              case FireteamTwo => "fireteam-two"
              case FireteamThree => "fireteam-three"
              case _ => ""
            })
            li(cls:=classes,renderEditAssignment(assignment, idx))
          })
        )
      } else {
        h3("Select Roles:")
      }
    )
  }

  def canSave() = {
    assignments().size == 12 && patternName() != "" && !DefaultPatterns.names.contains(patternName())
  }

  def loadPattern: js.ThisFunction0[HTMLSelectElement,Boolean] = { (select:HTMLSelectElement) =>
    AlgorithmJS.patterns().find(_.name == select.value).map { pattern =>
      patternName() = pattern.name
      assignments().clear()
      assignments().appendAll(pattern.assignments)
      assignments.propagate()
    } getOrElse {
      patternName() = ""
      assignments().clear()
      assignments.propagate()
    }
    true
  }

  def savePattern = { () =>
    if(canSave()) {
      val newPattern = Pattern(patternName(),true,assignments().toArray,Option(10))
      AlgorithmJS.patterns() = AlgorithmJS.patterns().filter(_.name != patternName()) :+ newPattern
      PatternJS.storeLocal(AlgorithmJS.patterns())
      patternName() = ""
      assignments().clear
      assignments.propagate()
    }
  }

  val info: Rx[HtmlTag] = Rx {
    div(cls:="info-block")(
      h3("Info"),
      label(`for`:="load-pattern","Load Pattern:"),
      select(
        id:="load-pattern",
        cls:="form-control",
        onchange := loadPattern
      )(
        option("Select Pattern to Load", value:=""),
        option("Clear Selected", value:="__omg_clear__"),
        AlgorithmJS.patterns().toList.sortBy(_.name).map { pattern =>
          option(s"Pattern ${pattern.name} ${if(!pattern.custom) "(Default)" else ""}", value := pattern.name)
        }
      ),
      h3("About"),
      p("""
        This is a tool for creating custom Squad 'Patterns'. A Pattern is a set of 12 Roles, ordered by importance.
        Roles are filled from top to bottom, based on individual player preference.
        """
      ),
      p("""
        For example, if you have 7 squad members, the algorithim will fill the first 7 slots of the pattern, while maximizing member preference.
        """
      ),
      p("""
        Custom Patterns are currently only saved locally to your current browsers, but members in your squad will still receive the correct assignments.
        """
      ),
      h3(s"Save ${if(patternName() != "") s"-- ${patternName()}" else ""}"),
      label(`for`:="save-custom","Pattern Name / Voice Command:"),
      input(
        id:="save-custom",
        `type`:="text",
        value := patternName(),
        "onkeyup".attr := { { (jsThis: HTMLInputElement, event: KeyboardEvent) =>
          patternName.updateSilent(jsThis.value)
          if(event.keyCode == KeyCode.enter) { patternName.propagate() }
          true
        }:js.ThisFunction1[HTMLInputElement,KeyboardEvent,Boolean]},
        onblur := { () => patternName.propagate() }
      ),
      p(button(
        `type`:="button",
        cls:= Rx { s"btn btn-primary btn-xs ${if(!canSave()) "disabled"}"},
        onclick := savePattern
      )("Save Custom Pattern")),
      p(
        if(DefaultPatterns.names.contains(patternName())) "Cannot overwrite default Pattern, pick another name."
        else ""
      )
    )
  }
  val styles: HtmlTag = tags2.style("media".attr:="screen", `type`:="text/css")(
    """
      .fireteam-one { border: 5px solid red; }
      .fireteam-two { border: 5px solid blue; }
      .fireteam-three { border: 5px solid green; }
    """)

  def screen: HtmlTag = {
    dom.console.log("screen...")
    div(cls:="create-pattern")(
      styles,
      Nav.header,
      div(cls:="col-xs-3")(construction),
      div(cls:="col-xs-6")(currentPattern),
      div(cls:="col-xs-3")(info)
    )
  }
}
