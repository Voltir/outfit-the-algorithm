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

sealed trait AlgoRequest
case object GetOnlineMembers extends AlgoRequest
case class Join(mid: MemberId) extends AlgoRequest

sealed trait AlgoResponse
case class OnlineMembers(members: List[Member]) extends AlgoResponse
case class JoinResponse(iteratee: Iteratee[JsValue,_], enumeratee: Enumerator[JsValue]) extends AlgoResponse

class TheAlgorithm extends Actor with Channels[TNil,(AlgoRequest,AlgoResponse) :+: TNil] {

  implicit val timeout = akka.util.Timeout(Duration(5,"seconds"))

  var members = Set.empty[MemberId]
  val (algoEnumerator, algoChannel) = Concurrent.broadcast[JsValue]

  var soe_supervisor: Option[ChannelRef[
    (UpdateOnlineCharacter,Nothing) :+: (CensusRequest,CensusResult)  :+: TNil]] = None

  var member_supervisor: Option[ChannelRef[
    (MemberRequest,MemberResult) :+: TNil]] = None

  override def preStart() = {
    soe_supervisor = Some(createChild(new SoeCensusSupervisor()))
    member_supervisor = Some(createChild(new MemberSupervisor()))
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

    case (Join(mid),snd) => {
      val iteratee = Iteratee.foreach[JsValue] { event =>
        algoChannel.push(Json.obj("foo"->"bar"))
      }.map { _ => println("Quitting") }
      snd <-!- JoinResponse(iteratee,algoEnumerator)
    }
  }
}
