package com.hydra.io

import com.hydra.io.MessageClient
import com.hydra.io.MessageServer
import com.hydra.io.MessageSession
import com.hydra.io.MessageTransport
import com.hydra.io.RemoteInvokeException
import com.hydra.io.RemoteObject
import com.hydra.io.BlockingRemoteObject
import com.hydra.io.SessionListener
import java.io.IOException
import java.util.concurrent.CountDownLatch

import com.hydra.core.MessageEncodingProtocol
import org.scalatest._
import com.hydra.core.MessageType._
import com.hydra.io.MessageTransport.FutureDirect

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class MessageTransportTest extends FunSuite with BeforeAndAfter with BeforeAndAfterAll {
  val port = 55659
  lazy val server = new MessageServer(port)
  lazy val channelFuture = server.start

  override def beforeAll() {
    channelFuture.awaitUninterruptibly
  }

  override def afterAll() {
    server.stop.await
  }

  before {
  }

  test("Test configuration load.") {
    assert(MessageTransport.Configuration.getProperty("messageserver.port", "20102") == "20102")
    //TODO: Test unchangable here.
  }

  test("Test server status.") {
    assert(channelFuture.isDone)
    assert(channelFuture.isSuccess)
  }

  test("Test session creation and registration") {
    val initialSize = MessageSession.sessionsCount
    val s1 = MessageSession.create("S1", null)
    assert(MessageSession.sessionsCount == initialSize + 1)
    val s2 = MessageSession.create("S2", null)
    assert(s2.id - s1.id == 1)
    assert(MessageSession.sessionsCount == initialSize + 2)
    s2.close
    assert(MessageSession.sessionsCount == initialSize + 1)
    val s3 = MessageSession.create("S3", null)
    val s4 = MessageSession.create("S4", null)
    assert(MessageSession.sessionsCount == initialSize + 3)
    assert(s3.id - s1.id == 2)
    assert(s4.id - s1.id == 3)
    assert(s4.name == "S4")
    assert(MessageSession.getSession(s3.id).get == s3)
    assert(MessageSession.getSession(s1.name).get == s1)
    assert(MessageSession.getSession(s2.name) == None)
    assert(MessageSession.getSession(s2.id) == None)
    assert(MessageSession.getSessions.size == MessageSession.sessionsCount)
    s1.close
    s2.close
    s3.close
    s4.close
    assert(MessageSession.sessionsCount == initialSize)
    assert(MessageSession.getSession(s4.name) == None)
    assert(MessageSession.getSession(s4.id) == None)
    assert(MessageSession.getSessions.size == MessageSession.sessionsCount)
  }

  test("Test connection.") {
    val mc = new MessageClient("", "localhost", port, None)
    val f = mc.start
    f.await(1000)
    assert(f.isSuccess)
  }

  test("Test dynamic invoker.") {
    val client = new MessageClient("", "localhost", port, None)
    val invoker = client.toMessageInvoker()
    val m1 = invoker.fun1(a = 1, 2, "3", b = null, c = Vector(1, 2, "3d"))
    assert(m1.messageType == Request)
    assert(m1.requestContent == ("fun1", 2 :: "3" :: Nil, Map("a" -> 1, "b" -> null, "c" -> Vector(1, 2, "3d"))))
    intercept[IllegalArgumentException] {
      val m1 = invoker.fun2(To = 1, 2, "3", b = null, c = Vector(1, 2, "3d"))
    }
    val invoker2 = client.toMessageInvoker("OnT")
    val m2 = invoker2.fun2
    assert(m2.messageType == Request)
    assert(m2.requestContent == ("fun2", Nil, Map()))
    assert(m2.to.get == "OnT")
    client.stop.await
  }

  test("Test remote invoke and future.") {
    val client1 = new MessageClient("", "localhost", port, None)
    val f1 = client1.start
    f1.await(1000)
    assert(f1.isSuccess)
    val invoker1 = client1.asynchronousInvoker()
    val future1 = invoker1.co
    val latch1 = new CountDownLatch(1)
    var uS1: Any = None
    future1.onComplete { case t =>
      uS1 = t.isSuccess match {
        case true => t.get
        case _ => t.failed.get
      }
      latch1.countDown
    }
    assert(latch1.await(2, java.util.concurrent.TimeUnit.SECONDS))
    assert(uS1.getClass.getSimpleName == "RemoteInvokeException")
    assert(uS1.asInstanceOf[RemoteInvokeException].getMessage == "Method not found: co.")
    client1.stop.await
  }

  test("Test register client.") {
    val mc1 = new MessageClient("TestClient1", "localhost", port, None)
    val f1 = mc1.start
    f1.await(1000)
    assert(f1.isSuccess)
    val future1 = mc1.connect
    val r1 = Await.result[Any](future1, 1 second)
    assert(r1 == Unit)
    val mc2 = new MessageClient("TestClient2", "localhost", port, None)
    val f2 = mc2.start
    f2.await(1000)
    assert(f2.isSuccess)
    val invoker2 = mc2.blockingInvoker()
    val r2 = invoker2.connect("TestClient2")
    assert(r2 == Unit)
    intercept[RemoteInvokeException] {
      try {
        val r3 = invoker2.connect("TestClient2")
      }
      catch {
        case e: NullPointerException => e.printStackTrace
        case e: Throwable => throw e
      }
    }
    mc1.stop
    mc2.stop
  }

  test("Test invoke other client") {
    class Target {
      def v8 = "V8 great!"

      def v9 = throw new IllegalArgumentException("V9 not good.")

      def v10 = throw new IOException("V10 have problems.")

      def v(i: Int, b: Boolean) = "OK"
    }
    val mc1 = new MessageClient("T1-Benz", "localhost", port, new Target)
    val f1 = mc1.start.sync
    assert(f1.isDone)
    assert(f1.isSuccess)
    mc1.blockingInvoker().connect("T1-Benz")
    val checker = MessageClient.newClient("localhost", port, "T1-Checher")
    val benzChecker = checker.blockingInvoker("T1-Benz")
    val v8r = benzChecker.v8
    assert(v8r == "V8 great!")
    intercept[RemoteInvokeException] {
      val v9r = benzChecker.v9
    }
    intercept[RemoteInvokeException] {
      val v10r = benzChecker.v10
    }
    assert(benzChecker.v(1, false) == "OK")
    intercept[RemoteInvokeException] {
      benzChecker.v(false, false)
    }
    mc1.stop.await
    checker.stop.await
  }

  test("Test client name duplicated") {
    val mc1 = new MessageClient("T2-ClientDuplicated", "localhost", port, None)
    val f1 = mc1.start.sync
    assert(f1.isDone)
    assert(f1.isSuccess)
    mc1.blockingInvoker().connect("T2-ClientDuplicated")
    val mc2 = new MessageClient("T2-ClientDuplicated", "localhost", port, None)
    val f2 = mc2.start.sync
    assert(f2.isDone)
    assert(f2.isSuccess)
    intercept[RemoteInvokeException] {
      mc2.blockingInvoker().connect("T2-ClientDuplicated")
    }
    mc1.stop.sync
    mc2.blockingInvoker().connect("T2-ClientDuplicated")
    mc2.stop
  }

  test("Test invoke and return Object") {
    val oc = MessageClient.newClient("localhost", port, "T3-Benz", new Object {
      def func = new Object() {
        def change() = "Haha"
      }
    })
    val checker = MessageClient.newClient("localhost", port, "T3-Checher")
    val getter = checker.blockingInvoker("T3-Benz")
    val result = getter.func.asInstanceOf[BlockingRemoteObject]
    assert(result.asInstanceOf[RemoteObject].remoteName == "T3-Benz")
    assert(result.getClass.getName == "com.hydra.io.BlockingRemoteObject")
    assert(result.change == "Haha")
    checker.stop
    val checker2 = MessageClient.newClient("localhost", port, "T3-Checher2", new Object {
      def rGet = {
        val checker3 = MessageClient.newClient("localhost", port, "T3-Checher3")
        val getter = checker3.blockingInvoker("T3-Benz")
        val r = getter.func
        checker3.stop
        r
      }
    })
    val checker4 = MessageClient.newClient("localhost", port, "T3-Checher4")
    val getter2 = checker4.blockingInvoker("T3-Checher2")
    assert(getter2.rGet.asInstanceOf[RemoteObject].remoteName == "T3-Benz")
    oc.stop
    checker2.stop
    checker4.stop
  }

  test("Test Finilize of Remote Objects") {
    val oc = MessageClient.newClient("localhost", port, "T4-Benz", new Object {
      def func = new Object() {
        def change() = "Haha"

        override def finalize {
        }
      }
    })
    val checker = MessageClient.newClient("localhost", port, "T4-Checher")
    val getter = checker.blockingInvoker("T4-Benz")
    val result = getter.func.asInstanceOf[BlockingRemoteObject]
    assert(result.asInstanceOf[RemoteObject].remoteName == "T4-Benz")
    assert(result.getClass.getName == "com.hydra.io.BlockingRemoteObject")
    assert(result.change == "Haha")
    checker.stop
    oc.stop
  }

  test("Test Session Listening") {
    val lc = MessageClient.newClient("localhost", port, "T5-Monitor")
    val latch = new CountDownLatch(2)
    lc.addSessionListener(new SessionListener {
      def sessionConnected(session: String) {
        latch.countDown
      }

      def sessionDisconnected(session: String) {
        latch.countDown
      }
    })
    val rc = MessageClient.newClient("localhost", port, "T5-Rabit")
    rc.stop.sync
    latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
    lc.stop
  }

  test("Test Session invoke in Session") {
    class PXITDCHandle {
      var session: MessageClient = null

      def begin() {
        val storage = session.blockingInvoker("Storage")
        storage.ask()
      }
    }
    class StorageHandle {
      def ask() {}
    }
    val sto = MessageClient.newClient("localhost", port, "Storage", new StorageHandle)
    val pxiTDCHandler = new PXITDCHandle
    val s1 = MessageClient.newClient("localhost", port, "PXITDC", pxiTDCHandler)
    pxiTDCHandler.session = s1
    val slocal = MessageClient.newClient("localhost", port, "local")
    val invoker = slocal.blockingInvoker("PXITDC", 2 second)
    invoker.begin()
    s1.stop
    sto.stop
    slocal.stop
  }

  test("Test invoke other client with anonymous client") {
    class Target {
      def v8 = "V8 great!"

      def v9 = throw new IllegalArgumentException("V9 not good.")

      def v10 = throw new IOException("V10 have problems.")

      def v(i: Int, b: Boolean) = "OK"
    }
    val mc1 = new MessageClient("T1-Benz", "localhost", port, new Target)
    val f1 = mc1.start.sync
    assert(f1.isDone)
    assert(f1.isSuccess)
    mc1.blockingInvoker().connect("T1-Benz")
    val checker1 = MessageClient.newClient("localhost", port)
    val checker2 = MessageClient.newClient("localhost", port)
    val benzChecker1 = checker1.blockingInvoker("T1-Benz")
    val v8r1 = benzChecker1.v8
    assert(v8r1 == "V8 great!")
    intercept[RemoteInvokeException] {
      val v9r = benzChecker1.v9
    }
    intercept[RemoteInvokeException] {
      val v10r = benzChecker1.v10
    }
    assert(benzChecker1.v(1, false) == "OK")
    intercept[RemoteInvokeException] {
      benzChecker1.v(false, false)
    }
    val benzChecker2 = checker1.blockingInvoker("T1-Benz")
    val v8r2 = benzChecker2.v8
    assert(v8r2 == "V8 great!")
    intercept[RemoteInvokeException] {
      val v9r = benzChecker2.v9
    }
    intercept[RemoteInvokeException] {
      val v10r = benzChecker2.v10
    }
    assert(benzChecker2.v(1, false) == "OK")
    intercept[RemoteInvokeException] {
      benzChecker2.v(false, false)
    }
    mc1.stop.await
    checker1.stop.await
    checker2.stop.await
  }


  test("[JOSN]Test configuration load.") {
    assert(MessageTransport.Configuration.getProperty("messageserver.port", "20102") == "20102")
    //TODO: Test unchangable here.
  }

  test("[JOSN]Test server status.") {
    assert(channelFuture.isDone)
    assert(channelFuture.isSuccess)
  }

  test("[JOSN]Test connection.") {
    val mc = new MessageClient("", "localhost", port, None, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val f = mc.start
    f.await(1000)
    assert(f.isSuccess)
  }

  test("[JOSN]Test dynamic invoker.") {
    val client = new MessageClient("", "localhost", port, None, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val invoker = client.toMessageInvoker()
    val m1 = invoker.fun1(a = 1, 2, "3", b = null, c = Vector(1, 2, "3d"))
    assert(m1.messageType == Request)
    assert(m1.requestContent == ("fun1", 2 :: "3" :: Nil, Map("a" -> 1, "b" -> null, "c" -> Vector(1, 2, "3d"))))
    intercept[IllegalArgumentException] {
      val m1 = invoker.fun2(To = 1, 2, "3", b = null, c = Vector(1, 2, "3d"))
    }
    val invoker2 = client.toMessageInvoker("OnT")
    val m2 = invoker2.fun2
    assert(m2.messageType == Request)
    assert(m2.requestContent == ("fun2", Nil, Map()))
    assert(m2.to.get == "OnT")
    client.stop.await
  }

  test("[JOSN]Test remote invoke and future.") {
    val client1 = new MessageClient("", "localhost", port, None, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val f1 = client1.start
    f1.await(1000)
    assert(f1.isSuccess)
    val invoker1 = client1.asynchronousInvoker()
    val future1 = invoker1.co
    val latch1 = new CountDownLatch(2)
    var uC1: Any = None
    var uS1: Any = None
    var uF1: Any = None
    future1.onComplete { case u => uC1 = u; latch1.countDown }
    future1.onSuccess { case u => uS1 = u }
    future1.onFailure { case u => uF1 = u; latch1.countDown }
    assert(latch1.await(2, java.util.concurrent.TimeUnit.SECONDS))
    assert(uF1.getClass.getSimpleName == "RemoteInvokeException")
    assert(uF1.asInstanceOf[RemoteInvokeException].getMessage == "Method not found: co.")
    client1.stop.await
    val client2 = new MessageClient("", "wrongaddress", port, None, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val invoker2 = client2.asynchronousInvoker()
    val future2 = invoker2.co2
    val latch2 = new CountDownLatch(2)
    var uC2: Any = None
    var uS2: Any = None
    var uF2: Any = None
    future2.onComplete { case u => uC2 = u; latch2.countDown }
    future2.onSuccess { case u => uS2 = u }
    future2.onFailure { case u => uF2 = u; latch2.countDown }
    assert(latch2.await(5, java.util.concurrent.TimeUnit.SECONDS))
    assert(uS2 == None)
    assert(uF2.isInstanceOf[RuntimeException])
    client2.stop
  }

  test("[JOSN]Test register client.") {
    val mc1 = new MessageClient("TestClient1", "localhost", port, None, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val f1 = mc1.start
    f1.await(1000)
    assert(f1.isSuccess)
    val future1 = mc1.connect
    val r1 = Await.result[Any](future1, 1 second)
    assert(r1 == Unit)
    val mc2 = new MessageClient("TestClient2", "localhost", port, None, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val f2 = mc2.start
    f2.await(1000)
    assert(f2.isSuccess)
    val invoker2 = mc2.blockingInvoker()
    val r2 = invoker2.connect("TestClient2")
    assert(r2 == Unit)
    intercept[RemoteInvokeException] {
      try {
        val r3 = invoker2.connect("TestClient2")
      }
      catch {
        case e: NullPointerException => e.printStackTrace
        case e: Throwable => throw e
      }
    }
    mc1.stop
    mc2.stop
  }

  test("[JOSN]Test invoke other client") {
    class Target {
      def v8 = "V8 great!"

      def v9 = throw new IllegalArgumentException("V9 not good.")

      def v10 = throw new IOException("V10 have problems.")

      def v(i: Int, b: Boolean) = "OK"
    }
    val mc1 = new MessageClient("T1-Benz", "localhost", port, new Target, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val f1 = mc1.start.sync
    assert(f1.isDone)
    assert(f1.isSuccess)
    mc1.blockingInvoker().connect("T1-Benz")
    val checker = MessageClient.newClient("localhost", port, "T1-Checher", protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val benzChecker = checker.blockingInvoker("T1-Benz")
    val v8r = benzChecker.v8
    assert(v8r == "V8 great!")
    intercept[RemoteInvokeException] {
      val v9r = benzChecker.v9
    }
    intercept[RemoteInvokeException] {
      val v10r = benzChecker.v10
    }
    assert(benzChecker.v(1, false) == "OK")
    intercept[RemoteInvokeException] {
      benzChecker.v(false, false)
    }
    mc1.stop.await
    checker.stop.await
  }

  test("[JOSN]Test client name duplicated") {
    val mc1 = new MessageClient("T2-ClientDuplicated", "localhost", port, None, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val f1 = mc1.start.sync
    assert(f1.isDone)
    assert(f1.isSuccess)
    mc1.blockingInvoker().connect("T2-ClientDuplicated")
    val mc2 = new MessageClient("T2-ClientDuplicated", "localhost", port, None, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val f2 = mc2.start.sync
    assert(f2.isDone)
    assert(f2.isSuccess)
    intercept[RemoteInvokeException] {
      mc2.blockingInvoker().connect("T2-ClientDuplicated")
    }
    mc1.stop.sync
    mc2.blockingInvoker().connect("T2-ClientDuplicated")
    mc2.stop
  }

  test("[JOSN]Test Session Listening") {
    val lc = MessageClient.newClient("localhost", port, "T5-Monitor", protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val latch = new CountDownLatch(2)
    lc.addSessionListener(new SessionListener {
      def sessionConnected(session: String) {
        latch.countDown
      }

      def sessionDisconnected(session: String) {
        latch.countDown
      }
    })
    val rc = MessageClient.newClient("localhost", port, "T5-Rabit", protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    rc.stop.sync
    latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
    lc.stop
  }

  test("[JOSN]Test Session invoke in Sessoin") {
    class PXITDCHandle {
      var session: MessageClient = null

      def begin() {
        val storage = session.blockingInvoker("Storage")
        storage.ask()
      }
    }
    class StorageHandle {
      def ask() {}
    }
    val sto = MessageClient.newClient("localhost", port, "Storage", new StorageHandle, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val pxiTDCHandler = new PXITDCHandle
    val s1 = MessageClient.newClient("localhost", port, "PXITDC", pxiTDCHandler, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    pxiTDCHandler.session = s1
    val slocal = MessageClient.newClient("localhost", port, "local", protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val invoker = slocal.blockingInvoker("PXITDC", 2 second)
    invoker.begin()
    s1.stop
    sto.stop
    slocal.stop
  }

  test("[JOSN]Test invoke other client with int and double problem") {
    class Target {
      def v(i: Int, d: Double) = "OK"
    }
    val mc1 = new MessageClient("T1-Benz", "localhost", port, new Target, protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val f1 = mc1.start.sync
    assert(f1.isDone)
    assert(f1.isSuccess)
    mc1.blockingInvoker().connect("T1-Benz")
    val checker = MessageClient.newClient("localhost", port, "T1-Checher", protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val benzChecker = checker.blockingInvoker("T1-Benz")
    val v8r = benzChecker.v(1, 11.0)
    mc1.stop.await
    checker.stop.await
  }

  test("[JOSN]Test invoke mix of JSON and MSGPACK") {
    class Target {
      def v(i: Int, d: Double) = "OK"
    }
    val mc1 = new MessageClient("T1-Benz", "localhost", port, new Target, protocol = MessageEncodingProtocol.PROTOCOL_MSGPACK)
    val f1 = mc1.start.sync
    assert(f1.isDone)
    assert(f1.isSuccess)
    mc1.blockingInvoker().connect("T1-Benz")
    val checker = MessageClient.newClient("localhost", port, "T1-Checher", protocol = MessageEncodingProtocol.PROTOCOL_JSON)
    val benzChecker = checker.blockingInvoker("T1-Benz")
    val v8r = benzChecker.v(1, 11.0)
    mc1.stop.await
    checker.stop.await
  }

  test("[JSON]Test defined length message.") {
    class Obj {
      def func() = "Answer"
    }
    val mc1 = new MessageClient("TestClient1", "localhost", port, new Obj(), protocol = MessageEncodingProtocol.PROTOCOL_JSON_PREFIX)
    val f1 = mc1.start
    f1.await(1000)
    assert(f1.isSuccess)
    val future1 = mc1.connect
    val r1 = Await.result[Any](future1, 1 second)
    assert(r1 == Unit)
    val mc2 = new MessageClient("TestClient2", "localhost", port, None)
    val f2 = mc2.start
    f2.await(1000)
    assert(f2.isSuccess)
    val invoker2 = mc2.blockingInvoker()
    val r2 = invoker2.connect("TestClient2")
    assert(r2 == Unit)
    assert(mc2.blockingInvoker("TestClient1").func() == "Answer")
    mc1.stop
    mc2.stop
  }


  test("Test kill client.") {
    class Obj(answer:String) {
      def func() = answer
    }
    val mc1 = new MessageClient("TestClient1", "localhost", port, new Obj("Answer"))
    val f1 = mc1.start
    f1.await(1000)
    assert(f1.isSuccess)
    val future1 = mc1.connect
    val r1 = Await.result[Any](future1, 1 second)
    assert(r1 == Unit)
    val mc2 = new MessageClient("TestClient2", "localhost", port, None)
    val f2 = mc2.start
    f2.await(1000)
    assert(f2.isSuccess)
    val invoker2 = mc2.blockingInvoker()
    val r2 = invoker2.connect("TestClient2")
    assert(r2 == Unit)
    assert(mc2.blockingInvoker("TestClient1").func() == "Answer")
    intercept[RemoteInvokeException] {
      invoker2.kick("TestClientNotExist")
    }
    invoker2.kick("TestClient1")
    val mc3 = MessageClient.newClient("localhost", port, "TestClient1", new Obj("Answer Again"))
    assert(mc2.blockingInvoker("TestClient1").func() == "Answer Again")
    mc1.stop
    mc2.stop
    mc3.stop
  }

}

