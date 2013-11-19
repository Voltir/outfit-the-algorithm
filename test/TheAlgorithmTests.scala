import org.scalatest.FunSpec
import org.scalatest._

import models._
import fakedata._

class TheAlgorithmTests extends FunSpec with Matchers {

  def fakeit(all: List[MemberDetail], check: MemberDetail) = {
    //w = 0.3*norm_unlead_ratio+0.7*norm_desire_ratio
    val unlead_ratio = all.map(m => 1.0 - (m.leadTime/m.totalTime))
    val unlead_ratio_total = unlead_ratio.foldLeft(0.0){case (acc,e) => acc+e}
    val unlead_ratio_norm = unlead_ratio.map(_/unlead_ratio_total)
    val desire_total = all.foldLeft(0.0){case (acc,m) => acc+m.desireScore}
    val desire_ratio_norm = all.map(m => m.desireScore/desire_total)
    val leadership_prob = desire_ratio_norm.zip(unlead_ratio_norm).map { case (drn,urn) =>
      0.5*urn+0.5*drn
    }
    println(leadership_prob)
    println(leadership_prob.foldLeft(0.0){case (acc,e) => acc+e})
  }

  describe("The Algorithm") {
    describe("Leadership Proababilities Calculations") {
      it("should calculate even proabilities given similar members") {
        val members: List[MemberDetail] = List(
          FakeMemberDetail("Member1",3600.0,0.0,"LOW"),
          FakeMemberDetail("Member2",3600.0,3000.0,"LOW"),
          FakeMemberDetail("Member3",3600.0,0.0,"LOW"),
          FakeMemberDetail("Member3",3600.0,0.0,"LOW"),
          FakeMemberDetail("Member3",3600.0,0.0,"LOW")
        )
        fakeit(members,members.head)
      }
    }
    describe("A Squad Type") {
      it("should define an ordered list of preferences") {
        val medic1 = FakeMemberDetail("Want Medic (1)",3600.0,0.0,"LOW").copy(preferences=Map(Roles.MEDIC -> 50))
        val medic2 = FakeMemberDetail("Want Medic (2)",3600.0,0.0,"LOW").copy(preferences=Map(Roles.MEDIC -> 50))
        val meh = FakeMemberDetail("Meh",3600.0,0.0,"LOW")
        val ha = FakeMemberDetail("Want HA (1)",3600.0,0.0,"LOW").copy(preferences=Map(Roles.HA -> 50))
        val standard = FakeSquadType()
        val squad = new Squad(standard,medic1,List(medic1),List.empty) 
        println(squad.place(meh).place(medic2).place(ha).assignments)
      }
    }
  }
}
