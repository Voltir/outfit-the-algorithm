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
import actors.LookupCharacterListResponse
import actors.CommandSocketResponse
import actors.ValidateCharacter
import actors.CommandSocket
import actors.JoinSquad
import models.Member
import actors.ValidateCharacterResult
import actors.LookupCharacterList
import actors.JoinSquadResult
import models.PreferenceData
import models.CharacterId

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

  def memberFromPrefData(pd: PreferenceData): Member  = {
    val prefs = Map(
      Roles.HA->pd.ha.getOrElse(0),
      Roles.MEDIC->pd.medic.getOrElse(0),
      Roles.ENGY->pd.engy.getOrElse(0),
      Roles.LA->pd.la.getOrElse(0),
      Roles.INF->pd.inf.getOrElse(0),
      Roles.MAX->pd.MAX.getOrElse(0),
      Roles.MAG->pd.magrider.getOrElse(0),
      Roles.HARASSER->pd.harasser.getOrElse(0),
      Roles.LIGHTNING->pd.lightning.getOrElse(0),
      Roles.SUNDERER->pd.sunderer.getOrElse(0),
      Roles.GALAXY->pd.galaxy.getOrElse(0),
      Roles.SCYTHE->pd.scythe.getOrElse(0),
      Roles.LIB->pd.liberator.getOrElse(0))

    Member(
      id=CharacterId(pd.cid),
      tendency=Tendency.INFANTRY,
      name=pd.name,
      leadership=pd.leader.getOrElse(Leadership.NEVER),
      canMentor=false,
      point=pd.point.getOrElse(Leadership.NEVER),
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
      }.getOrElse(Future(BadRequest("Invalid Preference Data")))
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

  case class MemberJS(
    name: String,
    id: String,
    is_leader: Boolean,
    assignment: Option[Assignment],
    resources: Option[Resources],
    online: String
  )

  case class SquadJS(
    leader: String,
    leader_id: String,
    squad_id: Int,
    members: List[MemberJS]
  )

  //case class SquadsResult(squads: Squads, online: Set[CharacterId],resources:Map[CharacterId,Resources]) extends AlgoResult
  def squadInfo(char_id: String) = Action.async {
    import JSFormat._
    implicit val FormatMemberJS = Json.format[MemberJS]
    implicit val FormatSquadJS = Json.format[SquadJS]

    def jasonize(squad: Squad, resources: Map[CharacterId,Resources], online: Set[CharacterId]): SquadJS = {
      SquadJS(
        leader=squad.leader.name,
        leader_id=squad.leader.id.id,
        squad_id=squad.id,
        members=squad.members.toList.map { m =>
          val is_online = online.find(_ == m.id).map(_ => "online").getOrElse("offline")
          MemberJS(
            m.name,
            m.id.id,
            m.id.id == squad.leader.id.id,
            squad.getAssignment(m.id),
            resources.get(m.id),
            is_online
          )
        }
      )
    }

    (algo <-?- GetSquads).map {
      case SquadsResult(squads,online,resources) => {
        val unassigned = squads.unassigned.toList.map(_._1)
        squads.getSquad(CharacterId(char_id)).map { my_squad =>
          val other_squads = squads.squads.toList.filter(_ != my_squad)
          val result = Json.obj(
            "my_squad"->jasonize(my_squad,resources,online),
            "my_assignment"->my_squad.assignments.get(CharacterId(char_id)),
            "other_squads"->other_squads.map(jasonize(_,resources,online)),
            "unassigned"->unassigned.map { m =>
              val is_online = online.find(_ == m.id).map(_ => "online").getOrElse("offline")
              MemberJS(m.name,m.id.id,my_squad.leader.id.id == m.id.id,None,None,is_online)
            }
          )
          Ok(result)
        }.getOrElse {
          val result = Json.obj(
            "other_squads" -> squads.squads.toList.map(s => jasonize(s,resources,online)),
            "unassigned"->unassigned.map { m =>
              val is_online = online.find(_ == m.id).map(_ => "online").getOrElse("offline")
              MemberJS(m.name,m.id.id,false,None,None,is_online)
            }
          )
          Ok(result)
        }
      }
      case _ => Ok(Json.toJson("No data"))
    }.recover { case err =>
      Ok(Json.toJson("GetSquads timed out..."))
    }

    /* TODO REMOVE
    (algo <-?- GetSquads).map {
      case SquadsResult(squads,online,resources) => {
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
    }

    case _ => Ok(Json.toJson("No data"))

    }
    */
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
