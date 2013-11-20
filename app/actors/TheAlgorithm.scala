package actors

import models._
import akka.actor._
import akka.channels._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import fakedata.FakeSquadType

sealed trait AlgoRequest
case object GetOnlineMembers extends AlgoRequest
case object SetOnline extends AlgoRequest
case class LookupCharacterList(partial: String) extends AlgoRequest
case class Join(mem: MemberDetail) extends AlgoRequest
case class GetSquadData(char_id: String) extends AlgoRequest
case class CommandSocket(cid: CharacterId) extends AlgoRequest
case object WatTick extends AlgoRequest

sealed trait AlgoResponse
case class OnlineMembers(members: List[Member]) extends AlgoResponse
case class LookupCharacterListResponse(refs: List[CharacterRef]) extends AlgoResponse
case class JoinResponse(success: Boolean) extends AlgoResponse
case class GetSquadDataResponse(squad: Option[Squad], online: List[CharacterId]) extends AlgoResponse
case class CommandSocketResponse(iteratee: Iteratee[JsValue,_], enumeratee: Enumerator[JsValue]) extends AlgoResponse

class TheAlgorithm extends Actor with Channels[TNil,(AlgoRequest,AlgoResponse) :+: TNil] {

  implicit val timeout = akka.util.Timeout(Duration(5,"seconds"))

  var members = Set.empty[MemberId]
  val (algoEnumerator, algoChannel) = Concurrent.broadcast[JsValue]

  var soe_supervisor: Option[ChannelRef[
    (UpdateOnlineCharacter,Nothing) :+: (CensusRequest,CensusResult)  :+: TNil]] = None

  var member_supervisor: Option[ChannelRef[
    (MemberRequest,MemberResult) :+: TNil]] = None

  var tmp_squad: Option[Squad] = None

  override def preStart() = {
    soe_supervisor = Some(createChild(new SoeCensusSupervisor()))
    member_supervisor = Some(createChild(new MemberSupervisor()))
    context.system.scheduler.scheduleOnce(Duration(5,"seconds"), self, WatTick)
  }

  channel[AlgoRequest] {
    case (GetOnlineMembers,snd) => {
      (soe_supervisor.get <-?- GetOnlineCharecters).map { case OnlineCharecters(cids) =>
        //tmp
        cids.map { cid:CharacterId =>
          (member_supervisor.get <-?- StoreNewMember(s"Fakes${cid.id}")).map { case StoredId(mid) =>
            (member_supervisor.get <-?- AssociateCharecter(mid,cid))
          }
        }
        //end tmp
        (member_supervisor.get <-?- GetMembers(cids)).map {
          case Members(members) => snd <-!- OnlineMembers(members)
        }
      }
    }

    case (LookupCharacterList(partial),snd) => {
      (soe_supervisor.get <-?- Lookup(partial)).map {
        case LookupResult(refs) => snd <-!- LookupCharacterListResponse(refs)
      }
    }

    case (Join(mem),snd) => {
      tmp_squad.map { squad =>
        if(squad.members.size < 12)  {
          tmp_squad = Some(squad.place(mem))
          snd <-!- JoinResponse(true)
        } else {
          snd <-!- JoinResponse(false)
        }

      }.getOrElse {
        tmp_squad = Some(Squad.make(FakeSquadType(),mem))
        snd <-!- JoinResponse(true)
      }
    }

    case (GetSquadData(char_id),snd) => {
      (soe_supervisor.get <-?- GetOnlineCharecters).map { case OnlineCharecters(online) =>
        snd <-!- GetSquadDataResponse(tmp_squad,online)
      }
    }

    case (CommandSocket(mid),snd) => {
      val iteratee = Iteratee.foreach[JsValue] { event =>
        println(event)
        if(event == Json.obj("command"->"reset")) {
          tmp_squad = None
          algoChannel.push(Json.obj("command"->"reset"))
        }
        algoChannel.push(Json.obj("foo"->"bar"))
      }.map { _ => println("AlgoSocket -- Quitting") }
      snd <-!- CommandSocketResponse(iteratee,algoEnumerator)
    }

    case (WatTick,snd) => {
      algoChannel.push(Json.obj("tick"->"tock"))
      context.system.scheduler.scheduleOnce(Duration(5,"seconds"),self,WatTick)
    }
  }
}
