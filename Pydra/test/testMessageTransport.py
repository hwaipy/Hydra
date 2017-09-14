__author__ = 'Hwaipy'

import sys
import unittest
from Pydra import Message, ProtocolException, Session
import socket
import threading
import time


class MessageTransportTest(unittest.TestCase):
    port = 20102

    @classmethod
    def setUpClass(cls):
        pass

    def setUp(self):
        pass

    def testConnectionOfSession(self):
        mc = Session(("localhost", 10211), None)
        self.assertRaises(ConnectionRefusedError, mc.start)
        mc = Session(("localhost", MessageTransportTest.port), None)
        mc.start()
        mc.stop()

    def testDynamicInvoker(self):
        client = Session(("localhost", MessageTransportTest.port), None)
        invoker = client.toMessageInvoker()
        m1 = invoker.fun1(1, 2, "3", b=None, c=[1, 2, "3d"])
        self.assertEqual(m1.messageType(), Message.Type.Request)
        self.assertEqual(m1.requestContent(), ("fun1", [1, 2, "3"], {"b": None, "c": [1, 2, "3d"]}))
        self.assertRaises(ProtocolException, lambda: invoker.fun2(2, "3", b=None, c=[1, 2, "3d"], To=1))
        invoker2 = client.toMessageInvoker("OnT")
        m2 = invoker2.fun2()
        self.assertEqual(m2.messageType(), Message.Type.Request)
        self.assertEqual(m2.requestContent(), ("fun2", [], {}))
        self.assertEqual(m2.getTo(), "OnT")

    def testRemoteInvokeAndAsync(self):
        client1 = Session(("localhost", MessageTransportTest.port), None)
        f1 = client1.start()
        invoker1 = client1.asynchronousInvoker()
        future1 = invoker1.co()
        latch1 = threading.Semaphore(0)
        future1.onComplete(lambda: latch1.release())
        latch1.acquire()
        self.assertTrue(future1.isDone())
        self.assertFalse(future1.isSuccess())
        self.assertEqual(future1.exception().description, "Method not found: co.")
        client1.stop()

    def testRegisterClient(self):
        mc1 = Session(("localhost", MessageTransportTest.port), None)
        f1 = mc1.start()
        invoker1 = mc1.asynchronousInvoker()
        future1 = invoker1.connect("TestClient1")
        self.assertTrue(future1.await(1))
        self.assertTrue(future1.isDone())
        self.assertTrue(future1.isSuccess())
        self.assertEqual(future1.result(), None)
        mc1.stop()
        mc2 = Session(("localhost", MessageTransportTest.port), None)
        mc2.start()
        invoker2 = mc2.blockingInvoker()
        r2 = invoker2.connect("TestClient2")
        self.assertEqual(r2, None)
        self.assertRaises(ProtocolException, lambda: invoker2.connect('TestClient2'))
        mc2.stop()

    def testInvokeOtherClient(self):
        class Target:
            def v8(self): return "V8 great!"

            def v9(self): raise ProtocolException("V9 not good.")

            def v10(self): raise IOError("V10 have problems.")

            def v(self, i, b): return "OK"

        mc1 = Session(("localhost", MessageTransportTest.port), Target())
        mc1.start()
        mc1.blockingInvoker().connect("T1-Benz")
        checker = Session.newSession(("localhost", MessageTransportTest.port), None, "T1-Checher")
        benzChecker = checker.blockingInvoker("T1-Benz", 1)
        v8r = benzChecker.v8()
        self.assertEqual(v8r, "V8 great!")
        try:
            benzChecker.v9()
            self.assertTrue(False)
        except ProtocolException as e:
            self.assertEqual(e.__str__(), "V9 not good.")
        try:
            benzChecker.v10()
            self.assertTrue(False)
        except ProtocolException as e:
            self.assertEqual(e.__str__(), "V10 have problems.")
        self.assertEqual(benzChecker.v(1, False), "OK")
        try:
            benzChecker.v11()
            self.assertTrue(False)
        except ProtocolException as e:
            self.assertEqual(e.__str__(), "InvokeError: Command v11 not found.")
        mc1.stop()
        checker.stop()

    def testClientNameDuplicated(self):
        mc1 = Session(("localhost", MessageTransportTest.port), None)
        mc1.start()
        mc1.blockingInvoker().connect("T2-ClientDuplicated")
        mc2 = Session(("localhost", MessageTransportTest.port), None)
        mc2.start()
        self.assertRaises(ProtocolException, lambda: mc2.blockingInvoker().connect("T2-ClientDuplicated"))
        mc1.stop()
        time.sleep(0.5)
        mc2.blockingInvoker().connect("T2-ClientDuplicated")
        mc2.stop()

    def testInvokeAndReturnObject(self):
        class Target:
            class T:
                def change(self):
                    return 'Haha'

            def func(self):
                return Target.T()

        oc = Session.newSession(('localhost', MessageTransportTest.port), Target(), "T3-Benz")
        checker = Session.newSession(("localhost", MessageTransportTest.port), None, "T3-Checher")
        getter = checker.blockingInvoker("T3-Benz", 1)
        result = getter.func()
        self.assertEqual(result.change(), 'Haha')
        checker.stop()

        class Target2:
            def rGet(self):
                checker3 = Session.newSession(("localhost", MessageTransportTest.port), None, "T3-Checher3")
                r = checker3.blockingInvoker("T3-Benz").func()
                checker3.stop()
                return r

        checker2 = Session.newSession(("localhost", MessageTransportTest.port), Target2(), "T3-Checher2")
        checker4 = Session.newSession(("localhost", MessageTransportTest.port), None, "T3-Checher4")
        getter2 = checker4.blockingInvoker("T3-Checher2")
        self.assertEqual(getter2.rGet().name, 'T3-Benz')
        checker2.stop()
        checker4.stop()
        oc.stop()

    '''
  test("Test Session Listening") {
    val lc = MessageClient.newClient("localhost", port, "T5-Monitor")
    val latch = new CountDownLatch(2)
    lc.addSessionListener(new SessionListener {
      def sessionConnected(session: String) { latch.countDown }
      def sessionDisconnected(session: String) { latch.countDown }
    })
    val rc = MessageClient.newClient("localhost", port, "T5-Rabit")
    rc.stop.sync
    latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
    lc.stop
  }
'''

    def tearDown(self):
        pass

    @classmethod
    def tearDownClass(cls):
        pass


if __name__ == '__main__':
    unittest.main()
