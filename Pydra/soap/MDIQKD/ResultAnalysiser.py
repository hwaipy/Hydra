from pymongo import MongoClient
import math
import sys
import matplotlib.pyplot as plt


class ResultAnalysiser:
    def __init__(self, db, dir):
        self.db = db
        self.dir = dir
        self.reports = self.db['TDCReport-20190518']
        # self.monitor = self.db['ChannelMonitor-20190418']

    def getDocumentCount(self):
        return self.reports.count()

    def getNewestDocumentTime(self):
        docSet = self.reports.find().sort("SystemTime", -1).limit(1)
        return docSet[0]["SystemTime"]

    def loadRangedData(self, startTime, stopTime, dump):
        qbers = self.getQbers(startTime, stopTime)
        print('QBERs loaded: {}'.format(len(qbers.dbDocs)))
        if dump: qbers.dump('QBERs.dump')
        #if (len(qbers.systemAndTDCTimeMatchs) == 0): print("QBERS match 0!")
        #print(qbers.systemAndTDCTimeMatchs[0][0] / 1000)
        #channelMonitors = self.getChannelMonitor(startTime, stopTime)
        #print('ChannelMonitors loaded: {}'.format(len(channelMonitors.dbDocs)))
        #if dump: channelMonitors.dump('Channel.dump')


        # print(channelMonitors.triggers[0] / 1000)
        # syncDifference = (qbers.systemAndTDCTimeMatchs[0][0] - channelMonitors.triggers[0]) / 1000.
        # print('SystemTime difference between QBERS and ChannelMonitors is {} s'.format(syncDifference))
        # if math.fabs(syncDifference) > 1:
        #     print('exiting')
        #     # sys.exit(0)
        # dT_QBERs_ChannelMonitors = channelMonitors.triggers[0] - qbers.systemAndTDCTimeMatchs[0][1] / 1e9
        #
        # cursorChannelMonitor = 0
        # dataSets = []
        # for qberDoc in qbers.dbDocs[:]:
        #     sectionStart = qberDoc.tdcTime[0] / 1e9 + dT_QBERs_ChannelMonitors
        #     sectionStop = qberDoc.tdcTime[1] / 1e9 + dT_QBERs_ChannelMonitors
        #     sectionCount = len(qberDoc.qberSections)
        #     setDuration = (sectionStop - sectionStart) / sectionCount
        #     for setIndex in range(0, sectionCount):
        #         dataSet = DataSet(qberDoc, setIndex)
        #         setStart = sectionStart + setIndex * setDuration
        #         setStop = setStart + setDuration
        #         while cursorChannelMonitor < len(channelMonitors.entries):
        #             channelMonitorEntry = channelMonitors.entries[cursorChannelMonitor]
        #             if channelMonitorEntry.time < setStart:
        #                 cursorChannelMonitor += 1
        #             elif channelMonitorEntry.time < setStop:
        #                 dataSet.appendChannelMonitorEntry(channelMonitorEntry)
        #                 cursorChannelMonitor += 1
        #             else:
        #                 break
        #         dataSets.append(dataSet)
        # print('DataSet loaded: {}'.format(len(dataSets)))
        # return DataSets(dataSets, self.dir)

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
    def __init__(self, dataSets, dir):
        self.dataSets = dataSets
        self.dir = dir
        self.counts1 = [dataSet.counts[0] for dataSet in self.dataSets]
        self.counts2 = [dataSet.counts[1] for dataSet in self.dataSets]
        self.powers1 = [dataSet.powers()[0] for dataSet in self.dataSets]
        self.powers2 = [dataSet.powers()[1] for dataSet in self.dataSets]

    def length(self):
        return len(self.dataSets)

    def countingRateStatistic(self):
        # for p in self.power1:
        #     print(p)
        self.__countingRateStatistic(self.counts1, self.powers1, 'Alice')
        self.__countingRateStatistic(self.counts2, self.powers2, 'Bob')
        file1 = open('powers1.csv', 'w')
        file2 = open('powers2.csv', 'w')
        for dataSet in self.dataSets:
            op = dataSet.originalPowers()
            for p in op[0]:
                file1.write('{}\n'.format(p))
            for p in op[1]:
                file2.write('{}\n'.format(p))
        file1.close()
        file2.close()

    def __countingRateStatistic(self, counts, powers, filename):
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
        plt.savefig('{}/{}.png'.format(self.dir, filename))
        plt.close()

    def filter(self, threshold, ratioBetweenAB):
        filtered = []
        for dataSet in self.dataSets:
            powers = dataSet.powers()
            ratio = 0 if powers[1] == 0 else powers[0] / powers[1] * ratioBetweenAB
            if ratio > threshold and ratio < 1 / threshold:
                filtered.append(dataSet)
        return DataSets(filtered, self.dir)

    def HOM(self):
        xxDip = sum([dataSet.HOM[0] for dataSet in self.dataSets])
        xxAccident = sum([dataSet.HOM[1] for dataSet in self.dataSets])
        allDip = sum([dataSet.HOM[2] for dataSet in self.dataSets])
        allAccident = sum([dataSet.HOM[3] for dataSet in self.dataSets])

        HOMDipXX = math.nan if xxAccident == 0 else xxDip / xxAccident
        HOMDipAll = math.nan if allAccident == 0 else allDip / allAccident
        # print('HOM XX:  {} for {}'.format(HOMDipXX, xxAccident))
        # print('HOM All: {} for {}'.format(HOMDipAll, allAccident))
        return [HOMDipAll, allAccident]


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

    def originalPowers(self):
        if len(self.channelMonitorEntries) > 0:
            power1 = [entry.power1 for entry in self.channelMonitorEntries]
            power2 = [entry.power2 for entry in self.channelMonitorEntries]
            return [power1, power2]
        else:
            return [[], []]


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
            print(delta)
            if math.fabs(delta - 10) > 0.0001:
                print('wrong!!!!!!!!!!!!!!!!!!!')
                #    raise RuntimeError("Error in ChannelMonitorSyncs of QBER")

    def dump(self, path):
        import msgpack
        file = open(path, 'wb')
        for doc in self.dbDocs:
            data = {
                "SystemTime": doc.time,
                "TDCTime": doc.tdcTime,
                "HOMSections": doc.homSections,
                "CountSections": doc.countSections,
                "ChannelMonitorSyncs": doc.channelMonitorSyncs
            }
            packed = msgpack.packb(data, encoding='utf-8')
            file.write(packed)

        file.close()

        # unpacker = msgpack.Unpacker(encoding='utf-8')
        # fileLength  =os.path.getsize(path)
        # file = open(path, 'rb')
        # readed = file.read(fileLength)
        # unpacker.feed(readed)
        # for packed in unpacker:
        #     print(packed)
        # file.close()

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
            print(delta)
            if math.fabs(delta - 10) > 0.1:
                raise RuntimeError("Error in ChannelMonitorSyncs of ChannelMonitor")

    def dump(self, path):
        import msgpack
        file = open(path, 'wb')
        for doc in self.dbDocs:
            data = {
                "SystemTime": doc.time,
                "ChannelData": doc.data
            }
            packed = msgpack.packb(data, encoding='utf-8')
            file.write(packed)
        file.close()

        # unpacker = msgpack.Unpacker(encoding='utf-8')
        # fileLength  =os.path.getsize(path)
        # file = open(path, 'rb')
        # readed = file.read(fileLength)
        # unpacker.feed(readed)
        # for packed in unpacker:
        #     print(packed)
        # file.close()

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


if __name__ == '__main__' or True:
    import time

    client = MongoClient('mongodb://MDIQKD:freespace@192.168.25.27:27019')
    db = client['Freespace_MDI_QKD']
    ana = ResultAnalysiser(db, dir)
    print('{} documents found in DB.'.format(ana.getDocumentCount()))
    lastTime = ana.getNewestDocumentTime()
    print('Last Time is {}'.format(lastTime))

    startTime = lastTime - 100000
    stopTime = lastTime - 2000
    dataSets = ana.loadRangedData(startTime, stopTime, True)
    #
    # dataSets.countingRateStatistic()
    # ratios = []
    # homs = []
    # homCounts = []
    # ratio = 0.1
    # while ratio < 10:
    #     hom = dataSets.filter(0.8, ratio).HOM()
    #     print('Ratio = {}, HOM = {} for {}'.format(int(ratio*1000)/1000, hom[0], hom[1]))
    #     ratios.append(ratio)
    #     homs.append(hom[0])
    #     homCounts.append(hom[1])
    #     ratio *= 1.2
    # plt.semilogx(ratios, homs)
    # plt.savefig('{}/HOMs.png'.format(dir))
    # plt.close()
    # plt.semilogx(ratios, homCounts)
    # plt.savefig('{}/HOMCounts.png'.format(dir))
