package models

case class SquadType(name: String, roles: Array[String])

case class Squad(
  stype: SquadType, 
  leader: MemberDetail,
  members: List[MemberDetail],
  assignments: List[(MemberId,String)]) {

  def score(member: MemberDetail, role: String): Int = {
    member.preferences.get(role).getOrElse(0)
  }

  def doAssignments(input: List[MemberDetail]) = {
    var unassigned = input
    stype.roles.take(input.size).foldLeft(List.empty[(MemberId,String)]) { case (acc,role) =>
      val ordered = unassigned.map(m => (score(m,role),m)).sortBy(_._1).reverse
      val (_,selected) = ordered.head
      unassigned = ordered.tail.map(_._2)
      (selected.id,role) :: acc
    }
  }
  
  def place(new_member: MemberDetail) = {
    val updated_members = new_member :: members
    copy(members=updated_members,assignments=doAssignments(updated_members))
  }

  def remove(member: MemberDetail) = {
    val updated_members = members.filter(_.id == member.id)
    copy(members=updated_members,assignments=doAssignments(updated_members))
  }
  
  def getRole(mid: MemberId): Option[String] = assignments.find(_._1 == mid).map(_._2)
}
