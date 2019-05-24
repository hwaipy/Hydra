import Pydra
import queue
import math
import msgpack
import time
from datetime import datetime


class MDIQKD_Analysiser:
    def __init__(self):
        self.QBERQueue = queue.Queue()
        self.channelQueue = queue.Queue()

    def dumpQBER(self, QBER):
        self.QBERQueue.put(QBER)

    def dumpChannel(self, channel):
        self.channelQueue.put(channel)

    def __formatTime(self, timestamp):
        date_time = datetime.fromtimestamp(timestamp)
        return date_time.strftime("%Y%m%d-%H%M%S.%f")[:-3]

    def loop(self):
        QBERList = []
        channelList = []
        while True:
            QBERList.append(self.QBERQueue.get())
            channelList.append(self.channelQueue.get())
            qbers = QBERs(QBERList)
            qbersSyncs = qbers.getChannelMonitorSyncs()
            if (len(qbersSyncs) == 0):
                QBERList = []
            elif (len(qbersSyncs) >= 2):
                self.__dump(QBERList, '{}_QBER.dump'.format(self.__formatTime(qbers.getSystemTimes()[0] / 1000.0)))
                QBERList = [QBERList[-1]]
            channel = Channel(channelList)
            channelsRiseIndices = channel.getRiseIndices()
            if (len(channelsRiseIndices) == 0):
                channelList = []
            elif (len(channelsRiseIndices) >= 2):
                self.__dump(channelList,
                            '{}_Channel.dump'.format(self.__formatTime(channel.getSystemTimes()[0] / 1000.0)))
                channelList = [channelList[-1]]

    def __dump(self, content, path):
        file = open(path, 'wb')
        for doc in content:
            packed = msgpack.packb(doc, encoding='utf-8')
            file.write(packed)
        file.close()


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

    def getChannelMonitorSyncs(self):
        return self.channelMonitorSyncs

    def getSystemTimes(self):
        return self.systemTimes


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

    def getRiseIndices(self):
        return self.riseIndices

    def getSystemTimes(self):
        return self.systemTimes

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


if __name__ == '__main__':
    analysiser = MDIQKD_Analysiser()
    session = Pydra.Session.newSession(('localhost', 20102), analysiser, 'MDIQKD-Analysiser')
    analysiser.loop()
