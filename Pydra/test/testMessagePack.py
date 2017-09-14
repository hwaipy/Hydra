__author__ = 'Hwaipy'

import sys
import unittest
from Pydra import Message, ProtocolException
import msgpack
from random import Random


class MessagePackTest(unittest.TestCase):
    mapIn = {
        "keyString": "value1",
        "keyInt": 123,
        "keyLong": (sys.maxsize + 100),
        "keyBigInteger": 2 ** 64 - 1,
        "keyBooleanFalse": False,
        "KeyBooleanTrue": True,
        "keyByteArray": b"\x01\x02\x02\x02\x02\x01\x01\x02\x02\x01\x01\x04\x05\x00\x04\x04\xFF",
        "keyIntArray": [3, 526255, 1321, 4, -1],
        "keyNull": None,
        "keyDouble": 1.242,
        "keyDouble2": -12.2323e-100}
    map = {"keyMap": mapIn}

    @classmethod
    def setUpClass(cls):
        pass

    def setUp(self):
        pass

    def testFeedOverflow(self):
        unpacker = msgpack.Unpacker(encoding='utf-8')
        bytes = Message(MessagePackTest.map).pack()
        for i in range(1000000):
            unpacker.feed(bytes)

    def testOverDeepth(self):
        map = {"a": "b"}
        for i in range(200):
            map = {'a': map}
        Message(map).pack()

    def testMapPackAndUnpack(self):
        bytes = Message(MessagePackTest.map).pack()
        unpacker = msgpack.Unpacker(encoding='utf-8')
        unpacker.feed(bytes)
        m1 = unpacker.__next__()
        self.assertRaises(StopIteration, unpacker.__next__)
        self.assertEqual(m1, MessagePackTest.map)

    def testMultiUnpack(self):
        multi = 100
        bytes = b''
        for i in range(multi):
            bytes += Message(MessagePackTest.map).pack()
        unpacker = msgpack.Unpacker(encoding='utf-8')
        unpacker.feed(bytes)
        for i in range(multi):
            self.assertEqual(unpacker.__next__(), MessagePackTest.map)
        self.assertRaises(StopIteration, unpacker.__next__)

    def testPartialUnpackByteByByte(self):
        multi = 10
        bytes = Message(MessagePackTest.map).pack()
        unpacker = msgpack.Unpacker(encoding='utf-8')
        for j in range(multi):
            for i in range(len(bytes) - 1):
                unpacker.feed(bytes[i:i + 1])
                self.assertRaises(StopIteration, unpacker.__next__)
            unpacker.feed(bytes[-1:])
            self.assertEqual(unpacker.__next__(), MessagePackTest.map)

    def testPartialUnpackBlockByBlock(self):
        unitSize = len(Message(MessagePackTest.map).pack())
        limit = unitSize * 3
        random = Random()
        multi = 10
        blockSizesA = [0] * (multi - 1)
        for i in range(len(blockSizesA)):
            blockSizesA[i] = random.randint(0, limit)
        sumA = sum(blockSizesA)
        lastSize = unitSize - (sumA % unitSize)
        blockSizes = blockSizesA + [lastSize]
        totalSize = sumA + lastSize
        self.assertEqual(totalSize % unitSize, 0)
        bytes = b''
        for i in range(totalSize // unitSize):
            bytes += Message(MessagePackTest.map).pack()
        self.assertEqual(len(bytes), totalSize)
        unpacker = msgpack.Unpacker(encoding='utf-8')
        generated = 0
        sumD = 0
        for size in blockSizes:
            unpacker.feed(bytes[sumD:sumD + size])
            sumD += size
            newGenerated = sumD // unitSize
            while newGenerated > generated:
                self.assertEqual(unpacker.__next__(), MessagePackTest.map)
                generated += 1
            self.assertRaises(StopIteration, unpacker.__next__)
        self.assertEqual(generated, totalSize // unitSize)

    def testException(self):
        unpacker = msgpack.Unpacker(encoding='utf-8')
        bytes = Message(MessagePackTest.map).pack()
        unpacker.feed(bytes)
        unpacker.feed(bytes[1:-1])
        unpacker.feed(bytes)
        self.assertEqual(unpacker.__next__(), MessagePackTest.map)
        self.assertNotEqual(unpacker.__next__(), MessagePackTest.map)

    def tearDown(self):
        pass

    @classmethod
    def tearDownClass(cls):
        pass


if __name__ == '__main__':
    unittest.main()
