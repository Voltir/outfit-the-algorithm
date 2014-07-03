package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Akka
import actors._
import akka.actor.Props
import akka.pattern._
import scala.concurrent.duration._
import akka.util.Timeout
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json

object Application extends Controller {

  val algo = Akka.system.actorOf(Props[TheAlgorithm], name="the-algorithm")
  implicit val timeout = Timeout(5 seconds)

  def app = Action {
    Ok(views.html.app())
  }

  def lookupCharacter(partial: String) = Action.async {
    (algo ? LookupCharacterRequest(partial.toLowerCase)).mapTo[LookupCharacterResult].map { r =>
      val mapped = r.refs.map { ref => Json.obj("cid"->ref.character_id,"name"->ref.name.first)}
      Ok(Json.toJson(mapped))
    }
  }

}
