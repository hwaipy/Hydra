from pymongo import MongoClient
import math
import sys
import matplotlib.pyplot as plt

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
        qbers = self.getQbers(startTime, stopTime)
        print('QBERs loaded: {}'.format(len(qbers.dbDocs)))
        channelMonitors = self.getChannelMonitor(startTime, stopTime)
        print('ChannelMonitors loaded: {}'.format(len(channelMonitors.dbDocs)))
        syncDifference = (qbers.systemAndTDCTimeMatchs[0][0] - channelMonitors.triggers[0]) / 1000.
        print('SystemTime difference between QBERS and ChannelMonitors is {} s'.format(syncDifference))
        if math.fabs(syncDifference) > 1:
            print('exiting')
            sys.exit(0)
        dT_QBERs_ChannelMonitors = channelMonitors.triggers[0] - qbers.systemAndTDCTimeMatchs[0][1] / 1e9

        cursorChannelMonitor = 0
        dataSets = []
        for qberDoc in qbers.dbDocs[:]:
            sectionStart = qberDoc.tdcTime[0] / 1e9 + dT_QBERs_ChannelMonitors
            sectionStop = qberDoc.tdcTime[1] / 1e9 + dT_QBERs_ChannelMonitors
            sectionCount = len(qberDoc.qberSections)
            setDuration = (sectionStop - sectionStart) / sectionCount
            for setIndex in range(0, sectionCount):
                dataSet = DataSet(qberDoc, setIndex)
                setStart = sectionStart + setIndex * setDuration
                setStop = setStart + setDuration
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
        print('DataSet loaded: {}'.format(len(dataSets)))
        return DataSets(dataSets)

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


class DataSets:
    def __init__(self, dataSets):
        self.dataSets = dataSets
        self.counts1 = [dataSet.counts[0] for dataSet in self.dataSets]
        self.counts2 = [dataSet.counts[1] for dataSet in self.dataSets]
        self.powers1 = [dataSet.powers()[0] for dataSet in self.dataSets]
        self.powers2 = [dataSet.powers()[1] for dataSet in self.dataSets]

    def length(self):
        return len(self.dataSets)

    def countingRateStatistic(self):
        self.__countingRateStatistic(self.counts1, self.powers1)
        self.__countingRateStatistic(self.counts2, self.powers2)

    def __countingRateStatistic(self, counts, powers):
        binCount = 30
        maxPower = max(powers)
        minPower = min(powers)
        photonCountHistogram = [0] * binCount
        dataSetCountHistogram = [0] * binCount
        powerSteps = [(maxPower - minPower) / binCount * i + minPower for i in range(0, binCount)]
        for i in range(0, len(powers)):
            index = int((powers[i] - minPower) / (maxPower - minPower) * binCount)
            if index == binCount: index -= 1
            photonCountHistogram[index] += counts[i]
            dataSetCountHistogram[index] += 1
        for i in range(0, binCount):
            if dataSetCountHistogram[i] > 0:
                photonCountHistogram[i] = photonCountHistogram[i] / dataSetCountHistogram[i] * 100

        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax1.plot(powerSteps, photonCountHistogram, 'red')
        ax1.set_ylabel('Photon Count')
        ax2 = ax1.twinx()
        ax2.plot(powerSteps, dataSetCountHistogram, 'green')
        ax2.set_ylabel('Frequency')
        plt.show()

    def filter(self, threshold, ratioBetweenAB):
        filtered = []
        for dataSet in self.dataSets:
            powers = dataSet.powers()
            ratio = 0 if powers[1] == 0 else powers[0] / powers[1] * ratioBetweenAB
            if ratio > threshold and ratio < 1 / threshold:
                filtered.append(dataSet)
        return DataSets(filtered)

    def HOM(self):
        xxDip = sum([dataSet.HOM[0] for dataSet in self.dataSets])
        xxAccident = sum([dataSet.HOM[1] for dataSet in self.dataSets])
        allDip = sum([dataSet.HOM[2] for dataSet in self.dataSets])
        allAccident = sum([dataSet.HOM[3] for dataSet in self.dataSets])

        HOMDipXX = math.nan if xxAccident == 0 else xxDip / xxAccident
        HOMDipAll = math.nan if allAccident == 0 else allDip / allAccident
        print('HOM XX:  {} for {}'.format(HOMDipXX, xxAccident))
        print('HOM All: {} for {}'.format(HOMDipAll, allAccident))
        return [HOMDipXX, HOMDipAll]


class DataSet:
    def __init__(self, qberDoc, setIndex):
        self.qberDoc = qberDoc
        self.setIndex = setIndex
        self.channelMonitorEntries = []
        # QBER: O-O, O-X, O-Y, O-Z, X-O, X-X, X-Y, X-Z, Y-O, Y-X, Y-Y, Y-Z, Z-O, Z-X, Z-Y, Z-Z
        self.QBER = self.qberDoc.qberSections[setIndex]
        # HOM for pulse 0: X-X dip, X-X accident, All dip, All accident
        self.HOM = [self.qberDoc.homSections[i][setIndex] for i in range(0, 4)]
        # Count: channel 2, channel 4
        self.counts = self.qberDoc.countSections[setIndex]

    def appendChannelMonitorEntry(self, entry):
        self.channelMonitorEntries.append(entry)

    def powers(self):
        if len(self.channelMonitorEntries) > 0:
            power1 = sum([entry.power1 for entry in self.channelMonitorEntries]) / len(self.channelMonitorEntries)
            power2 = sum([entry.power2 for entry in self.channelMonitorEntries]) / len(self.channelMonitorEntries)
            return [power1, power2]
        else:
            return [0, 0]


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
            self.countSections = dbDoc['CountSections']
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
    # lastTime = 1554302870484
    print('Last Time is {}'.format(lastTime))

    startTime = lastTime - 30000
    stopTime = lastTime - 5000
    dataSets = ana.loadRangedData(startTime, stopTime)

    dataSets.countingRateStatistic()
    # filteredDataSets = dataSets.filter(0.8, 1)
    # print(len(filteredDataSets.dataSets))
    # print(filteredDataSets.HOM())
