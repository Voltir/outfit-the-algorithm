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
        Unit
        /*
        val p1 = fakeit(members.head)
        val p2 = fakeit(members.head.head)
        p1 shouldEqual p2
        */
      }
    }
  }
}
