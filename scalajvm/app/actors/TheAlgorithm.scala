package actors

import akka.actor._

case class LookupCharacterRequest(partial: String)
case class LookupCharacterResult(refs: List[soe.CensusParser.SoeCharacterRef])

class TheAlgorithm extends Actor {

  def receive = {

    case req @ LookupCharacterRequest(partial) => {
      val soe = context.actorOf(Props[SoeActor])
      soe ! (req,sender())
    }

    case q => {
      println("Hey wat? -- " + q)
    }
  }
}
