package actors

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._
import shared.models._
import squad.Preference

case class LookupCharacterRequest(partial: String)
case class LookupCharacterResult(refs: List[soe.CensusParser.SoeCharacterRef])

case class Join(character: Character)
case class Joined(socket: (Iteratee[String,_],Enumerator[String]))

case class NewPlayer(player: ActorRef, character: Character)

class TheAlgorithm extends Actor {

  lazy val squadsActor = context.actorOf(Props[SquadsActor])

  def receive = {

    case req @ LookupCharacterRequest(partial) => {
      val soe = context.actorOf(Props[SoeActor])
      soe ! (req,sender())
    }

    case req @ Join(character) => {
      val player = context.child(character.cid.toString).getOrElse {
        context.actorOf(PlayerActor.props(character,squadsActor),character.cid.toString)
      }
      player ! (req,sender())
    }

    case q => {
      println("Hey? -- " + q)
    }
  }
}
