__author__ = 'Hwaipy'


class StorageService:
    def __init__(self, session):
        self.session = session
        self.blockingInvoker = self.session.blockingInvoker('StorageService')

    def getElement(self, path):
        return StorageElement(self, path)

    def listElements(self, path, withMetaData=False):
        return self.blockingInvoker.listElements("", path, withMetaData)

    def metaData(self, path, withTime=False):
        return self.blockingInvoker.metaData("", path, withTime)

    def read(self, path, start, length):
        return self.blockingInvoker.read("", path, start, length)

    def readAsString(self, path, start, length):
        return str(self.read(path, start, length), encoding="UTF-8")

    def readAll(self, path):
        return self.blockingInvoker.readAll("", path)

    def readAllAsString(self, path):
        return str(self.readAll(path), encoding="UTF-8")

    def append(self, path, data):
        return self.blockingInvoker.append("", path, data)

    def write(self, path, data, start):
        return self.blockingInvoker.write("", path, data, start)

    def clear(self, path):
        return self.blockingInvoker.clear("", path)

    def delete(self, path):
        return self.blockingInvoker.delete("", path)

    def readNote(self, path):
        return self.blockingInvoker.readNote("", path).get("Note")

    def writeNote(self, path, data):
        return self.blockingInvoker.writeNote("", path, data)

    def createFile(self, path):
        return self.blockingInvoker.createFile("", path)

    def createDirectory(self, path):
        return self.blockingInvoker.createDirectory("", path)

    def exists(self, path):
        return self.blockingInvoker.exists("", path)

    def HBTFileInitialize(self, path, heads):
        return self.blockingInvoker.HBTFileInitialize("", path, heads)

    def HBTFileAppendRows(self, path, rows):
        return self.blockingInvoker.HBTFileAppendRows("", path, rows)

    def HBTFileReadRows(self, path, start, count):
        return self.blockingInvoker.HBTFileReadRows("", path, start, count)

    def HBTFileReadAllRows(self, path):
        return self.blockingInvoker.HBTFileReadAllRows("", path)


class StorageElement:
    def __init__(self, storageService, path):
        self.storageService = storageService
        self.path = path

    def resolve(self, subPath):
        p = self.path
        if (not p.endswith('/')) and (not subPath.startswith('/')):
            p = p + '/'
        p = p + subPath
        return StorageElement(self.storageService, p)

    def listElements(self, withMetaData=False):
        return self.storageService.listElements(self.path, withMetaData)

    def metaData(self, withTime=False):
        return self.storageService.metaData(self.path, withTime)

    def read(self, start, length):
        return self.storageService.read(self.path, start, length)

    def readAsString(self, start, length):
        return self.storageService.readAsString(self.path, start, length)

    def readAll(self):
        return self.storageService.readAll(self.path)

    def readAllAsString(self):
        return self.storageService.readAllAsString(self.path)

    def append(self, data):
        return self.storageService.append(self.path, data)

    def write(self, data, start):
        return self.storageService.write(self.path, data, start)

    def clear(self):
        return self.storageService.clear(self.path)

    def delete(self):
        return self.storageService.delete(self.path)

    def readNote(self):
        return self.storageService.readNote(self.path)

    def writeNote(self, data):
        return self.storageService.writeNote(self.path, data)

    def createFile(self):
        return self.storageService.createFile(self.path)

    def createDirectory(self):
        return self.storageService.createDirectory(self.path)

    def exists(self):
        return self.storageService.exists(self.path)

    def toHBTFileElement(self):
        return HBTFileElement(self)


class HBTFileElement:
    BYTE = 'Byte'
    SHORT = 'Short'
    INT = 'Int'
    LONG = 'Long'
    FLOAT = 'Float'
    DOUBLE = 'Double'

    def __init__(self, storageElement):
        self.storageElement = storageElement

    def initialize(self, heads):
        return self.storageElement.storageService.HBTFileInitialize(self.storageElement.path, heads)

    def appendRows(self, rows):
        return self.storageElement.storageService.HBTFileAppendRows(self.storageElement.path, rows)

    def appendRow(self, row):
        return self.appendRows([row])

    def readRows(self, start, count):
        return self.storageElement.storageService.HBTFileReadRows(self.storageElement.path, start, count)

    def readRow(self, rowNum):
        return self.readRows(rowNum, 1)[0]

    def readAllRows(self):
        return self.storageElement.storageService.HBTFileReadAllRows(self.storageElement.path)
