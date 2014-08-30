package algorithm.partials

import algorithm.AlgorithmJS
import org.scalajs.dom
import org.scalajs.dom.HTMLSelectElement

import scala.scalajs.js
import scalatags.JsDom.all._
import shared.models._
import rx._
import algorithm.framework.Framework._

trait LocalLink
case object SquadLink extends LocalLink
case object PreferenceLink extends LocalLink
case object CreatePatternLink extends LocalLink
case object LoginLink extends LocalLink
case object VoiceTestLink extends LocalLink

object Nav {

  val currentLink: Var[Option[LocalLink]] = Var(None)

  val selectedLocale: Var[String] = Var("en-US")

  val englishLocale = List(
    ("en-AU", "Australia"),
    ("en-CA", "Canada"),
    ("en-IN", "India"),
    ("en-NZ", "New Zealand"),
    ("en-ZA", "South Africa"),
    ("en-GB", "United Kingdom"),
    ("en-US", "United States")
  )

  def localeChanged: js.ThisFunction0[HTMLSelectElement,Boolean] = { (select: HTMLSelectElement) =>
    selectedLocale() = select.value
    if(AlgorithmJS.annyang.isInstanceOf[js.Object]) {
      AlgorithmJS.annyang.setLanguage(select.value)
    }
    true
  }

  val selectLangLocale: Rx[HtmlTag] = Rx {
    select(
      onchange := localeChanged,
      englishLocale.map { case (ident,description) =>
        val isSelected = selectedLocale() == ident
        option(
          description,
          value:=ident,
          if(isSelected) "selected".attr:="selected" else ()
        )
      }
    )
  }

  def linkTo(link: LocalLink, text: String): HtmlTag = {
    a(
      href:="#",
      onclick := { () => Nav.goto(link)}
    )(text)
  }

  val header: HtmlTag = {
    div(`class`:="navbar navbar-default","role".attr:="navigation")(
      div(`class`:="container-fluid")(
        div(`class`:="navbar-header")(
          a(`class`:="navbar-brand", href:="#", onclick := { () => Nav.goto(SquadLink) } )("The Algorithm")
        ),
        div(`class`:="navbar-collapse collapse")(
          ul(`class`:="nav navbar-nav")(
            li(linkTo(PreferenceLink,"Preferences")),
            li(linkTo(CreatePatternLink,"Create Pattern")),
            li(linkTo(VoiceTestLink,"Voice Command Test")),
            li(linkTo(SquadLink,"Main")),
            li(a(href:="#")(selectLangLocale))
          )
        )
      )
    )
  }

  def goto(link: LocalLink) = {
    currentLink() = Some(link)
  }

}
