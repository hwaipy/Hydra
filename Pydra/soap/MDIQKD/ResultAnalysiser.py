from pymongo import MongoClient
import time
import math


class ResultAnalysiser:
    def __init__(self, db):
        self.db = db
        self.reports = self.db['TDCReport']
        self.monitor = self.db['ChannelMonitor']

    def getDocumentCount(self):
        return self.reports.count()

    def getNewestDocumentTime(self):
        docSet = self.reports.find().sort("SystemTime", -1).limit(1)
        return docSet[0]["SystemTime"]

    def loadRangedData(self, startTime, stopTime):
        qbers = self.getQbers(startTime - 5000, stopTime + 5000)
        print('QBERs loaded: {}'.format(len(qbers.dbDocs)))
        channelMonitors = self.getChannelMonitor(startTime - 5000, stopTime + 5000)
        print('ChannelMonitors loaded: {}'.format(len(channelMonitors.dbDocs)))
        print('SystemTime difference between QBERS and ChannelMonitors is {} s'.format(
                (qbers.systemAndTDCTimeMatchs[0][0] - channelMonitors.triggers[0]) / 1000.))
        dT_QBERs_ChannelMonitors = channelMonitors.triggers[0] - qbers.systemAndTDCTimeMatchs[0][1] / 1e9

        cursorChannelMonitor = 0
        dataSets = []
        for qberDoc in qbers.dbDocs[:1]:
            sectionStart = qberDoc.tdcTime[0] / 1e9 + dT_QBERs_ChannelMonitors
            sectionStop = qberDoc.tdcTime[1] / 1e9 + dT_QBERs_ChannelMonitors
            sectionCount = len(qberDoc.qberSections)
            setDuration = (sectionStop - sectionStart) / sectionCount
            # print('section start at {}, stop at {}'.format(sectionStart, sectionStop))
            # print('set duration is {}'.format(setDuration))
            for setIndex in range(0, sectionCount):
                dataSet = DataSet(qberDoc)
                setStart = sectionStart + setIndex * setDuration
                setStop = setStart + setDuration
                # print('set start at {}, stop at {}'.format(setStart, setStop))
                while cursorChannelMonitor < len(channelMonitors.entries):
                    channelMonitorEntry = channelMonitors.entries[cursorChannelMonitor]
                    if channelMonitorEntry.time < setStart:
                        cursorChannelMonitor += 1
                    elif channelMonitorEntry.time < setStop:
                        dataSet.appendChannelMonitorEntry(channelMonitorEntry)
                        cursorChannelMonitor += 1
                    else:
                        break
                dataSets.append(dataSet)

            print(len(dataSets))
            for i in range(0, 30):
                print(dataSets[i].test())

    def getQbers(self, start, stop):
        docSet = self.__getDocumentsInRange(self.reports, "Time", start, stop)
        return QberSections(docSet)

    def getChannelMonitor(self, start, stop):
        docSet = self.__getDocumentsInRange(self.monitor, "SystemTime", start, stop)
        return ChannelMonitorSections(docSet)

    def __getDocumentsInRange(self, collection, keyWord, start, stop):
        docSet = collection.find({"$and": [{keyWord: {"$gt": start}}, {keyWord: {"$lt": stop}}]})
        docs = [doc for doc in docSet]
        return docs


class DataSet:
    def __init__(self, qberDoc):
        self.qberDoc = qberDoc
        self.channelMonitorEntries = []

    def appendChannelMonitorEntry(self, entry):
        self.channelMonitorEntries.append(entry)

    def test(self):
        print('ttt')
        print(len(self.channelMonitorEntries))


class QberSections:
    def __init__(self, dbDocs):
        self.dbDocs = [QberSections.DBDoc(dbDoc) for dbDoc in dbDocs]
        self.channelMonitorSyncs = []
        for dbDoc in self.dbDocs:
            self.channelMonitorSyncs += dbDoc.channelMonitorSyncs
        self.__checkChannelMonitorSyncs()
        self.systemAndTDCTimeMatchs = []
        for dbDoc in self.dbDocs:
            if len(dbDoc.channelMonitorSyncs) > 0:
                self.systemAndTDCTimeMatchs.append((dbDoc.time, dbDoc.channelMonitorSyncs[0]))

    def __checkChannelMonitorSyncs(self):
        for i in range(0, len(self.channelMonitorSyncs) - 1):
            delta = (self.channelMonitorSyncs[i + 1] - self.channelMonitorSyncs[i]) / 1e12
            if math.fabs(delta - 10) > 0.0001:
                raise RuntimeError("Error in ChannelMonitorSyncs of QBER")

    class DBDoc:
        def __init__(self, dbDoc):
            self.time = dbDoc['Time']
            self.qberSections = dbDoc['QBERSections']
            self.homSections = dbDoc['HOMSections']
            self.channelMonitorSyncs = dbDoc['ChannelMonitorSync'][2:]
            self.tdcTime = dbDoc['ChannelMonitorSync'][:2]


class ChannelMonitorSections:
    def __init__(self, dbDocs):
        self.dbDocs = [ChannelMonitorSections.DBDoc(dbDoc) for dbDoc in dbDocs]
        self.__newEntryTriggerMark = 0
        self.entries = []
        for doc in self.dbDocs:
            for d in doc.data:
                self.__newEntry(d[0], d[1], d[2], d[3])
        self.triggers = [entry.time for entry in self.entries if entry.triggered]
        print(self.triggers)
        self.__checkChannelMonitorSyncs()

    def __newEntry(self, trigger, power1, power2, time):
        self.entries.append(ChannelMonitorSections.ChannelMonitorEntry(
                self.__newEntryTriggerMark < 1 and trigger > 1, power1, power2, int(time)))
        self.__newEntryTriggerMark = trigger

    def __checkChannelMonitorSyncs(self):
        for i in range(0, len(self.triggers) - 1):
            delta = (self.triggers[i + 1] - self.triggers[i]) / 1e3
            if math.fabs(delta - 10) > 0.1:
                raise RuntimeError("Error in ChannelMonitorSyncs of ChannelMonitor")

    class DBDoc:
        def __init__(self, dbDoc):
            self.time = dbDoc['SystemTime']
            self.data = dbDoc['Monitor']

    class ChannelMonitorEntry:
        def __init__(self, triggered, power1, power2, time):
            self.triggered = triggered
            self.power1 = power1
            self.power2 = power2
            self.time = time


if __name__ == '__main__':
    client = MongoClient('mongodb://MDIQKD:freespace@192.168.25.27:27019')
    db = client['Freespace_MDI_QKD']
    ana = ResultAnalysiser(db)
    # print(ana.getDocumentCount())
    lastTime = ana.getNewestDocumentTime()
    # lastTime = 1554129531379
    print('Last Time is {}'.format(lastTime))

    startTime = lastTime - 100000
    stopTime = lastTime - 5000
    ana.loadRangedData(startTime, stopTime)
    # print(reports.count())
    # print(monitor.count())
    # print(reports.find_one())
    # print(monitor.find_one())
    # print(time.time()/3600-1553952980524/3600000)
    #
