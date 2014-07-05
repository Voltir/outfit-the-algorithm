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

class PlayerActor(cid: CharacterId, squadsRef: ActorRef) extends Actor {

  val (out,channel) = Concurrent.broadcast[JsValue]

  def receive = {
    case (Join(_), snd:ActorRef) => {
      val in = Iteratee.foreach[JsValue] { msg =>
        commands(msg)
      }.map { wat =>
        println("I THINK I AM CLOSED!")
      }
      squadsRef ! Join(cid)
      snd ! Joined((in,out))
    }

    case resp: Response => {
      println("PUSHING RESPONSE")
      channel.push(AlgoPickler.pickle(resp))
    }
  }

  def commands(inp: JsValue) = {
    AlgoPickler.unpickle(inp) match {
      case LoadInitial => squadsRef ! LoadInitial
    }
  }
}

object PlayerActor {
  def props(cid: CharacterId, squadsRef: ActorRef): Props = Props(new PlayerActor(cid,squadsRef))
}
