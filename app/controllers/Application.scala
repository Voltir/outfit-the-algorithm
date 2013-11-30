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

case class PreferenceData(
  cid: String,
  name: String
)


case class PreferanceFormData(
  ha: Option[String],
  Medic: Option[String],
  Engy: Option[String],
  La: Option[String],
  Inf: Option[String]
)

object Application extends Controller {
  import play.api.Play.current

  val preferanceForm = Form(mapping(
    "HA"->optional(text),
    "Medic"->optional(text),
    "Engy"->optional(text),
    "LA"->optional(text),
    "Infiltraitor"->optional(text)
  )(PreferanceFormData.apply)(PreferanceFormData.unapply))

  implicit val prefFormat = Json.format[PreferenceData]

  implicit val timeout = akka.util.Timeout(Duration(5,"seconds"))

  val algo = ChannelExt(Akka.system).actorOf(new TheAlgorithm(),"the-algorithm")

  def index = Action {
    Ok(views.html.index(true))
  }
  
  def indexNoAuto = Action {
    Ok(views.html.index(false))
  }

  def memberFromPrefData(pd: PreferenceData): MemberDetail  = {
    var prefs = Map[String,Int]()
    MemberDetail(
      id=CharacterId(pd.cid),
      name=pd.name,
      totalTime=0.0,
      leadTime=1.0,
      desire="HIGH",
      preferences=prefs)
  }

  def auto = Action.async { implicit request =>
    request.body.asJson.map { js =>
      js.validate[PreferenceData].map { pref =>
        println(pref)
        val mem = memberFromPrefData(pref)
        (algo <-?- JoinSquad(mem)).map {
          case JoinSquadResult(success) =>
            if(success) Ok("Logged in")
            else BadRequest("Full...")
          case _ => BadRequest("The world ended")
        }
      }.getOrElse(Future(BadRequest("Invalid Data")))
    }.getOrElse(Future(BadRequest("I am working on it...")))
  }

  def profile(name: String, cid: String) = Action.async {
    (algo <-?- ValidateCharacter(name,cid)).map {
      case ValidateCharacterResult(isValid, validated_cid) =>
        if(isValid) Ok(views.html.profile(name,validated_cid,preferanceForm))
        else Redirect(routes.Application.index)
      case _ => Redirect(routes.Application.index)
    }
  }

  def handleProfile(name: String, cid: String) = Action.async { implicit request =>
    preferanceForm.bindFromRequest.fold(
      errors => Future(BadRequest(views.html.profile(name,cid,errors))),
      pref => {
        def updatePref(key: String, option: Option[String], acc: Map[String,Int]): Map[String,Int] = {
          option match {
            case Some("M") => acc + (key->25)
            case Some("H") => acc + (key->50)
            case _ => acc + (key->0)
          }
        }
        var prefs = Map[String,Int]()
        prefs = updatePref(Roles.HA,pref.ha,prefs)
        prefs = updatePref(Roles.MEDIC,pref.Medic,prefs)
        prefs = updatePref(Roles.ENGY,pref.Engy,prefs)
        prefs = updatePref(Roles.LA,pref.La,prefs)
        prefs = updatePref(Roles.INF,pref.Inf,prefs)
        val mem = MemberDetail(
          id=CharacterId(cid),
          name=name,
          totalTime=0.0,
          leadTime=1.0,
          desire="HIGH",
          preferences=prefs)
        (algo <-?- JoinSquad(mem)).map {
          case JoinSquadResult(success) =>
            if(success) Redirect(routes.Application.active(cid))
            else Ok("Full...")
          case _ => Ok("The world ended")
        }
      }
    )
  }

  def active(char_id: String) = Action { implicit request =>
    val is_baid = true
    Ok(views.html.active(char_id,is_baid))
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
