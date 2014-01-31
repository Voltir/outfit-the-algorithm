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
case class TrackCid(cid: CharacterId) extends CensusRequest

sealed trait CensusResult
case class OnlineCharecters(cids: Set[CharacterId]) extends CensusResult
case class LookupResult(refs: List[CharacterRef]) extends CensusResult
case class SoeValidateCharacterResult(isValid: Boolean, cid: String) extends CensusResult

sealed trait CensusCommand
case class UpdateOnlineCharacter(members: Set[CharacterId]) extends CensusCommand
case class UpdateResources(resources: Map[CharacterId,Resources]) extends CensusCommand

trait SoeCensusCommand
case object TickOnline extends SoeCensusCommand
case object TickResources extends SoeCensusCommand
case class AddTracked(cid: CharacterId) extends SoeCensusCommand with CensusCommand
case class RemoveTracked(cid: CharacterId) extends SoeCensusCommand with CensusCommand

object SoeConfig {
  val SERVICE_ID = "s:algorithm"
}

import SoeConfig._

class SoeCensusActor extends Actor with Channels[
  (CensusCommand,Nothing) :+: TNil, 
  (SoeCensusCommand,Nothing) :+: TNil] {

  var tracked: Set[CharacterId] = Set()


  val ONLINE_OUTFIT_URL = s"http://census.soe.com/$SERVICE_ID/get/ps2:v2/outfit?alias=BAID&c:resolve=member_online_status,member_character_name"

  def ANY_ONLINE_URL(lookup: Set[CharacterId]) = {
    val ids = lookup.map(_.id).mkString(",")
    s"http://census.soe.com/$SERVICE_ID/get/ps2:v2/characters_online_status/?character_id=$ids"
  }

  def CURRENCY_TRACKER_URL(lookup: Set[CharacterId]) = {
    val ids = lookup.map(_.id).mkString(",")
    s"http://census.soe.com/$SERVICE_ID/get/ps2:v2/character/?character_id=$ids&c:show=character_id&c:resolve=currency"
  }

  channel[SoeCensusCommand] { 
    case (TickOnline,snd) => {
      WS.url(ANY_ONLINE_URL(tracked)).get().map { response =>
        val members = CensusParser.parseOnlineCharacter(response.json)
        parentChannel <-!- UpdateOnlineCharacter(members)
        context.system.scheduler.scheduleOnce(10 seconds,self,TickOnline)
      }
      /*
      WS.url(ONLINE_OUTFIT_URL).get().map { response =>
        val members = CensusParser.parseOnlineCharacter(response.json)
        parentChannel <-!- UpdateOnlineCharacter(members)
        context.system.scheduler.scheduleOnce(5 seconds,self,TickOnline)
      }
      */
    }
  
    case (TickResources,snd) => {
      if(tracked.size > 0) {
        WS.url(CURRENCY_TRACKER_URL(tracked)).get().map { response =>
          val resource_map = CensusParser.parseCurrency(response.json)
          parentChannel <-!- UpdateResources(resource_map)
        }
      }
      context.system.scheduler.scheduleOnce(60 seconds,self,TickResources)
    }

    case (AddTracked(cid),snd) => { 
      tracked += cid 
      context.system.scheduler.scheduleOnce(1 seconds,self,TickResources)
    }
    
    case (RemoveTracked(cid),snd) => tracked -= cid
  }
}

class CensusLookupActor extends Actor with Channels[TNil,(Lookup,LookupResult) :+: TNil] {
  channel[Lookup] { case (Lookup(partial),snd) =>
    val LOOKUP_URL = s"https://census.soe.com/$SERVICE_ID/get/ps2:v2/character_name/?name.first_lower=%5E$partial&c:limit=20&c:sort=name.first_lower&c:join=characters_world%5Eon:character_id%5Eouter:0%5Eterms:world_id=1"
    WS.url(LOOKUP_URL).get().map{ response =>
      val refs = CensusParser.parseLookupCharacters(response.json)
      snd <-!- LookupResult(refs)
    }
  }
}

class SoeValidateCharacterActor extends Actor with Channels[TNil,(SoeValidateCharacter,SoeValidateCharacterResult) :+: TNil] {
  channel[SoeValidateCharacter] { case (SoeValidateCharacter(name,cid),snd) =>
    val VALIDATE_URL = s"http://census.soe.com/$SERVICE_ID/get/ps2:v2/character/?name.first_lower=${name.toLowerCase}&c:join=characters_world%5Eon:character_id%5Eouter:0%5Eterms:world_id=1"
    WS.url(VALIDATE_URL).get().map{ response =>
      CensusParser.parseValidateName(response.json).map { validated_cid =>
        snd <-!- SoeValidateCharacterResult(true,validated_cid)
      }.getOrElse {
        snd <-!- SoeValidateCharacterResult(false,"")
      }
    }
  }
}

class SoeCensusSupervisor extends Actor with Channels [(AlgoRequest,Nothing) :+: TNil, (CensusCommand,Nothing) :+: (CensusRequest,CensusResult)  :+: TNil] {
  implicit val timeout = akka.util.Timeout(Duration(5,"seconds"))

  var census:Option[ChannelRef[(SoeCensusCommand,Nothing) :+: TNil]] = None

  var online: Set[CharacterId] = Set.empty

  override def preStart() = {
    census = Some(createChild(new SoeCensusActor()))
    context.system.scheduler.scheduleOnce(500 milliseconds, census.get.actorRef, TickOnline)
    context.system.scheduler.scheduleOnce(500 milliseconds, census.get.actorRef, TickResources)
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

  channel[CensusCommand] { 
    case (UpdateOnlineCharacter(members),snd) => {
      val new_online = members.diff(online)
      val new_offline = online.diff(members)
      online = members
      new_online.foreach { cid => parentChannel <-!- SetOnlineStatus(cid,true); println(s"Set $cid ONLINE"); }
      new_offline.foreach { cid => parentChannel <-!- SetOnlineStatus(cid,false); println(s"Set $cid OFFLINE"); }
    }

    case (AddTracked(cid),snd) => {
      println(s"TRACKING!! $cid")
      census.get <-!- AddTracked(cid)
    }

    case (RemoveTracked(cid),snd) => { 
      println(s"NOT TRACKING!! $cid")
      census.get <-!- RemoveTracked(cid)
    }

    case (UpdateResources(resources),snd) => {
      parentChannel <-!- SetResources(resources)
    }

    case _ => {
      println("RECEIVED SOME CRAZY CENSUS COMMAND")
    }
  }
}
