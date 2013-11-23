package actors

import akka.actor._
import akka.channels._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import soe.CensusParser
import models._
import play.api.libs.ws.WS

sealed trait CensusRequest
case object GetOnlineCharecters extends CensusRequest
case class SoeValidateCharacter(name: String, cid: String) extends CensusRequest
case class Lookup(partial: String) extends CensusRequest

sealed trait CensusResult
case class OnlineCharecters(cids: Set[CharacterId]) extends CensusResult
case class LookupResult(refs: List[CharacterRef]) extends CensusResult
case class SoeValidateCharacterResult(isValid: Boolean, cid: String) extends CensusResult

//Utility
case object Tick
case class UpdateOnlineCharacter(members: Set[CharacterId])

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

class CensusLookupActor extends Actor with Channels[TNil,(Lookup,LookupResult) :+: TNil] {
  channel[Lookup] { case (Lookup(partial),snd) =>
    //val LOOKUP_URL = s"https://census.soe.com/get/ps2:v2/character_name/?name.first_lower=%5E$partial&c:limit=10&c:show=name.first,name.first_lower,character_id&c:sort=name.first_lower"
    val LOOKUP_URL = s"https://census.soe.com/get/ps2:v2/character_name/?name.first_lower=%5E$partial&c:limit=20&c:sort=name.first_lower&c:join=characters_world%5Eon:character_id%5Eouter:0%5Eterms:world_id=1"
    WS.url(LOOKUP_URL).get().map{ response =>
      val refs = CensusParser.parseLookupCharacters(response.json)
      snd <-!- LookupResult(refs)
    }
  }
}

class SoeValidateCharacterActor extends Actor with Channels[TNil,(SoeValidateCharacter,SoeValidateCharacterResult) :+: TNil] {
  channel[SoeValidateCharacter] { case (SoeValidateCharacter(name,cid),snd) =>
    val VALIDATE_URL = s"http://census.soe.com/get/ps2:v2/character/?name.first_lower=${name.toLowerCase}&c:join=characters_world%5Eon:character_id%5Eouter:0%5Eterms:world_id=1"
    WS.url(VALIDATE_URL).get().map{ response =>
      CensusParser.parseValidateName(response.json).map { validated_cid =>
        snd <-!- SoeValidateCharacterResult(true,validated_cid)
      }.getOrElse {
        snd <-!- SoeValidateCharacterResult(false,"")
      }
    }
  }
}

class SoeCensusSupervisor extends Actor with Channels [(AlgoRequest,Nothing) :+: TNil, (UpdateOnlineCharacter,Nothing) :+: (CensusRequest,CensusResult)  :+: TNil] {
  implicit val timeout = akka.util.Timeout(Duration(5,"seconds"))

  var census:Option[ChannelRef[(Tick.type,Nothing) :+: TNil]] = None

  var online: Set[CharacterId] = Set.empty

  override def preStart() = {
    census = Some(createChild(new SoeCensusActor()))
    context.system.scheduler.scheduleOnce(500 milliseconds, census.get.actorRef, Tick)
  }

  channel[CensusRequest] {
    case (GetOnlineCharecters,snd) => snd <-!- OnlineCharecters(online)

    case (Lookup(partial),snd) => {
      if(partial.size < 3) snd <-!- LookupResult(List.empty)
      else {
        var task = createChild(new CensusLookupActor)
        (task <-?- Lookup(partial)).map { case result => snd <-!- result}
      }
    }

    case (SoeValidateCharacter(name,cid),snd) => {
      var task = createChild(new SoeValidateCharacterActor)
      snd <-!- (task <-?- SoeValidateCharacter(name,cid))
    }
  }

  channel[UpdateOnlineCharacter] { case (UpdateOnlineCharacter(members),snd) =>
    val new_online = members.diff(online)
    val new_offline = online.diff(members)
    online = members
    new_online.foreach { cid => parentChannel <-!- SetOnlineStatus(cid,true); println(s"Set $cid ONLINE"); }
    new_offline.foreach { cid => parentChannel <-!- SetOnlineStatus(cid,false); println(s"Set $cid OFFLINE"); }
  }

}
