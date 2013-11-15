package actors

import akka.actor._
import akka.channels._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.ws.WS
import soe.CensusParser
import models.Member

trait UmbraRequest
case object GetOnlineMembers extends UmbraRequest

trait UmbraResult
case class OnlineMembers(members: List[Member]) extends UmbraResult

//Utility
case object Tick
case class UpdateOnlineMembers(members: List[Member])

class SoeCensusActor extends Actor with Channels[(UpdateOnlineMembers,Nothing) :+: TNil,(Tick.type,Nothing) :+: TNil] {

  val SERVICE_ID = "s:umbra"
  val ONLINE_MEMBERS_URL = "http://census.soe.com/get/ps2:v2/outfit?alias=BAID&c:resolve=member_online_status"

  channel[Tick.type] { (tick,snd) =>
    WS.url(ONLINE_MEMBERS_URL).get().map{ response =>
      val members = CensusParser.parseOnlineMembers(response.json)
      parentChannel <-!- UpdateOnlineMembers(members)
      context.system.scheduler.scheduleOnce(5 seconds,self,Tick)
    }
  }
}

class SoeCensusSupervisor extends Actor with Channels [TNil, (UpdateOnlineMembers,Nothing) :+: (UmbraRequest,UmbraResult)  :+: TNil] {

  var census:Option[ChannelRef[(Tick.type,Nothing) :+: TNil]] = None

  var online: List[Member] = List.empty

  override def preStart() = {
    census = Some(createChild(new SoeCensusActor()))
    context.system.scheduler.scheduleOnce(500 milliseconds, census.get.actorRef, Tick)
  }

  channel[UmbraRequest] { case (GetOnlineMembers,snd) => snd <-!- OnlineMembers(online) }

  channel[UpdateOnlineMembers] { case (UpdateOnlineMembers(members),snd) =>
    println(s"SUPERVISOR GOT $members")
    online = members
  }

}