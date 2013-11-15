package actors

import akka.actor._
import akka.channels._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import soe.CensusParser
import models.CharacterId
import play.api.libs.ws.WS

sealed trait CensusRequest
case object GetOnlineCharecters extends CensusRequest

sealed trait CensusResult
case class OnlineCharecters(cids: List[CharacterId]) extends CensusResult

//Utility
case object Tick
case class UpdateOnlineCharacter(members: List[CharacterId])

class SoeCensusActor extends Actor with Channels[(UpdateOnlineCharacter,Nothing) :+: TNil,(Tick.type,Nothing) :+: TNil] {

  val SERVICE_ID = "s:umbra"
  val ONLINE_MEMBERS_URL = "http://census.soe.com/get/ps2:v2/outfit?alias=BAID&c:resolve=member_online_status,member_character_name"

  channel[Tick.type] { (tick,snd) =>
    WS.url(ONLINE_MEMBERS_URL).get().map{ response =>
      val members = CensusParser.parseOnlineCharacter(response.json)
      parentChannel <-!- UpdateOnlineCharacter(members)
      context.system.scheduler.scheduleOnce(5 seconds,self,Tick)
    }
  }
}

class SoeCensusSupervisor extends Actor with Channels [TNil, (UpdateOnlineCharacter,Nothing) :+: (CensusRequest,CensusResult)  :+: TNil] {

  var census:Option[ChannelRef[(Tick.type,Nothing) :+: TNil]] = None

  var online: List[CharacterId] = List.empty

  override def preStart() = {
    census = Some(createChild(new SoeCensusActor()))
    context.system.scheduler.scheduleOnce(500 milliseconds, census.get.actorRef, Tick)
  }

  channel[CensusRequest] { case (GetOnlineCharecters,snd) =>
    snd <-!- OnlineCharecters(online)
  }

  channel[UpdateOnlineCharacter] { case (UpdateOnlineCharacter(members),snd) =>
    online = members
  }

}