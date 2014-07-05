package actors

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._
import shared.models._

case class LookupCharacterRequest(partial: String)
case class LookupCharacterResult(refs: List[soe.CensusParser.SoeCharacterRef])

case class Join(cid: CharacterId)
case class Joined(socket: (Iteratee[JsValue,_],Enumerator[JsValue]))

class TheAlgorithm extends Actor {

  lazy val squadsActor = context.actorOf(Props[SquadsActor])

  def receive = {

    case req @ LookupCharacterRequest(partial) => {
      val soe = context.actorOf(Props[SoeActor])
      soe ! (req,sender())
    }

    case req @ Join(cid) => {
      val player = context.child(cid.toString).getOrElse {
        context.actorOf(PlayerActor.props(cid,squadsActor),cid.toString)
      }
      player ! (req,sender())
    }

    case q => {
      println("Hey wat? -- " + q)
    }
  }
}
