package algorithm.screens

import scalatags.JsDom.all._
import scalatags.JsDom.tags2
import shared.models._
import algorithm.partials._
import shared.models.Pattern._
import algorithm.framework.Framework._
import rx._
import scala.collection.mutable.{Map => MutableMap}
import org.scalajs.dom.{HTMLSelectElement, HTMLInputElement, KeyboardEvent}
import algorithm.{PreferenceJS, PatternJS, AlgorithmJS}
import org.scalajs.dom
import shared.commands.SetPreference

object EditPreferences {

  val preferences: Var[MutableMap[Pattern.Role,Int]] = Var(MutableMap.empty)

  def allocatedInfantry: Int = {
    preferences().filter { _ match {
      case (i:InfantryRole,_) => true
      case _ => false
    }}.map(_._2).sum
  }

  def allocatedArmor: Int = {
    preferences().filter { _ match {
      case (i:ArmorRole,_) => true
      case _ => false
    }}.map(_._2).sum
  }

  def allocatedAir: Int = {
    preferences().filter { _ match {
      case (i:AirRole,_) => true
      case _ => false
    }}.map(_._2).sum
  }

  def disabledAdd(role: Role): Boolean = {
    role match {
      case i: InfantryRole => allocatedInfantry == 10
      case i: ArmorRole => allocatedArmor == 10
      case i: AirRole => allocatedAir == 10
      case _ => true
    }
  }

  def disabledSub(role: Role): Boolean = {
    preferences().getOrElse(role,0) == 0
  }

  def PrefTag(role: Role): HtmlTag = {
    div(cls:="pref row clearfix")(
      span(float:="left",marginTop:=3.px)(b(s"${asString(role)}: (${preferences().getOrElse(role,0)})")),
      button(
        `type`:="button",
        float:="right",
        marginTop:=3.px,
        cls := s"btn btn-success btn-sm ${if(disabledAdd(role)) "disabled" else ""}",
        onclick := { () =>
          val updated = preferences().getOrElse(role,0) + 1
          preferences().put(role,updated)
          preferences.propagate()
        }
      )("+"),
      button(
        `type`:="button",
        float:="right",
        marginRight:=5.px,
        marginTop:=3.px,
        cls := s"btn btn-warning btn-sm ${if(disabledSub(role)) "disabled" else ""}",
        onclick := { () =>
          val updated = preferences().getOrElse(role,0) - 1
          preferences().put(role,updated)
          preferences.propagate()
        }
      )("-")
    )
  }

  val infantry: Rx[HtmlTag] = Rx {
    div(
      h3(s"Infantry: (Max: ${10 - allocatedInfantry})"),
      div(width:=14.em)(
        PrefTag(HeavyAssault),
        PrefTag(LightAssault),
        PrefTag(Medic),
        PrefTag(Engineer),
        PrefTag(MAX),
        PrefTag(Infiltraitor)
      )
    )
  }

  val armor: Rx[HtmlTag] = Rx {
    div(
      h3(s"Armor: (Max: ${10 - allocatedArmor})"),
      div(width:=14.em)(
        PrefTag(MBTDriver),
        PrefTag(MBTGunner),
        PrefTag(Lightning),
        PrefTag(HarasserDriver),
        PrefTag(HarasserGunner),
        PrefTag(SundererDriver),
        PrefTag(SundererGunner)
      )
    )
  }

  val air: Rx[HtmlTag] = Rx {
    div(
      h3(s"Air: (Max: ${10 - allocatedAir})"),
      div(width:=14.em)(
        PrefTag(Scythe),
        PrefTag(LiberatorPilot),
        PrefTag(LiberatorGunner),
        PrefTag(GalaxyPilot),
        PrefTag(GalaxyGunner)
      )
    )
  }

  val info: Rx[HtmlTag] = Rx {
    div(cls:="edit-info")(
      h3("Save Preferences"),
      p(button(
        `type`:="button",
        cls := "btn btn-primary",
        onclick := { () =>
          val pref = PreferenceDefinition(preferences().toList.map(i => Pref(i._1,i._2)))
          PreferenceJS.storeLocal(pref)
          AlgorithmJS.send(SetPreference(pref))
          Nav.goto(SquadLink)
        }
      )("Save")),
      p(button(
        `type`:="button",
        cls := "btn btn-warning",
        onclick := { () => preferences().clear() ; preferences.propagate() }
      )("Reset Preferences"))
    )
  }

  val screen: HtmlTag = {

    PreferenceJS.loadLocal().map { pref =>
      pref.values.foreach { p =>
        preferences().put(p.role, p.score)
      }
      dom.setTimeout({() => preferences.propagate()}, 10)
    }

    div(cls:="edit-preference")(
      Nav.header,
      div(cls:="col-xs-3")(Rx { infantry }),
      div(cls:="col-xs-3")(Rx { armor }),
      div(cls:="col-xs-3")(Rx { air }),
      div(cls:="col-xs-3")(Rx { info })
    )
  }
}
