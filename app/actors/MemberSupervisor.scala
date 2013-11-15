package actors

import akka.actor._
import akka.channels._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import models.{CharacterId, Member, MemberId}

object api {
  trait MemberStore {
    def save(name: String): MemberId
    def assoc(mid: MemberId, cid: CharacterId): Unit
    def get(mid: MemberId): Option[Member]
    def getByMemberName(name: String): Option[Member]
    def getByCharId(cid: CharacterId): Option[Member]
    def getMembersFromCID(cids: List[CharacterId]): List[Member]
  }
}

object MockMemberStore extends api.MemberStore {
  import scala.collection.concurrent.{Map => MutableMap}
  var count = 0
  val char_map: MutableMap[CharacterId,MemberId] = scala.collection.concurrent.TrieMap()
  val mem_map: MutableMap[MemberId,Member] = scala.collection.concurrent.TrieMap()

  def save(name: String) = {
    count += 1
    val mid = MemberId(s"mem$count")
    mem_map.put(mid,Member(mid,name))
    mid
  }

  def get(mid: MemberId) = mem_map.get(mid)

  def assoc(mid: MemberId, cid: CharacterId): Unit = char_map.putIfAbsent(cid,mid)

  def getByCharId(cid: CharacterId) = char_map.get(cid).flatMap(mid => mem_map.get(mid))

  def getByMemberName(name: String) = mem_map.find { case (mid,m) => m.name == name }.map(_._2)

  def getMembersFromCID(cids: List[CharacterId]): List[Member] = {
    println(s"Looking for $cids")
    cids.map(cid => getByCharId(cid)).flatten
  }
}

sealed trait MemberRequest
case class StoreNewMember(name: String) extends MemberRequest
case class AssociateCharecter(mid: MemberId, cid:CharacterId) extends MemberRequest
case class GetMembers(cids: List[CharacterId]) extends MemberRequest

sealed trait MemberResult
case class Members(members: List[Member]) extends MemberResult
case class StoredId(mid: MemberId) extends MemberResult

class MemberSupervisor extends Actor with Channels[TNil,(MemberRequest,MemberResult) :+: TNil] {
  val store = MockMemberStore

  channel[MemberRequest] {
    case (StoreNewMember(name), snd) => {
      val check = store.getByMemberName(name)
      check.map(m => snd <-!- StoredId(m.id)).getOrElse(snd <-!- StoredId(store.save(name)))
    }

    case (AssociateCharecter(mid: MemberId, cid: CharacterId),snd) => {
      store.assoc(mid,cid)
    }

    case (GetMembers(cids),snd) => {
      snd <-!- Members(store.getMembersFromCID(cids))
    }

  }

}