package algorithm.partials

import scalatags.JsDom.all._
import shared.models._
import rx._

trait LocalLink
case object SquadLink extends LocalLink
case object PreferenceLink extends LocalLink
case object CreatePatternLink extends LocalLink
case object LoginLink extends LocalLink
case object VoiceTestLink extends LocalLink

object Nav {

  val currentLink: Var[Option[LocalLink]] = Var(None)

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
            li(linkTo(SquadLink,"Main"))
          )
        )
      )
    )
  }

  def goto(link: LocalLink) = {
    currentLink() = Some(link)
  }

}
