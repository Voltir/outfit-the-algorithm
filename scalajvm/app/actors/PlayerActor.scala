package actors

import upickle._
import forceit._
import play.api.libs.iteratee._
import play.api.libs.json._
import akka.actor._
import scala.concurrent.ExecutionContext.Implicits.global
import shared.models._
import shared.commands._
import scala.concurrent.duration._
import java.nio.channels.ClosedChannelException

case object SendInitial
case object RemoveWS

class PlayerActor(player: Character, squadsRef: ActorRef) extends Actor {

  var activeWS = 0
  var logout = false
  val (out,channel) = Concurrent.broadcast[String]

  override def preStart() = {
    context.system.scheduler.schedule(3 seconds,30 seconds,self,ELBKeepAlive)
  }

  def receive = {
    case (Join(_), snd:ActorRef) => {
      val in = Iteratee.foreach[String] { msg =>
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
      try {
        channel.push(upickle.write(resp))
      } catch  {
        case wat:ClosedChannelException => {
          println(s"${player.name}: Push failed, plz dont die...")
          context.system.scheduler.scheduleOnce(3 seconds) {
            self ! ELBKeepAlive
          }
        }
        case _: Throwable => {
          println(s"${player.name}: Catch all push failed?")
        }
      }
    }

    case RemoveWS => {
      activeWS -= 1
      context.system.scheduler.scheduleOnce(30 seconds) {
        if(activeWS <= 0) {
          self ! PoisonPill
        }
      }
    }

    case ELBKeepAlive => {
      println("ELB PUSH!")
      channel.push(upickle.write(ELBKeepAlive))
    }
  }

  def commands(inp: String) = {
    println(s"${player.name} received command")
    upickle.read[Commands](inp) match {
      case LoadInitial(pref: PreferenceDefinition) => squadsRef ! LoadInitialAkka(player.cid,pref)
      case JoinSquad(lid) => squadsRef ! JoinSquadAkka(lid,player.cid)
      case MoveToSquad(lid,mid) => squadsRef ! JoinSquadAkka(lid,mid)
      case UnassignSelf => squadsRef ! UnassignSelfAkka(player.cid)
      case Unassign(cid) => squadsRef ! UnassignSelfAkka(cid)
      case DisbandSquad => squadsRef ! DisbandSquadAkka(player.cid)
      case SetPattern(pattern,lid) => squadsRef ! SetPatternAkka(player.cid,pattern,lid)
      case pin @ PinAssignment(lid,pattern,idx) => squadsRef ! AddPinAkka(player.cid,pin)
      case UnpinAssignment(lid,pattern) => squadsRef ! RemovePinAkka(player.cid,lid,pattern)
      case SetPreference(pref) => squadsRef ! SetPreferenceAkka(player.cid,pref)
      case VolunteerFC(cid) => squadsRef ! VolunteerFCAkka(player)
      case StepDownFC => squadsRef ! StepDownFCAkka(player)
      case MakeLeader(lid,target) => squadsRef ! MakeLeaderAkka(player.cid,lid,target)
      case Logout => self ! Logout
      case cmd: Commands => { println(s"Player good: $cmd") ; squadsRef ! cmd }
      case _ => println("Unknown Command!",inp)
    }
  }
}

object PlayerActor {
  def props(player: Character, squadsRef: ActorRef): Props = Props(new PlayerActor(player,squadsRef))
}
