package com.hydra.core

import java.io.IOException

import org.scalatest._
import com.hydra.core.MessageType._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class MessageSessionTest extends FunSuite with BeforeAndAfter with BeforeAndAfterAll {
  val manager = new MessageSessionManager
  val service = new StatelessMessageService(manager)

  override def beforeAll() {
  }

  override def afterAll(): Unit = {
    service.stop
  }

  before {
  }

  test("Test session creation and service registration") {
    val initialSize = manager.sessionsCount
    val session1 = manager.newSession()
    assert(manager.sessionsCount == initialSize + 1)
    val session2 = manager.newSession()
    session1.registerAsServcie("S1")
    session2.registerAsServcie("S2")
    assert(session2.id - session1.id == 1)
    assert(manager.sessionsCount == initialSize + 2)
    session2.close
    assert(manager.sessionsCount == initialSize + 1)
    val session3 = manager.newSession()
    val session4 = manager.newService("S4")
    assert(manager.sessionsCount == initialSize + 3)
    assert(session3.id - session1.id == 2)
    assert(session4.id - session1.id == 3)
    assert(session4.getServiceName.get == "S4")
    assert(manager.getSession(session3.id).get == session3)
    assert(manager.getService(session2.getServiceName.get) == None)
    assert(manager.getSession(session2.id) == None)
    assert(manager.getSessions.size == manager.sessionsCount)
    session1.close
    session2.close
    session3.close
    session4.close
    assert(manager.sessionsCount == initialSize)
    assert(manager.getService(session4.getServiceName.get) == None)
    assert(manager.getSession(session4.id) == None)
    assert(manager.getSessions.size == manager.sessionsCount)
    assert(manager.sessionsCount == 0)
    assert(manager.servicesCount == 0)
  }

  test("Test service name duplicated") {
    val s1 = manager.newService("T2-ClientDuplicated")
    intercept[MessageException] {
      manager.newService("T2-ClientDuplicated")
    }
    s1.close
    assert(manager.sessionsCount == 0)
    assert(manager.servicesCount == 0)
  }

  test("Test dynamic invoker.") {
    val client = MessageClient.create(new LocalStatelessMessageChannel(service))
    val invoker = client.messageInvoker()
    val m1 = invoker.fun1(a = 1, 2, "3", b = null, c = Vector(1, 2, "3d"))
    assert(m1.messageType == Request)
    assert(m1.requestContent == ("fun1", 2 :: "3" :: Nil, Map("a" -> 1, "b" -> null, "c" -> Vector(1, 2, "3d"))))
    intercept[IllegalArgumentException] {
      val m1 = invoker.fun2(To = 1, 2, "3", b = null, c = Vector(1, 2, "3d"))
    }
    val invoker2 = client.messageInvoker("OnT")
    val m2 = invoker2.fun2
    assert(m2.messageType == Request)
    assert(m2.requestContent == ("fun2", Nil, Map()))
    assert(m2.to.get == "OnT")
  }

  test("Test remote invoke and future.") {
    class Target {
      def v8 = "V8 great!"

      def v9 = throw new IllegalArgumentException("V9 not good.")

      def v10 = throw new IOException("V10 have problems.")

      def v(i: Int, b: Boolean) = "OK"
    }
    val provider = MessageClient.create(new LocalStatelessMessageChannel(service), "T1_Benz", new Target)
    val checker = MessageClient.create(new LocalStatelessMessageChannel(service))
    val v8r = checker.T1_Benz.v8
    assert(v8r == "V8 great!")
    intercept[MessageException] {
      val v9r = checker.T1_Benz.v9
    }
    intercept[MessageException] {
      val v10r = checker.T1_Benz.v10
    }
    assert(checker.T1_Benz.v(1, false) == "OK")
    intercept[MessageException] {
      checker.T1_Benz.v(false, false)
    }
    provider.close
    checker.close
  }

  test("Test Session invoke in Session") {
    class PXITDCHandle {
      var session: MessageClient = null

      def begin() = {
        val storage = session.blockingInvoker("Storage")
        val ask = storage.ask()
        ask
      }
    }
    class StorageHandle {
      def ask() = "YES"
    }
    val sto = MessageClient.create(new LocalStatelessMessageChannel(service), "Storage", new StorageHandle)
    val pxiTDCHandler = new PXITDCHandle
    val s1 = MessageClient.create(new LocalStatelessMessageChannel(service), "PXITDC", pxiTDCHandler)
    pxiTDCHandler.session = s1
    val slocal = MessageClient.create(new LocalStatelessMessageChannel(service))
    val invoker = slocal.blockingInvoker("PXITDC", 10 second)
    assert(invoker.begin() == "YES")
    s1.close
    sto.close
    slocal.close
  }

  test("Test service name duplicated.") {
    val mc1 = MessageClient.create(new LocalStatelessMessageChannel(service), "T1_Benz", new Object())
    intercept[MessageException] {
      val mc2 = MessageClient.create(new LocalStatelessMessageChannel(service), "T1_Benz", new Object())
    }
    mc1.close
  }

  test("Test invoke other client with anonymous client") {
    class Target {
      def v8 = "V8 great!"

      def v9 = throw new IllegalArgumentException("V9 not good.")

      def v10 = throw new IOException("V10 have problems.")

      def v(i: Int, b: Boolean) = "OK"
    }
    val mc1 = MessageClient.create(new LocalStatelessMessageChannel(service), "T1_Benz", new Target)
    val checker1 = MessageClient.create(new LocalStatelessMessageChannel(service))
    val checker2 = MessageClient.create(new LocalStatelessMessageChannel(service))
    val v8r1 = checker1.T1_Benz.v8
    assert(v8r1 == "V8 great!")
    intercept[MessageException] {
      val v9r = checker1.T1_Benz.v9
    }
    intercept[MessageException] {
      val v10r = checker1.T1_Benz.v10
    }
    assert(checker1.T1_Benz.v(1, false) == "OK")
    intercept[MessageException] {
      checker1.T1_Benz.v(false, false)
    }
    val benzChecker2 = checker1.blockingInvoker("T1_Benz")
    val v8r2 = benzChecker2.v8
    assert(v8r2 == "V8 great!")
    intercept[MessageException] {
      val v9r = benzChecker2.v9
    }
    intercept[MessageException] {
      val v10r = benzChecker2.v10
    }
    assert(benzChecker2.v(1, false) == "OK")
    intercept[MessageException] {
      benzChecker2.v(false, false)
    }
    mc1.close
    checker1.close
    checker2.close
  }

  test("[JSON] Test dynamic invoker.") {
    val client = MessageClient.create(new LocalStatelessMessageChannel(service, "JSON"))
    val invoker = client.messageInvoker()
    val m1 = invoker.fun1(a = 1, 2, "3", b = null, c = Vector(1, 2, "3d"))
    assert(m1.messageType == Request)
    assert(m1.requestContent == ("fun1", 2 :: "3" :: Nil, Map("a" -> 1, "b" -> null, "c" -> Vector(1, 2, "3d"))))
    intercept[IllegalArgumentException] {
      val m1 = invoker.fun2(To = 1, 2, "3", b = null, c = Vector(1, 2, "3d"))
    }
    val invoker2 = client.messageInvoker("OnT")
    val m2 = invoker2.fun2
    assert(m2.messageType == Request)
    assert(m2.requestContent == ("fun2", Nil, Map()))
    assert(m2.to.get == "OnT")
  }

  test("[JSON] Test remote invoke and future.") {
    class Target {
      def v8 = "V8 great!"

      def v9 = throw new IllegalArgumentException("V9 not good.")

      def v10 = throw new IOException("V10 have problems.")

      def v(i: Int, b: Boolean) = "OK"
    }
    val provider = MessageClient.create(new LocalStatelessMessageChannel(service, "JSON"), "T1_Benz", new Target)
    val checker = MessageClient.create(new LocalStatelessMessageChannel(service, "JSON"))
    val v8r = checker.T1_Benz.v8
    assert(v8r == "V8 great!")
    intercept[MessageException] {
      val v9r = checker.T1_Benz.v9
    }
    intercept[MessageException] {
      val v10r = checker.T1_Benz.v10
    }
    assert(checker.T1_Benz.v(1, false) == "OK")
    intercept[MessageException] {
      checker.T1_Benz.v(false, false)
    }
    provider.close
    checker.close
  }

  test("[JSON] Test Session invoke in Session") {
    class PXITDCHandle {
      var session: MessageClient = null

      def begin() = {
        val storage = session.blockingInvoker("Storage")
        storage.ask()
      }
    }
    class StorageHandle {
      def ask() = "YES"
    }
    val sto = MessageClient.create(new LocalStatelessMessageChannel(service, "JSON"), "Storage", new StorageHandle)
    val pxiTDCHandler = new PXITDCHandle
    val s1 = MessageClient.create(new LocalStatelessMessageChannel(service, "JSON"), "PXITDC", pxiTDCHandler)
    pxiTDCHandler.session = s1
    val slocal = MessageClient.create(new LocalStatelessMessageChannel(service))
    val invoker = slocal.blockingInvoker("PXITDC", 10 second)
    assert(invoker.begin() == "YES")
    s1.close
    sto.close
    slocal.close
  }

  test("[JSON] Test service name duplicated.") {
    val mc1 = MessageClient.create(new LocalStatelessMessageChannel(service, "JSON"), "T1_Benz", new Object())
    intercept[MessageException] {
      val mc2 = MessageClient.create(new LocalStatelessMessageChannel(service, "JSON"), "T1_Benz", new Object())
    }
    mc1.close
  }

  test("[JSON] Test invoke other client with anonymous client") {
    class Target {
      def v8 = "V8 great!"

      def v9 = throw new IllegalArgumentException("V9 not good.")

      def v10 = throw new IOException("V10 have problems.")

      def v(i: Int, b: Boolean) = "OK"
    }
    val mc1 = MessageClient.create(new LocalStatelessMessageChannel(service, "JSON"), "T1_Benz", new Target)
    val checker1 = MessageClient.create(new LocalStatelessMessageChannel(service, "JSON"))
    val checker2 = MessageClient.create(new LocalStatelessMessageChannel(service, "JSON"))
    val v8r1 = checker1.T1_Benz.v8
    assert(v8r1 == "V8 great!")
    intercept[MessageException] {
      val v9r = checker1.T1_Benz.v9
    }
    intercept[MessageException] {
      val v10r = checker1.T1_Benz.v10
    }
    assert(checker1.T1_Benz.v(1, false) == "OK")
    intercept[MessageException] {
      checker1.T1_Benz.v(false, false)
    }
    val benzChecker2 = checker1.blockingInvoker("T1_Benz")
    val v8r2 = benzChecker2.v8
    assert(v8r2 == "V8 great!")
    intercept[MessageException] {
      val v9r = benzChecker2.v9
    }
    intercept[MessageException] {
      val v10r = benzChecker2.v10
    }
    assert(benzChecker2.v(1, false) == "OK")
    intercept[MessageException] {
      benzChecker2.v(false, false)
    }
    mc1.close
    checker1.close
    checker2.close
  }

  //  test("Test kill client.") {
  //    class Obj(answer: String) {
  //      def func() = answer
  //    }
  //    val mc1 = new MessageClient("TestClient1", "localhost", port, new Obj("Answer"))
  //    val f1 = mc1.start
  //    f1.await(1000)
  //    assert(f1.isSuccess)
  //    val future1 = mc1.connect
  //    val r1 = Await.result[Any](future1, 1 second)
  //    assert(r1 == Unit)
  //    val mc2 = new MessageClient("TestClient2", "localhost", port, None)
  //    val f2 = mc2.start
  //    f2.await(1000)
  //    assert(f2.isSuccess)
  //    val invoker2 = mc2.blockingInvoker()
  //    val r2 = invoker2.connect("TestClient2")
  //    assert(r2 == Unit)
  //    assert(mc2.blockingInvoker("TestClient1").func() == "Answer")
  //    intercept[RemoteInvokeException] {
  //      invoker2.kick("TestClientNotExist")
  //    }
  //    invoker2.kick("TestClient1")
  //    val mc3 = MessageClient.newClient("localhost", port, "TestClient1", new Obj("Answer Again"))
  //    assert(mc2.blockingInvoker("TestClient1").func() == "Answer Again")
  //    mc1.stop
  //    mc2.stop
  //    mc3.stop
  //  }
  //
  //  test("Test UDP broadcast.") {
  //    val messageExpected = "Hydra Server Test"
  //    val broadcastServer = new BroadcastServer(InetAddress.getByName("192.168.25.255"), 20100, messageExpected)
  //    broadcastServer.start
  //
  //    val serverSocket = new DatagramSocket(20100)
  //    val buffer = new Array[Byte](1000)
  //    val packet = new DatagramPacket(buffer, buffer.length)
  //    serverSocket.receive(packet)
  //    val message = new String(packet.getData.slice(0, packet.getLength), "UTF-8")
  //    assert(message == messageExpected)
  //    serverSocket.close()
  //    broadcastServer.stop
  //  }
}

