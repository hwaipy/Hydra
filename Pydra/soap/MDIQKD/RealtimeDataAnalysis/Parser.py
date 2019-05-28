import math
from datetime import datetime
import time
import os
import msgpack
import matplotlib.pyplot as plt
import shutil
import numpy as np


class ParserMonitor:
    def __init__(self, storeDir, resultDir):
        self.storeDir = storeDir
        self.resultDir = resultDir
        self.lastParsed = datetime.strptime("20100101-000000.000", "%Y%m%d-%H%M%S.%f")

    def begin(self):
        while True:
            newData = self.__checkNewData()
            if newData:
                self.__parse(newData)
            else:
                # time.sleep(1)
                break

    def __parse(self, newData):
        parser = Parser(['{}/{}'.format(self.storeDir, f) for f in newData])
        parser.storeDumpedFiles('{}/dumps'.format(self.resultDir))
        parser.parse('{}/results/'.format(self.resultDir))

    def __checkNewData(self):
        dumpedFiles = [f for f in os.listdir(self.storeDir) if f.lower().endswith('.dump')]
        dumpedFiles.sort()
        dumpedEntries = [(f, datetime.strptime(f[:19], "%Y%m%d-%H%M%S.%f")) for f in dumpedFiles]
        newDumpedEntries = [e for e in dumpedEntries if e[1] > self.lastParsed]
        newDumpedQBEREntries = [e for e in newDumpedEntries if e[0].lower().endswith('_qber.dump')]
        newDumpedChannelEntries = [e for e in newDumpedEntries if e[0].lower().endswith('_channel.dump')]
        pQBER = 0
        pChannel = 0
        while pQBER < len(newDumpedQBEREntries) and pChannel < len(newDumpedChannelEntries):
            QBEREntry = newDumpedQBEREntries[pQBER]
            channelEntry = newDumpedChannelEntries[pChannel]
            delta = (channelEntry[1] - QBEREntry[1]).total_seconds()
            if math.fabs(delta) < 3:
                if channelEntry[1] > QBEREntry[1]:
                    self.lastParsed = channelEntry[1]
                else:
                    self.lastParsed = QBEREntry[1]
                return (QBEREntry[0], channelEntry[0])
            if channelEntry[1] > QBEREntry[1]:
                pQBER += 1
            else:
                pChannel += 1
        return None


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


class Channel:
    def __init__(self, sections, threshold=1):
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


class Parser:
    def __init__(self, data):
        self.parameters = {}
        self.QBERFile = [f for f in data if f.lower().endswith('_qber.dump')][0]
        self.channelFile = [f for f in data if f.lower().endswith('_channel.dump')][0]
        self.__loadQBERs(self.QBERFile)
        self.__loadChannel(self.channelFile)
        self.__performTimeMatch()
        self.__performEntryMatch()

    def storeDumpedFiles(self, targetDir):
        shutil.copyfile(self.QBERFile, '{}/{}'.format(targetDir, os.path.basename(self.QBERFile)))
        shutil.copyfile(self.channelFile, '{}/{}'.format(targetDir, os.path.basename(self.channelFile)))

    def parse(self, resultDir):
        resultDir = '{}/{}'.format(resultDir, os.path.basename(self.QBERFile)[:15])
        if not os.path.exists(resultDir):
            os.mkdir(resultDir)
        self.showCountChannelRelations('{}/CountChannelRelations.png'.format(resultDir))
        # ana.showChannelAndFFTs('sampleReport/Channel-1.png', 'sampleReport/Channel-2.png', 'sampleReport/ChannelFFT-1.png',
        #                        'sampleReport/ChannelFFT-2.png')
        # ana.showHOMs(0.8, np.logspace(-0.7, 0.7, num=100, endpoint=True, base=10.0), 'sampleReport/HOMDipAll.png', 0.8)
        print(self.parameters)

    def showCountChannelRelations(self, path):
        filteredQBEREntries = [e for e in self.QBERs.entries if len(e.relatedChannelEntries) > 0]
        counts = [e.counts for e in filteredQBEREntries]
        powers = [e.relatedPowers() for e in filteredQBEREntries]
        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax2 = ax1.twinx()
        labels = ['Alice', 'Bob']
        colors = ['C0', 'C1']
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

            validSidePowers = []
            validSideCounts = []
            for i in range(len(sidePowers)):
                if sidePowers[i] < 4.8:
                    validSidePowers.append(sidePowers[i])
                    validSideCounts.append(sideCounts[i])
            z = np.polyfit(validSidePowers, validSideCounts, 1)
            self.parameters['CountChannelRelations Fitting {} Slope'.format(kk)] = z[1]
            self.parameters['CountChannelRelations Fitting {} Intercept'.format(kk)] = z[0]

            ax1.scatter(sidePowers, sideCounts, s=2, color=colors[kk], label='Counts', alpha=0.5)
            ax2.plot(powerSteps, entryCountHistogram, colors[kk], label=labels[kk])
            ax1.plot(powerSteps, [z[0] + p * z[1] for p in powerSteps])
        ax1.set_ylabel('APD Counts')
        ax1.set_xlabel('PD Power')
        ax2.set_ylabel('Frequencies')
        plt.legend()
        plt.savefig(path, dpi=300)
        plt.close()

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


if __name__ == '__main__':
    begin = time.time()

    monitor = ParserMonitor('/Users/Hwaipy/Downloads/MDI-Store', '/Users/Hwaipy/Downloads/MDI-Result')
    monitor.begin()

    end = time.time()
    print('Done in {} s.'.format(end - begin))
