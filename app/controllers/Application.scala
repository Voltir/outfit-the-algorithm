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

object Application extends Controller {
  import play.api.Play.current
  implicit val timeout = akka.util.Timeout(Duration(5,"seconds"))

  val temp = ChannelExt(Akka.system).actorOf(new TheAlgorithm(),"the-algorithm")

  def index = Action {
    Ok(views.html.index())
  }

  def tmponline = Action.async {
    (temp <-?- GetOnlineMembers).map { case OnlineMembers(cids) =>
      Ok(Json.toJson(cids))
    }
  }

  def thealgorithmJS = Action { implicit request =>
    Ok(views.js.thealgorithm())
  }

  def thealgorithm = WebSocket.async[JsValue] { request => 
    (temp <-?- Join(models.MemberId("testid"))).map { case JoinResponse(in,out) =>
      (in,out)
    }
  }
}
