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

  override def preStart() = {
    context.system.scheduler.schedule(3 seconds,30 seconds,self,ELBKeepAlive)
  }

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

    case ELBKeepAlive => {
      println("ELB PUSH!")
      channel.push(AlgoPickler.pickle(ELBKeepAlive))
    }
  }

  def commands(inp: JsValue) = {
    AlgoPickler.unpickle(inp) match {
      case LoadInitial(pref: PreferenceDefinition) => squadsRef ! LoadInitialAkka(player.cid,pref)
      case JoinSquad(lid) => squadsRef ! JoinSquadAkka(lid,player.cid)
      case MoveToSquad(lid,mid) => squadsRef ! JoinSquadAkka(lid,mid)
      case UnassignSelf => squadsRef ! UnassignSelfAkka(player.cid)
      case Unassign(cid) => squadsRef ! UnassignSelfAkka(cid)
      case DisbandSquad => squadsRef ! DisbandSquadAkka(player.cid)
      case SetPattern(pattern) => squadsRef ! SetPatternAkka(player.cid,pattern)
      case pin @ PinAssignment(lid,pattern,idx) => squadsRef ! AddPinAkka(player.cid,pin)
      case UnpinAssignment(lid,pattern) => squadsRef ! RemovePinAkka(player.cid,lid,pattern)
      case SetPreference(pref) => squadsRef ! SetPreferenceAkka(player.cid,pref)
      case Logout => self ! Logout
      case cmd: Commands => { println(s"Player good: $cmd") ; squadsRef ! cmd }
      case _ => println("Unknown Command!",inp)
    }
  }
}

object PlayerActor {
  def props(player: Character, squadsRef: ActorRef): Props = Props(new PlayerActor(player,squadsRef))
}
