
import org.scalatest.FunSpec
import org.scalatest._

import org.joda.time._

import models._
import fakedata._

class TheAlgorithmTests extends FunSpec with Matchers {

  describe("The Algorithm") {
    describe("A Member") {
      it("Should have a leadership preference") {
        val leader = FakeLeader("leader1")
        leader.leadership should be(Leadership.HIGH)
      }

      it("Should have a squad tendency preference") {
        val member = FakeMember("m1")
        member.tendency == Tendency.INFANTRY
      }
    }

    describe("A Squad") {
      it("should give assignments by preference") {
        //val leader = FakeLeader("a")
        //var squad = Squad.make(SquadTypes.STANDARD,0,leader)
        //for { i <- 0 until 11 } yield { squad = squad.place(FakeMember(s"mem$i"))}
      }

      it("should be full when it has 12 members") {
        val leader = FakeLeader("a")
        var squad = Squad.make(SquadTypes.STANDARD,0,leader)
        for { i <- 0 until 11 } yield { squad = squad.place(FakeMember(s"mem$i"))}
        squad.members.size should be(12)
        squad.full should be(true)
      }

      it("should break ties based on date") {
        val m1 = FakeMember("m1",Map(Roles.HA->8,Roles.MEDIC->2))
        val m2 = FakeMember("m2",Map(Roles.HA->8,Roles.MEDIC->2))
        val m3 = FakeMember("m3",Map(Roles.HA->8,Roles.MEDIC->2))
        val m4 = FakeMember("m4",Map(Roles.HA->8,Roles.MEDIC->2))
        
        val joined = Map(
          m1.id->DateTime.now.minusHours(5), 
          m2.id->DateTime.now.minusHours(4),
          m3.id->DateTime.now.minusHours(3),
          m4.id->DateTime.now.minusHours(2))
        
        val mems = Set(m1,m2,m3,m4)
        val squad = Squad(0,
          SquadTypes.STANDARD,
          m1,
          mems,
          joined,
          false,
          Squad.doAssignments(SquadTypes.STANDARD,m1,mems,joined))

        println(squad.members.map(m => (m.id,squad.getAssignment(m.id))).mkString("\n"))

        val removed = squad.remove(m2.id)
        println(removed.members)

      }
    }

    describe("Multi-Squad tracking") {
      it("should create a squad when needed and a leader is available") {
        val squads = new Squads()
        val leader = FakeLeader("leader1")
        squads.assign(leader)
        squads.numSquads should be(1)
      }

      it("should add a member to the squad if one is available") {
        val squads = new Squads()
        val leader = FakeLeader("leader1")
        val member = FakeMember("m1")
        squads.assign(leader)
        squads.assign(member)
        squads.numSquads should be(1)
        squads.unassigned.size should be(0)
      }

      it("should keep members unassigned if no leader is available") {
        val squads = new Squads()
        val member = FakeMember("m1")
        squads.assign(member)
        squads.unassigned.size should be(1)
      }

      it("should fill available squads then spill into unassigned when full") {
        val squads = new Squads()
        val leader = FakeLeader("leader1")
        squads.assign(leader)
        for { i <- 1 to 11 } yield { squads.assign(FakeMember(s"mem$i"))}
        squads.numSquads should be(1)
        squads.unassigned.size should be(0)
        squads.assign(FakeMember("spill"))
        squads.numSquads should be(1)
        squads.unassigned.size should be(1)
      }

      it("should assign unassigned members when a leader comes on") {
        val squads = new Squads()
        for { i <- 1 to 12 } yield { squads.assign(FakeMember(s"mem$i"))}
        squads.numSquads should be(0)
        squads.assign(FakeLeader("l"))
        squads.numSquads should be(1)
        squads.unassigned.size should be(1)
      }

      it("should allow a leader to create a squad on demand") {
        val squads = new Squads()
        val leader = FakeLeader("leader1")
        val air_lead = FakeLeader("air").copy(tendency=Tendency.AIR)
        squads.assign(leader)
        squads.makeSquad(air_lead)
        squads.numSquads should be(2)
        squads.unassigned.size should be(0)
      }

      it("should prefer similar tendencies when assigning members") {
        val squads = new Squads()
        val inf_lead = FakeLeader("inf1")
        val air_lead = FakeLeader("air_lead").copy(tendency=Tendency.AIR)
        squads.makeSquad(inf_lead)
        squads.makeSquad(air_lead)
        for { i <- 1 to 12 } yield {
          val tendency = if(i % 2 == 0) Tendency.INFANTRY else Tendency.AIR
          squads.assign(FakeMember(s"mem$i").copy(tendency=tendency))
        }
        squads.squads.foreach { squad =>
          squad.members.foreach { m => m.tendency should be(squad.leader.tendency) }
          squad.members.size should be(7)
        }
      }
    }
  }
}
