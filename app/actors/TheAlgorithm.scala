package actors

import models._
import models.JSFormat._

import akka.actor._
import akka.channels._

import play.api._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json._
import play.api.libs.functional.syntax._

sealed trait AlgoRequest
case class SetOnlineStatus(cid: CharacterId, status: Boolean) extends AlgoRequest
case class SetResources(resources: Map[CharacterId,Resources]) extends AlgoRequest
case class LookupCharacterList(partial: String) extends AlgoRequest
case class ValidateCharacter(name: String, cid: String) extends AlgoRequest
case class JoinSquad(mem: Member) extends AlgoRequest
case object GetSquads extends AlgoRequest
case class RoleChange(cid: CharacterId,assignment: Assignment) extends AlgoRequest
case class CommandSocket(cid: CharacterId) extends AlgoRequest
case object WatTick extends AlgoRequest

sealed trait AlgoResult
case class LookupCharacterListResponse(refs: List[CharacterRef]) extends AlgoResult
case class ValidateCharacterResult(isValid: Boolean, cid: String) extends AlgoResult
case class JoinSquadResult(success: Boolean) extends AlgoResult
case class SquadsResult(squads: Squads, online: Set[CharacterId],resources:Map[CharacterId,Resources]) extends AlgoResult
case class CommandSocketResponse(iteratee: Iteratee[JsValue,_], enumeratee: Enumerator[JsValue]) extends AlgoResult

class TheAlgorithm extends Actor with Channels[TNil,(AlgoRequest,AlgoResult) :+: TNil] {

  implicit val timeout = akka.util.Timeout(Duration(5,"seconds"))

  //var members = Set.empty[MemberId]
  val (algoEnumerator, algoChannel) = Concurrent.broadcast[JsValue]

  var soe_supervisor: Option[ChannelRef[
    (CensusCommand,Nothing) :+: (CensusRequest,CensusResult)  :+: TNil]] = None


  var squad_actor: Option[ChannelRef[
    (AlgoRequest,AlgoResult) :+: (SquadCommand,Nothing) :+: TNil]] = None

  override def preStart() = {
    soe_supervisor = Some(createChild(new SoeCensusSupervisor()))
    squad_actor = Some(createChild(new SquadActor(selfChannel.narrow)))
    context.system.scheduler.scheduleOnce(Duration(5,"seconds"), self, WatTick)
  }

  channel[AlgoRequest] {

    case (LookupCharacterList(partial),snd) => {
      (soe_supervisor.get <-?- Lookup(partial)).map {
        case LookupResult(refs) => snd <-!- LookupCharacterListResponse(refs)
      }
    }

    case (ValidateCharacter(name, cid),snd) => {
      (soe_supervisor.get <-?- SoeValidateCharacter(name,cid)).map {
        case SoeValidateCharacterResult(isValid,validated_cid) =>  snd <-!- ValidateCharacterResult(isValid,validated_cid)
        case _ => snd <-!- ValidateCharacterResult(false,"")
      }
    }

    case (JoinSquad(mem),snd) => {
      (squad_actor.get <-?- JoinSquad(mem)) -!-> snd
    }

    case (SetOnlineStatus(cid,status),snd) => (squad_actor.get <-!- SetActivity(cid,status))

    case (SetResources(resources),snd) => (squad_actor.get <-!- SetSquadResources(resources))

    case (GetSquads,snd) => {
      /*val online = tmp_squad.get.members.filter(m => tmp_online_status.get(m.id).getOrElse(false)).map(_.id)
      snd <-!- GetSquadDataResponse(tmp_squad,online)*/
      (squad_actor.get <-?- GetSquads).map { snd <-!- _ }
    }

    case (RoleChange(cid,role),snd) => {
      algoChannel.push(Json.obj("role_change"->cid.id,"assignment"->role))
    }

    case (CommandSocket(cid),snd) => {
      
      soe_supervisor.get <-!- AddTracked(cid)
      
      val iteratee = Iteratee.foreach[JsValue] { event =>

        event.transform((__ \ "remove").json.pick[JsString]).map(_.value).foreach { cid_str =>
          (squad_actor.get <-!- RemoveMember(CharacterId(cid_str)))
          algoChannel.push(Json.obj("remove"->cid_str))
        }

        event.transform((__ \ "unassign").json.pick[JsString]).map(_.value).foreach { cid_str =>
          (squad_actor.get <-!- UnassignMember(CharacterId(cid_str)))
        }

        event.transform((__ \ "create_squad").json.pick[JsString]).map(_.value).foreach { cid_str =>
          (squad_actor.get <-!- CreateSquad(CharacterId(cid_str)))
        }

        event.transform((__ \ "leaderize").json.pick[JsString]).map(_.value).foreach { cid_str =>
          (squad_actor.get <-!- MakeLeader(CharacterId(cid_str)))
        }
        
        event.transform((__ \ "set_standard").json.pick[JsString]).map(_.value).foreach { cid_str =>
          (squad_actor.get <-!- SetSquadType(SquadTypes.STANDARD,CharacterId(cid_str)))
        }
        
        event.transform((__ \ "set_support").json.pick[JsString]).map(_.value).foreach { cid_str =>
          (squad_actor.get <-!- SetSquadType(SquadTypes.SUPPORT,CharacterId(cid_str)))
        }
        
        event.transform((__ \ "set_jetpack").json.pick[JsString]).map(_.value).foreach { cid_str =>
          (squad_actor.get <-!- SetSquadType(SquadTypes.JETPACK,CharacterId(cid_str)))
        }

        event.transform((__ \ "set_crash").json.pick[JsString]).map(_.value).foreach { cid_str =>
          (squad_actor.get <-!- SetSquadType(SquadTypes.CRASH,CharacterId(cid_str)))
        }

        if(event == Json.obj("command"->"reset")) {
          (squad_actor.get <-!- ResetSquads)
          algoChannel.push(Json.obj("command"->"reset"))
        }
        if(event == Json.obj("change"->"change")) {
          (squad_actor.get <-!- RandomizeLeader)
        }
      }.map { _ => 
        soe_supervisor.get <-!- RemoveTracked(cid)
        println("AlgoSocket -- Quitting") 
      }
      snd <-!- CommandSocketResponse(iteratee,algoEnumerator)
    }

    case (WatTick,snd) => {
      algoChannel.push(Json.obj("tick"->"tock"))
      context.system.scheduler.scheduleOnce(Duration(5,"seconds"),self,WatTick)
    }

    case _ => println("RECEIVED SOME CRAZY ALGO REQUEST")
  }
}
