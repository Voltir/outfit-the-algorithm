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

case class RegistrationData(
  character_name: String,
  pass1: String,
  pass2: String
)

case class PreferanceFormData(
  ha: Option[String],
  Medic: Option[String],
  Engy: Option[String],
  La: Option[String],
  Inf: Option[String]
)

//https://census.soe.com/get/ps2:v2/character_name/?name.first_lower=^voltaire&c:limit=10&c:show=name.first,character_id
//https://census.soe.com/get/ps2:v2/character/5428010618015225217/?c:resolve=faction,world

object Application extends Controller {
  import play.api.Play.current

  val preferanceForm = Form(mapping(
    "HA"->optional(text),
    "Medic"->optional(text),
    "Engy"->optional(text),
    "LA"->optional(text),
    "Infiltraitor"->optional(text)
  )(PreferanceFormData.apply)(PreferanceFormData.unapply))

  implicit val timeout = akka.util.Timeout(Duration(5,"seconds"))

  val algo = ChannelExt(Akka.system).actorOf(new TheAlgorithm(),"the-algorithm")

  def index = Action {
    Ok(views.html.index())
  }

  def profile(name: String, cid: String) = Action {
    Ok(views.html.profile(name,cid,preferanceForm))
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
        (algo <-?- JoinSquad(mem)).map { case JoinSquadResult(success) =>
          if(success) Redirect(routes.Application.active(cid))
          else Ok("Full...")
        }
      }
    )
  }

  def active(char_id: String) = Action { implicit request =>
    Ok(views.html.active(char_id))
  }

  def lookupCharacters(partial: String) = Action.async {
    (algo <-?- LookupCharacterList(partial.toLowerCase)).map { case LookupCharacterListResponse(refs) =>
      Ok(Json.toJson(List(refs)))
    }
  }

  def squadInfo(char_id: String) = Action.async {
    import JSFormat._
    (algo <-?- GetSquadData).map { case SquadDataResult(maybeSquad,online) =>
      maybeSquad.map { squad =>
        val result = Json.obj(
          "leader"->squad.leader.name,
          "role"->squad.getRole(CharacterId(char_id)),
          "assignments"->Json.arr { for { a <- squad.assignments } yield {
            val is_online = online.find(_ == a._1.id).map(_ => true).getOrElse(false)
            println(s"${a._1.id} in $online? -- $is_online");
            Json.obj(
              "name"->a._1.name,
              "role"->a._2,
              "online"->is_online)
          }}
        )
        Ok(result)
      }.getOrElse(Ok(Json.toJson("No data")))
    }
  }

  def indexJS = Action { implicit request =>
    Ok(views.js.indexJS())
  }

  def thealgorithmJS(char_id: String) = Action { implicit request =>
    Ok(views.js.thealgorithm(char_id))
  }

  def thealgorithm = WebSocket.async[JsValue] { request => 
    (algo <-?- CommandSocket(models.CharacterId("testid"))).map { case CommandSocketResponse(in,out) =>
      (in,out)
    }
  }
}
