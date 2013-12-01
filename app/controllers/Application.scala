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
import play.api.data._
import play.api.data.Forms._

import models._
import scala.concurrent.Future

object Application extends Controller {
  import play.api.Play.current
  implicit val timeout = akka.util.Timeout(Duration(5,"seconds"))

  val algo = ChannelExt(Akka.system).actorOf(new TheAlgorithm(),"the-algorithm")

  def index = Action {
    Ok(views.html.index(true))
  }
  
  def indexNoAuto = Action {
    Ok(views.html.index(false))
  }

  def memberFromPrefData(pd: PreferenceData): MemberDetail  = {
    val prefs = Map(
      Roles.HA->pd.ha,
      Roles.MEDIC->pd.medic,
      Roles.ENGY->pd.engy,
      Roles.LA->pd.la,
      Roles.INF->pd.inf)

    MemberDetail(
      id=CharacterId(pd.cid),
      name=pd.name,
      leader=pd.leader,
      point=pd.point,
      prefs=prefs)
  }

  def auto = Action.async { implicit request =>
    request.body.asJson.map { js =>
      js.validate[PreferenceData].map { pref =>
        val mem = memberFromPrefData(pref)
        (algo <-?- JoinSquad(mem)).map {
          case JoinSquadResult(success) => {
            println(s"AUTO CONTROLLER -- Did JOIN $mem -$success")
            if(success) Ok("Logged in")
            else BadRequest("Full...")
        }
          case _ => BadRequest("The world ended")
        }
      }.getOrElse(Future(BadRequest("Invalid Data")))
    }.getOrElse(Future(BadRequest("I am working on it...")))
  }

  def profile(name: String, cid: String) = Action.async {
    (algo <-?- ValidateCharacter(name,cid)).map {
      case ValidateCharacterResult(isValid, validated_cid) =>
        if(isValid) Ok(views.html.profile(name,validated_cid))
        else Redirect(routes.Application.indexNoAuto)
      case _ => Redirect(routes.Application.indexNoAuto)
    }.recover { case _ =>
      Thread.sleep(2500)
      Redirect(routes.Application.index)
    }
  }

  def active(name: String, char_id: String) = Action { implicit request =>
    val is_baid = true
    Ok(views.html.active(char_id,name,is_baid))
  }

  def lookupCharacters(partial: String) = Action.async {
    (algo <-?- LookupCharacterList(partial.toLowerCase)).map {
      case LookupCharacterListResponse(refs) => Ok(Json.toJson(List(refs)))
      case _ => Ok(Json.arr())
    }
  }

  def squadInfo(char_id: String) = Action.async {
    import JSFormat._
    (algo <-?- GetSquadData).map {
      case SquadDataResult(maybeSquad,online,resources) => maybeSquad.map { squad =>
        val result = Json.obj(
          "leader"->squad.leader.name,
          "leader_id"->squad.leader.id.id,
          "my_assignment"->squad.assignments.get(CharacterId(char_id)),
          "assignments"-> squad.members.toList.map { a =>
            val is_online = online.find(_ == a.id).map(_ => "online").getOrElse("offline")
            Json.obj(
              "name"->a.name,
              "id"->a.id.id,
              "assignment"->squad.assignments.get(a.id),
              "resources"->resources.get(a.id),
              "online"->is_online)
          }
        )
        Ok(result)
    }.getOrElse(Ok(Json.toJson("No data")))

    case _ => Ok(Json.toJson("No data"))

    }
  }

  def indexJS = Action { implicit request =>
    Ok(views.js.indexJS())
  }

  def thealgorithmJS(char_id: String) = Action { implicit request =>
    Ok(views.js.thealgorithm(char_id))
  }

  def thealgorithmSoundsJS(char_id: String) = Action { implicit request =>
    Ok(views.js.algosounds(char_id))
  }

  def thealgorithm(cid: String) = WebSocket.async[JsValue] { request => 
    (algo <-?- CommandSocket(models.CharacterId(cid))).map {
      case CommandSocketResponse(in,out) => (in,out)
      case _ => (Iteratee.ignore,Enumerator.empty)
    }
  }
}
