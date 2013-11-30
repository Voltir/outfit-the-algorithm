import org.scalatest.FunSpec
import org.scalatest._

import org.joda.time._

import models._

class TheAlgorithmTests extends FunSpec with Matchers {

  describe("The Algorithm") {
    describe("A Squad") {
      it("should give assignments by preference") {
      }
      it("should break ties based on date") {
        val m1 = MemberDetail(CharacterId("m1"),"LikesHA","NEVER","NEVER",Map(Roles.HA->8,Roles.MEDIC->2))
        val m2 = MemberDetail(CharacterId("m2"),"LikesHA","NEVER","NEVER",Map(Roles.HA->8,Roles.MEDIC->2))
        val m3 = MemberDetail(CharacterId("m3"),"LikesHA","NEVER","NEVER",Map(Roles.HA->8,Roles.MEDIC->2))
        val m4 = MemberDetail(CharacterId("m4"),"LikesHA","NEVER","NEVER",Map(Roles.HA->8,Roles.MEDIC->2))
        
        val joined = Map(
          m1.id->DateTime.now.minusHours(5), 
          m2.id->DateTime.now.minusHours(4),
          m3.id->DateTime.now.minusHours(3),
          m4.id->DateTime.now.minusHours(2))
        
        val mems = Set(m1,m2,m3,m4)
        val squad = Squad(
          SquadTypes.STANDARD,
          m1,
          mems,
          joined,
          false,
          Squad.doAssignments(SquadTypes.STANDARD,m1,mems,joined))

        println(squad.members.map(m => (m.id,squad.getAssignment(m.id))).mkString("\n"))
      }
    }
  }
}
