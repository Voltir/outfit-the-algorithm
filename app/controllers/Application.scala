package controllers

import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

import akka.channels._
import play.api.libs.concurrent.Akka
import actors._
import scala.concurrent.duration.Duration
import play.api.libs.json._
import play.api.libs.iteratee._

import models.Format._

case class RegistrationData(
  character_name: String,
  pass1: String,
  pass2: String
)


//https://census.soe.com/get/ps2:v2/character_name/?name.first_lower=^voltaire&c:limit=10&c:show=name.first,character_id
//https://census.soe.com/get/ps2:v2/character/5428010618015225217/?c:resolve=faction,world

object Application extends Controller {
  import play.api.Play.current
  implicit val timeout = akka.util.Timeout(Duration(5,"seconds"))

  val algo = ChannelExt(Akka.system).actorOf(new TheAlgorithm(),"the-algorithm")


  def index = Action {
    Ok(views.html.index())
  }

  def tmponline = Action.async {
    (algo <-?- GetOnlineMembers).map { case OnlineMembers(cids) =>
      Ok(Json.toJson(cids))
    }
  }

  def lookupCharacters(partial: String) = Action.async {
    (algo <-?- LookupCharacterList(partial.toLowerCase)).map { case LookupCharacterListResponse(refs) =>
      Ok(Json.toJson(List(refs)))
    }
  }

  def thealgorithmJS = Action { implicit request =>
    Ok(views.js.thealgorithm())
  }

  def thealgorithm = WebSocket.async[JsValue] { request => 
    (algo <-?- Join(models.MemberId("testid"))).map { case JoinResponse(in,out) =>
      (in,out)
    }
  }
}