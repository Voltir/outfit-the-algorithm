package actors

import akka.actor._
import play.api.Play.current
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.{Map => MutableMap, ArrayBuffer}
import shared.models._
import shared.commands._

class SquadsActor extends Actor {

  val squads: ArrayBuffer[Squad] = Squad.fake
  
  val unassigned: ArrayBuffer[Character] = ArrayBuffer(Squad.FakeLeader1,Squad.FakeLeader2)

  def receive = {

    case Join(cid) => {
      println(s"User wants to join: $cid")
    }

    case LoadInitial => {
      sender() ! LoadInitialResponse(squads.toList,unassigned.toList)
    }
  }
}
