package com.hydra.core
import java.math.BigInteger
import org.scalatest._
import MessageType._

class MessageTest extends FunSuite with BeforeAndAfter {
  val map = scala.collection.mutable.LinkedHashMap(
    "keyString" -> "value1",
    "keyInt" -> 123,
    "keyLong" -> (Int.MaxValue.toLong + 100),
    "keyBigInteger" -> new BigInteger(s"${Long.MaxValue}").add(new BigInteger(s"${Long.MaxValue}")),
    "keyBooleanFalse" -> false,
    "KeyBooleanTrue" -> true,
    "keyByteArray" -> Array[Byte](1, 2, 2, 2, 2, 1, 1, 2, 2, 1, 1, 4, 5, 4, 4, -1),
    "keyIntArray" -> Array[Int](3, 526255, 1321, 4, -1),
    "keyNull" -> null,
    "keyLongMax" -> Long.MaxValue,
    "keyLongMin" -> Long.MinValue,
    "keyDouble" -> 1.242,
    "keyDouble2" -> -12.2323e-100,
    "keyDouble3" -> Double.MaxValue,
    "keyDouble4" -> Double.MinPositiveValue,
    "keyBigInteger2" -> BigInteger.valueOf(Long.MaxValue).add(BigInteger.TEN)
  )

  before {
  }

  test("Test MessageID and its increcement") {
//    val m0 = Message.newBuilder.create
//    val id0 = m0.messageID
//    for (i <- Range(0, 100)) {
//      val mi = Message.newBuilder.create
//      assert(mi.messageID == id0 + 1 + i)
//    }
//    Vector(
//      Message.wrap(Map()),
//      Message.wrap(Map(Message.KeyMessageID -> "-1")),
//      Message.wrap(Map(Message.KeyMessageID -> None)),
//      Message.wrap(Map(Message.KeyMessageID -> 1.2))
//    ).foreach(m => { intercept[IllegalArgumentException] { m.messageID } })
//    assert(Message.wrap(Map(Message.KeyMessageID -> 100)).messageID == 100)
  }

  test("Test get information") {
    val m = Message.wrap(map)
    assert(m.get[String]("keyString").get == "value1")
    intercept[ClassCastException] {
      m.get[Int]("keyString").get + 1
    }
    intercept[IllegalArgumentException] {
      m.get[String]("keyNull", false)
    }
    assert(m.get[String]("keyNull", true) == None)
  }

  test("Test basic information") {
    val b = Message.newBuilder
    assert(b.create.to == None)
    b.to(null)
    assert(b.create.to == None)
    b.to("the target")
    assert(b.create.to.get == "the target")
  }

  test("Test type and content") {
    val builder = Message.newBuilder
    assert(builder.create.messageType == Unknown)
    val m1 = builder.asRequest("TestRequest1").create
    assert(m1.messageType == Request)
    assert(m1.requestContent == ("TestRequest1", Nil, Map()))
    intercept[IllegalArgumentException] {
      builder.asRequest("TestRequest2", List(100, "arg"), Map("a" -> 1, Message.KeyTo -> "11")).create
    }
    val m2 = builder.asRequest("TestRequest2", List(100, "arg"), Map("a" -> 1, "b" -> "bb")).create
    assert(m2.messageType == Request)
    assert(m2.requestContent == ("TestRequest2", 100 :: "arg" :: Nil, Map("b" -> "bb", "a" -> 1)))
    val m3 = builder.asResponse("ContentOfResponse", 100).create
    assert(m3.messageType == Response)
    intercept[IllegalStateException] {
      m3.requestContent
    }
    assert(m3.responseContent == ("ContentOfResponse", 100))
    val m4 = builder.asError("ContentOfError", 1001).create
    assert(m4.messageType == Error)
    intercept[IllegalStateException] {
      m4.requestContent
    }
    intercept[IllegalStateException] {
      m4.responseContent
    }
    assert(m4.errorContent == ("ContentOfError", 1001))
  }

  test("Test update message.") {
    val m1 = Message.newBuilder.asRequest("TestRequest1").create
    assert(m1.get[Int]("testitem") == None)
    val m2 = m1 + ("testitem" -> 100)
    assert(m2.get[Int]("testitem").get == 100)
    val m3 = m2.builder.create
    assert(m3.get[Int]("testitem").get == 100)
    val m4 = (m3.builder += ("t2" -> "11")).create
    assert(m4.get[Int]("testitem").get == 100)
    assert(m4.get[String]("t2").get == "11")
    val r1 = m4.response(99)
    assert(r1.messageType == Response)
    assert(r1.responseContent == (99, m4.messageID))
    val e1 = m4.error(999)
    assert(e1.messageType == Error)
    assert(e1.errorContent == (999, m4.messageID))
  }
}

