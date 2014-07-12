package actors

import play.api.libs.iteratee._
import play.api.libs.json._
import akka.actor._
import scala.concurrent.ExecutionContext.Implicits.global
import shared.models._
import shared.commands._
import org.scalajs.spickling.playjson._
import shared.AlgoPickler
import scala.concurrent.duration._

case object SendInitial
case object RemoveWS

class PlayerActor(player: Character, squadsRef: ActorRef) extends Actor {

  var activeWS = 0
  var logout = false
  val (out,channel) = Concurrent.broadcast[JsValue]

  def receive = {
    case (Join(_), snd:ActorRef) => {
      val in = Iteratee.foreach[JsValue] { msg =>
        commands(msg)
      }.map { wat =>
        if(!logout) {
          self ! RemoveWS
        }
      }

      if(activeWS == 0) {
        squadsRef ! NewPlayer(self,player)
      }

      activeWS += 1
      snd ! Joined((in,out))
    }

    case Logout => {
      logout = true
      channel.eofAndEnd()
      self ! PoisonPill
    }

    case resp: Response => {
      channel.push(AlgoPickler.pickle(resp))
    }

    case RemoveWS => {
      activeWS -= 1
      context.system.scheduler.scheduleOnce(15 seconds) {
        if(activeWS <= 0) {
          self ! PoisonPill
        }
      }
    }
  }

  def commands(inp: JsValue) = {
    AlgoPickler.unpickle(inp) match {
      case JoinSquad(lid) => squadsRef ! JoinSquadAkka(lid,player.cid)
      case UnassignSelf => squadsRef ! UnassignSelfAkka(player.cid)
      case DisbandSquad => squadsRef ! DisbandSquadAkka(player.cid)
      case Logout => self ! Logout
      case cmd: Commands => { println(s"Player good: $cmd") ; squadsRef ! cmd }
      case _ => println("Unknown Command!",inp)
    }
  }
}

object PlayerActor {
  def props(player: Character, squadsRef: ActorRef): Props = Props(new PlayerActor(player,squadsRef))
}
