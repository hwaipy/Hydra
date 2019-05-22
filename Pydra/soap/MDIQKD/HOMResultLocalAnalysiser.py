import math
import sys
import matplotlib.pyplot as plt
import os
import msgpack
import numpy as np


class HOMResultLocalAnalysiser:
    def __init__(self, dir):
        self.__loadQBERs('{}/QBERs.dump'.format(dir))
        self.__loadChannel('{}/Channel.dump'.format(dir))
        self.__performTimeMatch()
        self.__performEntryMatch()

    def showRefTimeDiffs(self, path):
        systemTimeReference = min(self.QBERs.systemTimes)
        QBERSystemTimes = [(t - systemTimeReference) / 1000.0 for t in self.QBERs.systemTimes]
        channelSystemTimes = [(t - systemTimeReference) / 1000.0 for t in self.channel.systemTimes]
        QBERMonitorSyncs = self.QBERs.channelMonitorSyncs
        channelTriggers = [(self.channel.entries[i].refTime - systemTimeReference) / 1000.0
                           for i in self.channel.riseIndices]
        systemTimeCount = min(len(QBERSystemTimes), len(channelSystemTimes))
        monitorSyncCount = min(len(QBERMonitorSyncs), len(channelTriggers))
        plt.scatter(QBERSystemTimes[:systemTimeCount], channelSystemTimes[:systemTimeCount], label='All Sections')
        plt.scatter(QBERMonitorSyncs[:monitorSyncCount], channelTriggers[:monitorSyncCount], label='Triggers')
        plt.xlabel('QBER system time (s)')
        plt.ylabel('Channel system time (s)')
        plt.legend()
        plt.savefig(path, dpi=300)
        plt.close()

    def showCountChannelRelations(self, path1, path2):
        filteredQBEREntries = [e for e in self.QBERs.entries if len(e.relatedChannelEntries) > 0]
        counts = [e.counts for e in filteredQBEREntries]
        powers = [e.relatedPowers() for e in filteredQBEREntries]
        paths = [path1, path2]
        for kk in [0, 1]:
            sidePowers = [p[kk] for p in powers]
            sideCounts = [c[kk] for c in counts]
            binCount = 30
            maxPower = max(sidePowers)
            minPower = min(sidePowers)
            powerSteps = [(maxPower - minPower) / binCount * (i + 0.5) + minPower for i in range(0, binCount)]
            entryCountHistogram = [0] * binCount
            for i in range(0, len(sidePowers)):
                index = int((sidePowers[i] - minPower) / (maxPower - minPower) * binCount)
                if index == binCount: index -= 1
                entryCountHistogram[index] += 1

            fig = plt.figure()
            ax1 = fig.add_subplot(111)
            ax1.scatter(sidePowers, sideCounts, label='Counts')
            ax1.set_ylabel('APD Counts')
            ax1.set_xlabel('PD Power')
            ax2 = ax1.twinx()
            ax2.plot(powerSteps, entryCountHistogram, 'green', label='Frequencies')
            ax2.set_ylabel('Frequencies')
            plt.legend()
            plt.savefig(paths[kk], dpi=300)
            plt.close()

    def showChannelAndFFTs(self, path1, path2, pathFFT1, pathFFT2):
        powers = [[e.power1 for e in self.channel.entries], [e.power2 for e in self.channel.entries]]
        paths = [path1, path2]
        pathFFTs = [pathFFT1, pathFFT2]
        for kk in [0, 1]:
            ps = powers[kk]
            import numpy
            fftCom = numpy.fft.fft(ps)
            ffted = [math.sqrt(d.real ** 2 + d.imag ** 2) for d in fftCom]
            ffted = ffted[:int(len(ffted) / 2)]
            plt.semilogy([i / len(ffted) * 5000 for i in range(1, len(ffted))], ffted[1:])
            plt.savefig(pathFFTs[kk], dpi=300)
            plt.close()

            # plt.plot([i for i in range(0, len(ps))], ps)
            # plt.savefig(paths[kk], dpi=300)
            # plt.close()

    def HOM(self, shreshold, ratio, singleMatch=None):
        filteredQBEREntries = [e for e in self.QBERs.entries if e.powerMatched(shreshold, ratio, singleMatch)]
        # filteredQBEREntries = [e for e in self.QBERs.entries if e.countMatched(shreshold, ratio)]
        # print(len(filteredQBEREntries))

        xxDip = sum([e.HOMs[0] for e in filteredQBEREntries])
        xxAccident = sum([e.HOMs[1] for e in filteredQBEREntries])
        allDip = sum([e.HOMs[2] for e in filteredQBEREntries])
        allAccident = sum([e.HOMs[3] for e in filteredQBEREntries])
        HOMDipXX = math.nan if xxAccident == 0 else xxDip / xxAccident
        HOMDipAll = math.nan if allAccident == 0 else allDip / allAccident
        # print('HOM XX:  {} for {}'.format(HOMDipXX, xxAccident))
        # print('HOM All: {} for {}'.format(HOMDipAll, allAccident))
        return [HOMDipXX, xxAccident, HOMDipAll, allAccident]

    def showHOMs(self, shreshold, ratios, path, singleMatch=None):
        ratios = [r for r in ratios]
        HOMDipXXs = []
        xxAccidents = []
        HOMDipAlls = []
        allAccidents = []
        for r in ratios:
            HOM = self.HOM(shreshold, r, singleMatch)
            HOMDipXXs.append(HOM[0])
            xxAccidents.append(HOM[1])
            HOMDipAlls.append(HOM[2])
            allAccidents.append(HOM[3])
        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax1.semilogx(ratios, HOMDipAlls, label='HOM-All')
        ax1.set_ylabel('ratios')
        ax1.set_xlabel('HOM Dip')
        ax2 = ax1.twinx()
        ax2.semilogx(ratios, allAccidents, 'green', label='Side Coincidences')
        ax2.set_ylabel('Side Coincidences')
        plt.legend()
        plt.savefig(path, dpi=300)
        plt.close()

    def __loadQBERs(self, path):
        entries = self.__loadMsgpackEntries(path)
        self.QBERs = QBERs(entries)
        self.QBERs.validate()

    def __loadChannel(self, path):
        entries = self.__loadMsgpackEntries(path)
        self.channel = Channel(entries)
        self.channel.validate()

    def __performTimeMatch(self):
        for i in range(0, len(self.QBERs.channelMonitorSyncs) - 1):
            segmentStart = self.QBERs.channelMonitorSyncs[i]
            segmentStop = self.QBERs.channelMonitorSyncs[i + 1]
            channelEntryStartIndex = self.channel.riseIndices[i]
            channelEntryStopIndex = self.channel.riseIndices[i + 1]
            # print('Segmeng:    {} --> {},    channel entries {} --> {}'.
            #       format(segmentStart, segmentStop, channelEntryStartIndex, channelEntryStopIndex))
            for i in range(channelEntryStartIndex, channelEntryStopIndex):
                self.channel.entries[i].tdcTime = \
                    (i * 1.0 - channelEntryStartIndex) / (channelEntryStopIndex - channelEntryStartIndex) \
                    * (segmentStop - segmentStart) + segmentStart

    def __performEntryMatch(self):
        channelSearchIndexStart = 0
        for QBEREntry in self.QBERs.entries:
            channelSearchIndex = channelSearchIndexStart
            while channelSearchIndex < len(self.channel.entries):
                channelEntry = self.channel.entries[channelSearchIndex]
                if channelEntry.tdcTime < QBEREntry.tdcStart:
                    channelSearchIndex += 1
                    channelSearchIndexStart += 1
                elif channelEntry.tdcTime < QBEREntry.tdcStop:
                    QBEREntry.relatedChannelEntries.append(channelEntry)
                    channelSearchIndex += 1
                else:
                    break

    def __loadMsgpackEntries(self, path):
        file = open(path, 'rb')
        data = file.read(os.path.getsize(path))
        file.close()
        unpacker = msgpack.Unpacker(raw=False)
        unpacker.feed(data)
        entries = []
        for packed in unpacker:
            entries.append(packed)
        return entries


class Channel:
    def __init__(self, sections, threshold=2.5):
        self.sections = sections
        self.systemTimes = []
        self.entries = []
        self.riseIndices = []
        for section in self.sections:
            self.systemTimes.append(section['SystemTime'])
            channelDatas = section['ChannelData']
            for channelData in channelDatas:
                self.entries.append(ChannelEntry(channelData[:3], channelData[3]))
        self.__searchForRises(threshold)

    def __searchForRises(self, threshold):
        for i in range(0, len(self.entries) - 1):
            triggerLevelPre = self.entries[i].trigger
            triggerLevelPost = self.entries[i + 1].trigger
            if (triggerLevelPre < threshold and triggerLevelPost > threshold):
                self.riseIndices.append(i)

    def validate(self):
        for i in range(0, len(self.riseIndices) - 1):
            delta = (self.entries[self.riseIndices[i + 1]].refTime - self.entries[self.riseIndices[i]].refTime) / 1000
            if math.fabs(delta - 10) > 0.01:
                print(delta)
                print([self.entries[j].refTime - self.entries[j - 1].refTime for j in range(1, len(self.riseIndices))])
                raise RuntimeError("Error in ChannelMonitorSyncs")


class ChannelEntry:
    def __init__(self, powers, refTime):
        self.power1 = powers[1]
        self.power2 = powers[2]
        self.trigger = powers[0]
        self.refTime = refTime
        self.tdcTime = -1


class QBERs:
    def __init__(self, sections):
        self.sections = sections
        self.systemTimes = []
        self.TDCTimeOfSectionStart = self.sections[0]['TDCTime'][0]
        self.channelMonitorSyncs = []
        self.entries = []
        for section in self.sections:
            self.systemTimes.append(section['SystemTime'])
            HOMSections = section['HOMSections']
            # print(len(HOMSections[0]))
            countSections = section['CountSections']
            tdcStartStop = [(time - self.TDCTimeOfSectionStart) / 1e12 for time in section['TDCTime']]
            for sync in section['ChannelMonitorSyncs']:
                self.channelMonitorSyncs.append((sync - self.TDCTimeOfSectionStart) / 1e12)
            entryCount = len(countSections)
            for i in range(0, entryCount):
                entryTDCStartStop = [(tdcStartStop[1] - tdcStartStop[0]) / entryCount * j + tdcStartStop[0]
                                     for j in [i, i + 1]]
                entryHOMs = [HOMSections[j][i] for j in range(0, len(HOMSections))]
                entryCounts = [countSections[i][j] for j in range(0, len(countSections[0]))]
                self.entries.append(QBEREntry(entryTDCStartStop, entryHOMs, entryCounts))

    def validate(self):
        for i in range(0, len(self.channelMonitorSyncs) - 1):
            delta = self.channelMonitorSyncs[i + 1] - self.channelMonitorSyncs[i]
            if math.fabs(delta - 10) > 0.001:
                print(self.channelMonitorSyncs)
                raise RuntimeError("Error in ChannelMonitorSyncs of QBER")


class QBEREntry:
    def __init__(self, tdcStartStop, HOMs, counts):
        self.tdcStart = tdcStartStop[0]
        self.tdcStop = tdcStartStop[1]
        self.HOMs = HOMs
        self.counts = counts
        self.relatedChannelEntries = []

    def powerMatched(self, threshold, ratio, singleMatch=None):
        if len(self.relatedChannelEntries) == 0: return False
        powers = self.relatedPowers()
        if singleMatch is not None:
            powers[1] = singleMatch
        actualRatio = 0 if powers[1] == 0 else powers[0] / powers[1] * ratio
        return (actualRatio > threshold) and (actualRatio < (1 / threshold))

    def countMatched(self, threshold, ratio):
        if self.counts[0] * self.counts[1] == 0: return False
        actualRatio = 0 if self.counts[1] == 0 else self.counts[0] * 1.0 / self.counts[1] * ratio
        return (actualRatio > threshold) and (actualRatio < (1 / threshold))

    def relatedPowers(self):
        if len(self.relatedChannelEntries) == 0: return [0, 0]
        power1 = sum([c.power1 for c in self.relatedChannelEntries]) / len(self.relatedChannelEntries)
        power2 = sum([c.power2 for c in self.relatedChannelEntries]) / len(self.relatedChannelEntries)
        return [power1, power2]


class ResultAnalysiser:
    def __init__(self, db, dir):
        self.db = db
        self.dir = dir
        self.reports = self.db['TDCReport-20190521']
        self.monitor = self.db['ChannelMonitor-20190521']

    def loadRangedData(self, startTime, stopTime, dump):
        qbers = self.getQbers(startTime, stopTime)
        print('QBERs loaded: {}'.format(len(qbers.dbDocs)))
        qbers.dump('QBERs.dump')
        if (len(qbers.systemAndTDCTimeMatchs) == 0): print("QBERS match 0!")
        # print(qbers.systemAndTDCTimeMatchs[0][0] / 1000)
        channelMonitors = self.getChannelMonitor(startTime, stopTime)
        print('ChannelMonitors loaded: {}'.format(len(channelMonitors.dbDocs)))
        if dump: channelMonitors.dump('Channel.dump')

    def getNewestDocumentTime(self):
        docSet = self.reports.find().sort("SystemTime", -1).limit(1)
        return docSet[0]["SystemTime"]

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
            if math.fabs(delta - 10) > 0.001:
                print('wrong!!!!!!!!!!!!!!!!!!!')
                print(delta)
                raise RuntimeError("Error in ChannelMonitorSyncs of QBER")

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
            # print(delta)
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


def ana():
    import os

    if True:
        from pymongo import MongoClient
        client = MongoClient('mongodb://MDIQKD:freespace@192.168.25.27:27019')
        db = client['Freespace_MDI_QKD']
        ana = ResultAnalysiser(db, dir)
        lastTime = ana.getNewestDocumentTime()
        # lastTime = 1558450056009.0
        print('Last Time is {}'.format(lastTime))
        startTime = lastTime - 100 * 1000
        stopTime = lastTime - 2 * 1000
        ana.loadRangedData(startTime, stopTime, True)

    if not os.path.exists('sampleReport'):
        os.mkdir('sampleReport')
    os.rename('QBERs.dump', 'sampleReport/QBERs.dump')
    os.rename('Channel.dump', 'sampleReport/Channel.dump')

    ana = HOMResultLocalAnalysiser('sampleReport')
    ana.showRefTimeDiffs('sampleReport/RefTimeDiffs.png')
    ana.showCountChannelRelations('sampleReport/CountChannelRelations-1.png',
                                  'sampleReport/CountChannelRelations-2.png')
    ana.showChannelAndFFTs('sampleReport/Channel-1.png', 'sampleReport/Channel-2.png', 'sampleReport/ChannelFFT-1.png',
                           'sampleReport/ChannelFFT-2.png')
    ana.showHOMs(0.8, np.logspace(-0.7, 0.7, num=100, endpoint=True, base=10.0), 'sampleReport/HOMDipAll.png', 0.8)

if __name__ == '__main__':
    ana()
