package actors

import play.api.libs.iteratee._
import play.api.libs.json._
import akka.actor._
import scala.concurrent.ExecutionContext.Implicits.global
import shared.models._
import shared.commands._
import org.scalajs.spickling.playjson._
import shared.AlgoPickler

case object SendInitial

class PlayerActor(player: Character, squadsRef: ActorRef) extends Actor {

  val (out,channel) = Concurrent.broadcast[JsValue]

  def receive = {
    case (Join(_), snd:ActorRef) => {
      val in = Iteratee.foreach[JsValue] { msg =>
        println(s"Iteratee: $msg")
        commands(msg)
      }.map { wat =>
        println("I THINK I AM CLOSED!")
      }
      snd ! Joined((in,out))
    }

    case resp: Response => {
      println("PUSHING RESPONSE")
      channel.push(AlgoPickler.pickle(resp))
    }
  }

  def commands(inp: JsValue) = {
    AlgoPickler.unpickle(inp) match {
      case join @ JoinSquad(lid) => squadsRef ! JoinSquadAkka(lid,player.cid) 
      case UnassignSelf => squadsRef ! UnassignSelfAkka(player.cid) 
      case cmd: Commands => { println(s"Player good: $cmd") ; squadsRef ! cmd }
      case _ => println("Unknown Command!",inp)
    }
  }
}

object PlayerActor {
  def props(player: Character, squadsRef: ActorRef): Props = Props(new PlayerActor(player,squadsRef))
}
