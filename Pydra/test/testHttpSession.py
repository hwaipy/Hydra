__author__ = 'Hwaipy'

import sys
import unittest
from Pydra import Message, ProtocolException, HttpSession
import socket
import threading
import time


class MessageTransportTest(unittest.TestCase):
    url = 'http://localhost:9000/hydra/message'

    @classmethod
    def setUpClass(cls):
        pass

    def setUp(self):
        pass

    def testConnectionOfSession(self):
        mc = HttpSession(self.url, None)
        mc.start()
        mc.stop()

    def testDynamicInvoker(self):
        client = HttpSession(self.url, None)
        invoker = client.messageInvoker()
        m1 = invoker.fun1(1, 2, "3", b=None, c=[1, 2, "3d"])
        self.assertEqual(m1.messageType(), Message.Type.Request)
        self.assertEqual(m1.requestContent(), ("fun1", [1, 2, "3"], {"b": None, "c": [1, 2, "3d"]}))
        self.assertRaises(ProtocolException, lambda: invoker.fun2(2, "3", b=None, c=[1, 2, "3d"], To=1))
        invoker2 = client.messageInvoker("OnT")
        m2 = invoker2.fun2()
        self.assertEqual(m2.messageType(), Message.Type.Request)
        self.assertEqual(m2.requestContent(), ("fun2", [], {}))
        self.assertEqual(m2.getTo(), "OnT")
        client.stop()

    def testRemoteInvokeAndAsync(self):
        client1 = HttpSession(self.url)
        invoker1 = client1.asynchronousInvoker()
        future1 = invoker1.co()
        latch1 = threading.Semaphore(0)
        future1.onComplete(lambda: latch1.release())
        latch1.acquire()
        self.assertTrue(future1.isDone())
        self.assertFalse(future1.isSuccess())
        self.assertEqual(future1.exception().description, "Method not found: co.")
        client1.stop()

    def testInvokeOtherClient(self):
        class Target:
            def v8(self): return "V8 great!"

            def v9(self): raise ProtocolException("V9 not good.")

            def v10(self): raise IOError("V10 have problems.")

            def v(self, i, b): return "OK"

        serviceName = 'T1_Benz{}'.format(time.time())
        mc1 = HttpSession(self.url, Target(), serviceName)
        mc1.start()
        checker = HttpSession.create(self.url)
        benzChecker = checker.blockingInvoker(serviceName, 10)
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
        mc1 = HttpSession(self.url, None, serviceName="T2-ClientDuplicated")
        mc1.start()
        mc2 = HttpSession(self.url, None, serviceName="T2-ClientDuplicated")
        # self.assertRaises(ProtocolException, lambda: mc2.start())
        mc1.stop()
        time.sleep(0.5)
        mc2.blockingInvoker().registerAsService(u"T2-ClientDuplicated")
        mc2.stop()

    def testInvokeInDynamicStyle(self):
        class Target:
            def v8(self): return "V8 great!"

            def v9(self): raise ProtocolException("V9 not good.")

            def v10(self): raise IOError("V10 have problems.")

            def v(self, i, b): return "OK"

        mc1 = HttpSession.create(self.url, Target(), 'T1_Benz')
        checker = HttpSession.create(self.url)
        v8r = checker.T1_Benz.v8()
        self.assertEqual(v8r, "V8 great!")
        try:
            checker.T1_Benz.v9()
            self.assertTrue(False)
        except ProtocolException as e:
            self.assertEqual(e.__str__(), "V9 not good.")
        try:
            checker.T1_Benz.v10()
            self.assertTrue(False)
        except ProtocolException as e:
            self.assertEqual(e.__str__(), "V10 have problems.")
        self.assertEqual(checker.T1_Benz.v(1, False), "OK")
        try:
            checker.T1_Benz.v11()
            self.assertTrue(False)
        except ProtocolException as e:
            self.assertEqual(e.__str__(), "InvokeError: Command v11 not found.")
        mc1.stop()
        checker.stop()

    def tearDown(self):
        pass

    @classmethod
    def tearDownClass(cls):
        pass


if __name__ == '__main__':
    unittest.main()
