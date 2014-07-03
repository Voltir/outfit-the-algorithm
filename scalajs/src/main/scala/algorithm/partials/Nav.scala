package algorithm.partials

import scalatags.JsDom.all._
import shared.models._
import rx._

trait LocalLink
case class SquadLink(cid: CharacterId) extends LocalLink
case object PreferenceLink extends LocalLink
case object CreatePatternLink extends LocalLink

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
          a(`class`:="navbar-brand", href:="#")("The Algorithm")
        ),
        div(`class`:="navbar-collapse collapse")(
          ul(`class`:="nav navbar-nav")(
            li(linkTo(PreferenceLink,"Preferences")),
            li(linkTo(CreatePatternLink,"Patterns")),
            li(linkTo(SquadLink(CharacterId("wat")),"Main"))
          )
        )
      )
    )
  }

  def goto(link: LocalLink) = {
    currentLink() = Some(link)
  }

}