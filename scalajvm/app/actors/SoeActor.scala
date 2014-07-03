package actors

import akka.actor._
import play.api.Play.current
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global
import soe.CensusParser

class SoeActor extends Actor {

  val SERVICE_ID = "s:algorithm"

  //URLS
  def receive = {
    case (LookupCharacterRequest(partial), snd:ActorRef) => {
      val LOOKUP_URL = s"https://census.soe.com/$SERVICE_ID/get/ps2:v2/character_name/?name.first_lower=%5E$partial&c:limit=20&c:sort=name.first_lower&c:join=characters_world%5Eon:character_id%5Eouter:0%5Eterms:world_id=1"
      WS.url(LOOKUP_URL).get().map{ response =>
        val refs = CensusParser.parseLookupCharacters(response.json)
        snd ! LookupCharacterResult(refs)
      }.recover { case err =>
        println("Error accessing SOE Census in lookup!",err)
        snd ! LookupCharacterResult(List.empty)
      }
    }
  }
}
