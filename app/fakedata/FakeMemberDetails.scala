package fakedata

import models._

object FakeMemberDetail {
  def apply(id: String, totalTime: Double, leadTime: Double, desire:String) = {
    MemberDetail(id=MemberId(id),totalTime=totalTime,leadTime=leadTime,
      desire=desire,capabilities=List.empty,preferences=List.empty)
  }
}
