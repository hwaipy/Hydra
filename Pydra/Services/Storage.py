__author__ = 'Hwaipy'


class StorageService:
    def __init__(self, session):
        self.session = session
        self.blockingInvoker = self.session.blockingInvoker(u'StorageService')

    def getElement(self, path):
        return StorageElement(self, path)

    def listElements(self, path, withMetaData=False):
        return self.blockingInvoker.listElements(u"", path, withMetaData)

    def metaData(self, path, withTime=False):
        return self.blockingInvoker.metaData(u"", path, withTime)

    def read(self, path, start, length):
        return self.blockingInvoker.read(u"", path, start, length)

    def readAsString(self, path, start, length):
        return str(self.read(path, start, length), encoding="UTF-8")

    def readAll(self, path):
        return self.blockingInvoker.readAll(u"", path)

    def readAllAsString(self, path):
        return str(self.readAll(path), encoding="UTF-8")

    def append(self, path, data):
        return self.blockingInvoker.append(u"", path, data)

    def write(self, path, data, start):
        return self.blockingInvoker.write(u"", path, data, start)

    def clear(self, path):
        return self.blockingInvoker.clear(u"", path)

    def delete(self, path):
        return self.blockingInvoker.delete(u"", path)

    def readNote(self, path):
        return self.blockingInvoker.readNote(u"", path).get(u"Note")

    def writeNote(self, path, data):
        return self.blockingInvoker.writeNote(u"", path, data)

    def createFile(self, path):
        return self.blockingInvoker.createFile(u"", path)

    def createDirectory(self, path):
        return self.blockingInvoker.createDirectory(u"", path)

    def exists(self, path):
        return self.blockingInvoker.exists(u"", path)

    def HBTFileInitialize(self, path, heads):
        return self.blockingInvoker.HBTFileInitialize(u"", path, heads)

    def HBTFileAppendRows(self, path, rows):
        return self.blockingInvoker.HBTFileAppendRows(u"", path, rows)

    def HBTFileReadRows(self, path, start, count):
        return self.blockingInvoker.HBTFileReadRows(u"", path, start, count)

    def HBTFileReadAllRows(self, path):
        return self.blockingInvoker.HBTFileReadAllRows(u"", path)

    def HBTFileMetaData(self, path):
        return self.blockingInvoker.HBTFileMetaData(u"", path)


class StorageElement:
    def __init__(self, storageService, path):
        self.storageService = storageService
        self.path = path

    def resolve(self, subPath):
        p = self.path
        if (not p.endswith(u'/')) and (not subPath.startswith(u'/')):
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

    def readMetaData(self):
        return self.storageElement.storageService.HBTFileMetaData(self.storageElement.path)

    def getColumnCount(self):
        return self.readMetaData()['ColumnCount']

    def getRowCount(self):
        return self.readMetaData()['RowCount']

    def getHeads(self):
        return self.readMetaData()['Heads']

    def getHeadNames(self):
        return [h[0] for h in self.readMetaData()['Heads']]
