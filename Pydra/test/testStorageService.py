__author__ = 'Hwaipy'

import sys
import unittest
from Pydra import Message, ProtocolException, Session
import socket
import threading
import time
from Services.Storage import StorageService, HBTFileElement


class StorageServiceTest(unittest.TestCase):
    port = 20102
    testSpacePath = "/pydratest/testservicespace/"

    @classmethod
    def setUpClass(cls):
        pass

    def setUp(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        storage = mc.blockingInvoker("StorageService")
        if storage.exists("", StorageServiceTest.testSpacePath):
            storage.delete("", StorageServiceTest.testSpacePath)
        storage.createDirectory("", StorageServiceTest.testSpacePath)
        for i in range(1, 6):
            storage.createDirectory("", "{}a{}".format(StorageServiceTest.testSpacePath, i))
        storage.createFile("", "{}_A1".format(StorageServiceTest.testSpacePath))
        storage.createFile("", "{}_A2".format(StorageServiceTest.testSpacePath))
        storage.write("", "{}_A1".format(StorageServiceTest.testSpacePath), b"1234567890abcdefghijklmnopqrstuvwxyz", 0)
        storage.write("", "{}_A2".format(StorageServiceTest.testSpacePath), b"0123456789", 0)
        mc.stop()

    def testList(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        a = service.listElements(StorageServiceTest.testSpacePath)
        self.assertEqual(a, ["_A1", "_A2", "a1", "a2", "a3", "a4", "a5"])
        mc.stop()

    def testMetaData(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        self.assertEqual(service.metaData("{}a1".format(StorageServiceTest.testSpacePath)),
                         {"Name": "a1", "Path": "{}a1".format(StorageServiceTest.testSpacePath), "Type": "Collection"})
        mc.stop()

    def testListMetaData(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        elements = service.listElements(StorageServiceTest.testSpacePath, True)
        expected = [
            ["_A1", "_A1", "Content"],
            ["_A2", "_A2", "Content"],
            ["a1", "a1", "Collection"],
            ["a2", "a2", "Collection"],
            ["a3", "a3", "Collection"],
            ["a4", "a4", "Collection"],
            ["a5", "a5", "Collection"]]
        self.assertEqual(len(elements), len(expected))
        for i in range(0, len(expected)):
            exp = expected[i]
            res = elements[i]
            self.assertEqual(res.get("Name"), exp[0])
            self.assertEqual(res.get("Path"), StorageServiceTest.testSpacePath + exp[1])
            self.assertEqual(res.get("Type"), exp[2])
        self.assertEqual(elements[0].get("Size"), 36)
        self.assertEqual(elements[1].get("Size"), 10)
        mc.stop()

    def testNote(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        self.assertEqual(service.readNote(StorageServiceTest.testSpacePath), "")
        service.writeNote(StorageServiceTest.testSpacePath, "Test Note")
        self.assertEqual(service.readNote(StorageServiceTest.testSpacePath), "Test Note")
        mc.stop()

    def testRead(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        self.assertEqual(service.read("{}_A1".format(StorageServiceTest.testSpacePath), 1, 10), b"234567890a")
        self.assertEqual(service.read("{}_A1".format(StorageServiceTest.testSpacePath), 30, 6), b"uvwxyz")
        self.assertEqual(service.readAll("{}_A2".format(StorageServiceTest.testSpacePath)), b'0123456789')
        mc.stop()

    def testAppend(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        service.append("{}_A1".format(StorageServiceTest.testSpacePath), b"ABCDE")
        self.assertEqual(service.metaData("{}_A1".format(StorageServiceTest.testSpacePath)),
                         {"Name": "_A1", "Path": "{}_A1".format(StorageServiceTest.testSpacePath), "Type": "Content",
                          "Size": 41})
        self.assertEqual(service.read("{}_A1".format(StorageServiceTest.testSpacePath), 35, 6), b"zABCDE")
        mc.stop()

    def testWrite(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        service.write("{}_A1".format(StorageServiceTest.testSpacePath), b"ABCDE", 10)
        self.assertEqual(service.metaData("{}_A1".format(StorageServiceTest.testSpacePath)),
                         {"Name": "_A1", "Path": "{}_A1".format(StorageServiceTest.testSpacePath), "Type": "Content",
                          "Size": 36})
        self.assertEqual(service.readAsString("{}_A1".format(StorageServiceTest.testSpacePath), 0, 16),
                         "1234567890ABCDEf")
        self.assertEqual(service.readAllAsString("{}_A1".format(StorageServiceTest.testSpacePath)),
                         "1234567890ABCDEfghijklmnopqrstuvwxyz")
        service.write("{}_A1".format(StorageServiceTest.testSpacePath), b"defghi", 39)
        self.assertEqual(service.metaData("{}_A1".format(StorageServiceTest.testSpacePath)),
                         {"Name": "_A1", "Path": "{}_A1".format(StorageServiceTest.testSpacePath), "Type": "Content",
                          "Size": 45})
        self.assertEqual(service.readAsString("{}_A1".format(StorageServiceTest.testSpacePath), 35, 10),
                         "z\0\0\0defghi")
        self.assertEqual(service.readAsString("{}_A1".format(StorageServiceTest.testSpacePath), 0, 16),
                         "1234567890ABCDEf")
        mc.stop()

    def testDelete(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        self.assertEqual(
            service.listElements(StorageServiceTest.testSpacePath), ["_A1", "_A2", "a1", "a2", "a3", "a4", "a5"])
        service.delete("{}a1".format(StorageServiceTest.testSpacePath))
        self.assertEqual(service.listElements(StorageServiceTest.testSpacePath), ["_A1", "_A2", "a2", "a3", "a4", "a5"])
        service.delete("{}_A2".format(StorageServiceTest.testSpacePath))
        self.assertEqual(service.listElements(StorageServiceTest.testSpacePath), ["_A1", "a2", "a3", "a4", "a5"])
        mc.stop()

    def testCreateFile(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        service.createFile("{}NewFile".format(StorageServiceTest.testSpacePath))
        service.createFile("{}a2/NewFile".format(StorageServiceTest.testSpacePath))
        self.assertEqual(service.listElements(StorageServiceTest.testSpacePath),
                         ["NewFile", "_A1", "_A2", "a1", "a2", "a3", "a4", "a5"])
        self.assertEqual(service.listElements("{}a2".format(StorageServiceTest.testSpacePath)), ["NewFile"])
        mc.stop()

    def testCreateDirectory(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        service.createDirectory("{}a2/NewDir".format(StorageServiceTest.testSpacePath))
        self.assertEqual(service.listElements("{}a2".format(StorageServiceTest.testSpacePath)), ["NewDir"])
        mc.stop()

    def testElementList(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        element = service.getElement(StorageServiceTest.testSpacePath)
        a = element.listElements()
        self.assertEqual(a, ["_A1", "_A2", "a1", "a2", "a3", "a4", "a5"])
        mc.stop()

    def testElementMetaData(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        testRoot = StorageService(mc).getElement(StorageServiceTest.testSpacePath)
        self.assertEqual(testRoot.resolve("a1").metaData(),
                         {"Name": "a1", "Path": "{}a1".format(StorageServiceTest.testSpacePath),
                          "Type": "Collection"})
        mc.stop()

    def testElementListMetaData(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        testRoot = StorageService(mc).getElement(StorageServiceTest.testSpacePath)
        elements = testRoot.listElements(True)
        expected = [
            ["_A1", "_A1", "Content"],
            ["_A2", "_A2", "Content"],
            ["a1", "a1", "Collection"],
            ["a2", "a2", "Collection"],
            ["a3", "a3", "Collection"],
            ["a4", "a4", "Collection"],
            ["a5", "a5", "Collection"]]
        self.assertEqual(len(elements), len(expected))
        for i in range(0, len(expected)):
            exp = expected[i]
            res = elements[i]
            self.assertEqual(res.get("Name"), exp[0])
            self.assertEqual(res.get("Path"), StorageServiceTest.testSpacePath + exp[1])
            self.assertEqual(res.get("Type"), exp[2])
        self.assertEqual(elements[0].get("Size"), 36)
        self.assertEqual(elements[1].get("Size"), 10)
        mc.stop()

    def testElementNote(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        testRoot = StorageService(mc).getElement(StorageServiceTest.testSpacePath)
        self.assertEqual(testRoot.readNote(), "")
        testRoot.writeNote("Test Note")
        self.assertEqual(testRoot.readNote(), "Test Note")
        mc.stop()

    def testRead(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        testRoot = StorageService(mc).getElement(StorageServiceTest.testSpacePath)
        self.assertEqual(testRoot.resolve('/_A1').read(1, 10), b"234567890a")
        self.assertEqual(testRoot.resolve('/////_A1').read(30, 6), b"uvwxyz")
        self.assertEqual(testRoot.resolve('_A2').readAll(), b'0123456789')
        mc.stop()

    def testAppend(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        service.append("{}_A1".format(StorageServiceTest.testSpacePath), b"ABCDE")
        self.assertEqual(service.metaData("{}_A1".format(StorageServiceTest.testSpacePath)),
                         {"Name": "_A1", "Path": "{}_A1".format(StorageServiceTest.testSpacePath),
                          "Type": "Content",
                          "Size": 41})
        self.assertEqual(service.read("{}_A1".format(StorageServiceTest.testSpacePath), 35, 6), b"zABCDE")
        mc.stop()

    def testWrite(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        testRoot = StorageService(mc).getElement(StorageServiceTest.testSpacePath)
        A1 = testRoot.resolve('_A1')
        A1.write(b"ABCDE", 10)
        self.assertEqual(A1.metaData(), {"Name": "_A1", "Path": "{}_A1".format(StorageServiceTest.testSpacePath),
                                         "Type": "Content", "Size": 36})
        self.assertEqual(A1.readAsString(0, 16), "1234567890ABCDEf")
        self.assertEqual(A1.readAllAsString(), "1234567890ABCDEfghijklmnopqrstuvwxyz")
        A1.write(b"defghi", 39)
        self.assertEqual(A1.metaData(),
                         {"Name": "_A1", "Path": "{}_A1".format(StorageServiceTest.testSpacePath), "Type": "Content",
                          "Size": 45})
        self.assertEqual(A1.readAsString(35, 10), "z\0\0\0defghi")
        self.assertEqual(A1.readAsString(0, 16), "1234567890ABCDEf")
        mc.stop()

    def testElementDelete(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        testRoot = StorageService(mc).getElement(StorageServiceTest.testSpacePath)
        self.assertEqual(testRoot.listElements(), ["_A1", "_A2", "a1", "a2", "a3", "a4", "a5"])
        testRoot.resolve('a1').delete()
        self.assertEqual(testRoot.listElements(), ["_A1", "_A2", "a2", "a3", "a4", "a5"])
        testRoot.resolve('_A2').delete()
        self.assertEqual(testRoot.listElements(), ["_A1", "a2", "a3", "a4", "a5"])
        mc.stop()

    def testCreateFile(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        testRoot = StorageService(mc).getElement(StorageServiceTest.testSpacePath)
        testRoot.resolve('NewFile').createFile()
        testRoot.resolve('a2/NewFile').createFile()
        self.assertEqual(testRoot.listElements(),
                         ["NewFile", "_A1", "_A2", "a1", "a2", "a3", "a4", "a5"])
        self.assertEqual(testRoot.resolve('a2').listElements(), ["NewFile"])
        mc.stop()

    def testCreateDirectory(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        testRoot = StorageService(mc).getElement(StorageServiceTest.testSpacePath)
        testRoot.resolve('a2').resolve('NewDir').createDirectory()
        self.assertEqual(testRoot.resolve('a2').listElements(), ["NewDir"])
        mc.stop()

    def testHBTFile(self):
        mc = Session(("localhost", StorageServiceTest.port), None)
        mc.start()
        service = StorageService(mc)
        hbtFile = service.getElement(StorageServiceTest.testSpacePath).resolve('HBTFileTest.hbt').toHBTFileElement()
        hbtFile.initialize(
            [["Column 1", HBTFileElement.BYTE], ["Column 2", HBTFileElement.SHORT], ["Column 3", HBTFileElement.INT],
             ["Column 4", HBTFileElement.LONG], ["Column 5", HBTFileElement.FLOAT],
             ["Column 6", HBTFileElement.DOUBLE]])
        hbtFile.appendRow([1, 2, 3, 4, 5, 6])
        hbtFile.appendRows([[1, 2, 3, 4, 5, 6], [1, 2, 3, 4, 5, 6], [1, 2, 3, 4, 5, 6], [1.1, 2.2, 3.3, 4.4, 5.5, 6.6],
                            [1.1, 2.2, 3.3, 4.4, 5.5, 6.6]])
        self.assertEqual(hbtFile.readRows(0, 1)[0], [1, 2, 3, 4, 5, 6])
        self.assertEqual(hbtFile.readRow(1), [1, 2, 3, 4, 5, 6])
        self.assertEqual(hbtFile.readRow(2), [1, 2, 3, 4, 5, 6])
        self.assertEqual(hbtFile.readRow(3), [1, 2, 3, 4, 5, 6])
        self.assertEqual(hbtFile.readRow(4), [1, 2, 3, 4, 5.5, 6.6])
        self.assertEqual(hbtFile.readRow(5), [1, 2, 3, 4, 5.5, 6.6])
        self.assertEqual(hbtFile.getColumnCount(), 6)
        self.assertEqual(hbtFile.getRowCount(), 6)
        self.assertEqual(hbtFile.getHeadNames(),
                         ['Column 1', 'Column 2', 'Column 3', 'Column 4', 'Column 5', 'Column 6'])
        mc.stop()

    def tearDown(self):
        pass

    @classmethod
    def tearDownClass(cls):
        pass


if __name__ == '__main__':
    unittest.main()
