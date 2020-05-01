import math
from datetime import datetime
import time
import os
import msgpack
import matplotlib.pyplot as plt
import shutil
import numpy as np
from concurrent.futures import ThreadPoolExecutor
import threading

def tt():
    print('tt')

class ParserMonitor:
    def __init__(self, storeDir, resultDir, fromNow=True):
        self.storeDir = storeDir
        self.resultDir = resultDir
        self.lastParsed = datetime.now() if fromNow else datetime.strptime("20200429-225700.000", "%Y%m%d-%H%M%S.%f")
        self.fromNow = fromNow
        self.executor = ThreadPoolExecutor(max_workers=8)

    def begin(self):
        while True:
            newData = self.__checkNewData()
            print(newData)
            if newData:
                if self.fromNow:
                    time.sleep(1)
                print('new data: {}'.format(newData))
                def parse():
                    print('parsing')
                    os.system('python3 D:/GitHub/Hydra/Pydra/soap/MDIQKD/RealtimeDataAnalysis/Parser.py {} {}'.format(newData[0],newData[1]))
                # if self.fromNow:
                # parse()
                # else:
                # self.executor.submit(tt, ())
                # print('submitted')
                # time.sleep(3)
                # try:
                #     self.__parse(newData)
                # except BaseException as e:
                #     print(e)
                threading.Thread(target=parse).start()

            else:
                time.sleep(1)

    def parse(self, newData):
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
        self.TDCTimeOfSectionStart = self.sections[0]['ChannelMonitorSync'][0]
        self.channelMonitorSyncs = []
        self.entries = []
        for section in self.sections:
            self.systemTimes.append(section['Time'])
            HOMSections = section['HOMSections']
            QBERSections = section['QBERSections']
            # print(section.keys())
            countSections = section['CountSections']
            tdcStartStop = [(time - self.TDCTimeOfSectionStart) / 1e12 for time in section['ChannelMonitorSync'][:2]]
            for sync in section['ChannelMonitorSync'][2:]:
                self.channelMonitorSyncs.append((sync - self.TDCTimeOfSectionStart) / 1e12)
            entryCount = len(countSections)
            for i in range(0, entryCount):
                entryTDCStartStop = [(tdcStartStop[1] - tdcStartStop[0]) / entryCount * j + tdcStartStop[0]
                                     for j in [i, i + 1]]
                entryHOMs = [HOMSections[j][i] for j in range(0, len(HOMSections))]
                entryQBERs = [QBERSections[i][j] for j in range(0, len(QBERSections[0]))]
                entryCounts = [countSections[i][j] for j in range(0, len(countSections[0]))]
                self.entries.append(QBEREntry(entryTDCStartStop, entryHOMs, entryCounts, entryQBERs))

    def validate(self):
        for i in range(0, len(self.channelMonitorSyncs) - 1):
            delta = self.channelMonitorSyncs[i + 1] - self.channelMonitorSyncs[i]
            if math.fabs(delta - 10) > 0.001:
                raise RuntimeError("Error in ChannelMonitorSyncs of QBER: {}".format(self.channelMonitorSyncs))


class QBEREntry:
    def __init__(self, tdcStartStop, HOMs, counts, QBERs):
        self.tdcStart = tdcStartStop[0]
        self.tdcStop = tdcStartStop[1]
        self.HOMs = HOMs
        self.counts = counts
        self.relatedChannelEntries = []
        self.QBERs = QBERs

    def powerMatched(self, threshold, ratio, singleMatch=None):
        if len(self.relatedChannelEntries) == 0: return False
        powers = self.relatedPowers()
        if singleMatch is not None:
            powers[1] = singleMatch
        actualRatio = 0 if powers[1] == 0 else powers[0] / powers[1] * ratio
        if (powers[0]>4.5) or (powers[1]>4.5): return False
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
            channelDatas = section['Monitor']
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
            if math.fabs(delta - 10) > 0.02:
                #print([self.entries[j].refTime - self.entries[j - 1].refTime for j in range(1, len(self.riseIndices))])
                raise RuntimeError("Error in ChannelMonitorSyncs: {}".format(delta))


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
        self.showCountChannelRelations('{}/CountChannelRelations'.format(resultDir))
        shutil.copyfile('{}/CountChannelRelations.png'.format(resultDir),
                        '{}/{}.png'.format(os.path.dirname(resultDir), os.path.basename(self.QBERFile)[:15]))
        self.showHOMs(0.8, np.logspace(-1.7, 1.7, num=100, endpoint=True, base=10.0),
                      '{}/HOMDip'.format(resultDir))
        self.parameters['QBERZZCorrect'] = sum([e.QBERs[(3 * 4 + 3) * 2 + (0)] for e in self.QBERs.entries])
        self.parameters['QBERZZWrong'] = sum([e.QBERs[(3 * 4 + 3) * 2 + (1)] for e in self.QBERs.entries])
        self.saveParameters('{}/meta.txt'.format(resultDir))
        self.saveQBERs(0.8, np.logspace(-1.7, 1.7, num=100, endpoint=True, base=10.0),
                      '{}/QBERs'.format(resultDir))

    def saveQBERs(self, shreshold, ratios, path, singleMatch=None):
        ratios = [r for r in ratios]
        file = open('{}.csv'.format(path), 'w')
        file.write(
            'ratio,filteredsection,totlasection,QBERCorrectOO,QBERWrongOO,QBERCorrectOX,QBERWrongOX,QBERCorrectOY,QBERWrongOY,QBERCorrectOZ,QBERWrongOZ,QBERCorrectXO,QBERWrongXO,QBERCorrectXX,QBERWrongXX,QBERCorrectXY,QBERWrongXY,QBERCorrectXZ,QBERWrongXZ,QBERCorrectYO,QBERWrongYO,QBERCorrectYX,QBERWrongYX,QBERCorrectYY,QBERWrongYY,QBERCorrectYZ,QBERWrongYZ,QBERCorrectZO,QBERWrongZO,QBERCorrectZX,QBERWrongZX,QBERCorrectZY,QBERWrongZY,QBERCorrectZZ,QBERWrongZZ\n')
        for r in ratios:
            file.write('{},'.format(r))
            QBER = self.getQBER(shreshold, r, singleMatch)
            for qber in QBER:
                file.write('{},'.format(qber))
            file.write('\n')
        file.close()

        print("show all QBERs: ")
        allQBEREntries = [e for e in self.QBERs.entries]
        allQBERs = [0] * 32
        for qberEntry in allQBEREntries:
            for i in range(0, 32):
                allQBERs[i] += qberEntry.QBERs[i]
        print(allQBERs)

    def getQBER(self, shreshold, ratio, singleMatch=None):
        # Careful!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        # HOMSection length extended to 6
        filteredQBEREntries = [e for e in self.QBERs.entries if e.powerMatched(shreshold, ratio, singleMatch)]
        QBEROOCorrect = sum([e.QBERs[(0 * 4 + 0) * 2 + (0)] for e in filteredQBEREntries])
        QBEROOWrong = sum([e.QBERs[(0 * 4 + 0) * 2 + (1)] for e in filteredQBEREntries])

        QBEROXCorrect = sum([e.QBERs[(0 * 4 + 1) * 2 + (0)] for e in filteredQBEREntries])
        QBEROXWrong = sum([e.QBERs[(0 * 4 + 1) * 2 + (1)] for e in filteredQBEREntries])

        QBEROYCorrect = sum([e.QBERs[(0 * 4 + 2) * 2 + (0)] for e in filteredQBEREntries])
        QBEROYWrong = sum([e.QBERs[(0 * 4 + 2) * 2 + (1)] for e in filteredQBEREntries])

        QBEROZCorrect = sum([e.QBERs[(0 * 4 + 3) * 2 + (0)] for e in filteredQBEREntries])
        QBEROZWrong = sum([e.QBERs[(0 * 4 + 3) * 2 + (1)] for e in filteredQBEREntries])

        QBERXOCorrect = sum([e.QBERs[(1 * 4 + 0) * 2 + (0)] for e in filteredQBEREntries])
        QBERXOWrong = sum([e.QBERs[(1 * 4 + 0) * 2 + (1)] for e in filteredQBEREntries])

        QBERXXCorrect = sum([e.QBERs[(1 * 4 + 1) * 2 + (0)] for e in filteredQBEREntries])
        QBERXXWrong = sum([e.QBERs[(1 * 4 + 1) * 2 + (1)] for e in filteredQBEREntries])

        QBERXYCorrect = sum([e.QBERs[(1 * 4 + 2) * 2 + (0)] for e in filteredQBEREntries])
        QBERXYWrong = sum([e.QBERs[(1 * 4 + 2) * 2 + (1)] for e in filteredQBEREntries])

        QBERXZCorrect = sum([e.QBERs[(1 * 4 + 3) * 2 + (0)] for e in filteredQBEREntries])
        QBERXZWrong = sum([e.QBERs[(1 * 4 + 3) * 2 + (1)] for e in filteredQBEREntries])

        QBERYOCorrect = sum([e.QBERs[(2 * 4 + 0) * 2 + (0)] for e in filteredQBEREntries])
        QBERYOWrong = sum([e.QBERs[(2 * 4 + 0) * 2 + (1)] for e in filteredQBEREntries])

        QBERYXCorrect = sum([e.QBERs[(2 * 4 + 1) * 2 + (0)] for e in filteredQBEREntries])
        QBERYXWrong = sum([e.QBERs[(2 * 4 + 1) * 2 + (1)] for e in filteredQBEREntries])

        QBERYYCorrect = sum([e.QBERs[(2 * 4 + 2) * 2 + (0)] for e in filteredQBEREntries])
        QBERYYWrong = sum([e.QBERs[(2 * 4 + 2) * 2 + (1)] for e in filteredQBEREntries])

        QBERYZCorrect = sum([e.QBERs[(2 * 4 + 3) * 2 + (0)] for e in filteredQBEREntries])
        QBERYZWrong = sum([e.QBERs[(2 * 4 + 3) * 2 + (1)] for e in filteredQBEREntries])

        QBERZOCorrect = sum([e.QBERs[(3 * 4 + 0) * 2 + (0)] for e in filteredQBEREntries])
        QBERZOWrong = sum([e.QBERs[(3 * 4 + 0) * 2 + (1)] for e in filteredQBEREntries])

        QBERZXCorrect = sum([e.QBERs[(3 * 4 + 1) * 2 + (0)] for e in filteredQBEREntries])
        QBERZXWrong = sum([e.QBERs[(3 * 4 + 1) * 2 + (1)] for e in filteredQBEREntries])

        QBERZYCorrect = sum([e.QBERs[(3 * 4 + 2) * 2 + (0)] for e in filteredQBEREntries])
        QBERZYWrong = sum([e.QBERs[(3 * 4 + 2) * 2 + (1)] for e in filteredQBEREntries])

        QBERZZCorrect = sum([e.QBERs[(3 * 4 + 3) * 2 + (0)] for e in filteredQBEREntries])
        QBERZZWrong = sum([e.QBERs[(3 * 4 + 3) * 2 + (1)] for e in filteredQBEREntries])

        return [
            len(filteredQBEREntries), len(self.QBERs.entries),

            QBEROOCorrect, QBEROOWrong,
            QBEROXCorrect, QBEROXWrong,
            QBEROYCorrect, QBEROYWrong,
            QBEROZCorrect, QBEROZWrong,

            QBERXOCorrect, QBERXOWrong,
            QBERXXCorrect, QBERXXWrong,
            QBERXYCorrect, QBERXYWrong,
            QBERXZCorrect, QBERXZWrong,

            QBERYOCorrect, QBERYOWrong,
            QBERYXCorrect, QBERYXWrong,
            QBERYYCorrect, QBERYYWrong,
            QBERYZCorrect, QBERYZWrong,

            QBERZOCorrect, QBERZOWrong,
            QBERZXCorrect, QBERZXWrong,
            QBERZYCorrect, QBERZYWrong,
            QBERZZCorrect, QBERZZWrong,
        ]

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
                if sidePowers[i] < 4.5:
                    validSidePowers.append(sidePowers[i])
                    validSideCounts.append(sideCounts[i])
            z = np.polyfit(validSidePowers, validSideCounts, 1)
            self.parameters['CountChannelRelations Fitting {} Slope'.format(kk)] = z[0]
            self.parameters['CountChannelRelations Fitting {} Intercept'.format(kk)] = z[1]

            ax1.scatter(sidePowers, sideCounts, s=2, color=colors[kk], label='Counts', alpha=0.2)
            ax1.plot(powerSteps, [z[1] + p * z[0] for p in powerSteps], 'black')
            ax1.text(powerSteps[-1], z[1] + powerSteps[-1] * z[0], '{}: {:.2f}'.format(labels[kk], z[0]), size='large',
                     **{'horizontalalignment': 'right', 'verticalalignment': 'baseline'})
            ax2.plot(powerSteps, entryCountHistogram, colors[kk], label=labels[kk])
        ax1.set_ylabel('APD Counts')
        ax1.set_xlabel('PD Power')
        ax2.set_ylabel('Frequencies')
        plt.legend()
        plt.savefig('{}.png'.format(path), dpi=300)
        plt.close()

    # def showPowerStatistic(self, path):
    #     filteredQBEREntries = [e for e in self.QBERs.entries if len(e.relatedChannelEntries) > 0]
    #     powers = [e.relatedPowers() for e in filteredQBEREntries]
    #     fig = plt.figure()
    #     ax1 = fig.add_subplot(111)
    #     ax2 = ax1.twinx()
    #     labels = ['Alice', 'Bob']
    #     colors = ['C0', 'C1']
    #     for kk in [0, 1]:
    #         sidePowers = [p[kk] for p in powers]
    #         binCount = 30
    #         maxPower = max(sidePowers)
    #         minPower = min(sidePowers)
    #     powerSteps = [(maxPower - minPower) / binCount * (i + 0.5) + minPower for i in range(0, binCount)]
    #     entryCountHistogram = [0] * binCount
    #     for i in range(0, len(sidePowers)):
    #         index = int((sidePowers[i] - minPower) / (maxPower - minPower) * binCount)
    #         if index == binCount: index -= 1
    #         entryCountHistogram[index] += 1
    #
    #     validSidePowers = []
    #     validSideCounts = []
    #     for i in range(len(sidePowers)):
    #         if sidePowers[i] < 4.8:
    #             validSidePowers.append(sidePowers[i])
    #             validSideCounts.append(sideCounts[i])
    #     z = np.polyfit(validSidePowers, validSideCounts, 1)
    #     self.parameters['CountChannelRelations Fitting {} Slope'.format(kk)] = z[0]
    #     self.parameters['CountChannelRelations Fitting {} Intercept'.format(kk)] = z[1]
    #
    #     ax1.scatter(sidePowers, sideCounts, s=2, color=colors[kk], label='Counts', alpha=0.2)
    #     ax1.plot(powerSteps, [z[1] + p * z[0] for p in powerSteps], 'black')
    #     ax1.text(powerSteps[-1], z[1] + powerSteps[-1] * z[0], '{}: {:.2f}'.format(labels[kk], z[0]), size='large',
    #              **{'horizontalalignment': 'right', 'verticalalignment': 'baseline'})
    #     ax2.plot(powerSteps, entryCountHistogram, colors[kk], label=labels[kk])
    # ax1.set_ylabel('APD Counts')
    # ax1.set_xlabel('PD Power')
    # ax2.set_ylabel('Frequencies')
    # plt.legend()
    # plt.savefig('{}.png'.format(path), dpi=300)
    # plt.close()

    def showHOMs(self, shreshold, ratios, path, singleMatch=None):
        ratios = [r for r in ratios]
        HOMDipXXs = []
        xxAccidents = []
        HOMDipYYs = []
        yyAccidents = []
        HOMDipAlls = []
        allAccidents = []
        QBERXXCorrect = []
        QBERXXWrong = []
        QBERYYCorrect = []
        QBERYYWrong = []
        QBERZZCorrect = []
        QBERZZWrong = []
        for r in ratios:
            # self.HOM returns [HOMDipXX, xxAccident, HOMDipYY, yyAccident, HOMDipAll, allAccident, QBERXXCorrect, QBERXXWrong, QBERYYCorrect, QBERYYWrong, QBERZZCorrect, QBERZZWrong]
            HOM = self.HOM(shreshold, r, singleMatch)
            HOMDipXXs.append(HOM[0])
            xxAccidents.append(HOM[1])
            HOMDipYYs.append(HOM[2])
            yyAccidents.append(HOM[3])
            HOMDipAlls.append(HOM[4])
            allAccidents.append(HOM[5])
            QBERXXCorrect.append(HOM[6])
            QBERXXWrong.append(HOM[7])
            QBERYYCorrect.append(HOM[8])
            QBERYYWrong.append(HOM[9])
            QBERZZCorrect.append(HOM[10])
            QBERZZWrong.append(HOM[11])

        self.parameters['Total Accident-All'] = sum([e.HOMs[3] for e in self.QBERs.entries])
        self.parameters['Total Accident-XX'] = sum([e.HOMs[1] for e in self.QBERs.entries])

        file = open('{}.csv'.format(path), 'w')
        file.write('ratio,HOM-All,Accidence-All,HOM-XX,Accidence-XX,HOM-YY,Accidence-YY,QBERCorrectXX,QBERWrongXX,QBERCorrectYY,QBERWrongYY,QBERCorrectZZ,QBERWrongZZ\n')
        for i in range(0, len(ratios)):
            file.write(
                '{},{},{},{},{},{},{},{},{},{},{},{},{}\n'.format(ratios[i], HOMDipAlls[i], allAccidents[i], HOMDipXXs[i], xxAccidents[i], HOMDipYYs[i], yyAccidents[i], QBERXXCorrect[i], QBERXXWrong[i],QBERYYCorrect[i],QBERYYWrong[i], QBERZZCorrect[i], QBERZZWrong[i]))
        file.close()

        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax1.semilogx(ratios, HOMDipAlls, label='HOM-All')
        ax1.set_ylabel('HOM Dip')
        ax1.set_xlabel('ratios')
        ax2 = ax1.twinx()
        ax2.semilogx(ratios, allAccidents, 'green', label='Side Coincidences')
        ax2.set_ylabel('Side Coincidences')
        plt.legend()
        plt.savefig('{}-all.png'.format(path), dpi=300)
        plt.close()

        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax1.semilogx(ratios, HOMDipXXs, label='HOM-XX')
        ax1.set_ylabel('HOM Dip')
        ax1.set_xlabel('ratios')
        ax2 = ax1.twinx()
        ax2.semilogx(ratios, xxAccidents, 'green', label='Side Coincidences')
        ax2.set_ylabel('Side Coincidences')
        plt.legend()
        plt.savefig('{}-xx.png'.format(path), dpi=300)
        plt.close()

    def saveParameters(self, path):
        file = open(path, 'w')
        for key in self.parameters.keys():
            file.write('{}: {}\n'.format(key, self.parameters[key]))
        file.close()

    def HOM(self, shreshold, ratio, singleMatch=None):
        # Careful!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        # HOMSection length extended to 6
        filteredQBEREntries = [e for e in self.QBERs.entries if e.powerMatched(shreshold, ratio, singleMatch)]
        xxDip = sum([e.HOMs[0] for e in filteredQBEREntries])
        xxAccident = sum([e.HOMs[1] for e in filteredQBEREntries])
        yyDip = sum([e.HOMs[2] for e in filteredQBEREntries])
        yyAccident = sum([e.HOMs[3] for e in filteredQBEREntries])
        allDip = sum([e.HOMs[4] for e in filteredQBEREntries])
        allAccident = sum([e.HOMs[5] for e in filteredQBEREntries])
        HOMDipXX = math.nan if xxAccident == 0 else xxDip / xxAccident
        HOMDipYY = math.nan if yyAccident == 0 else yyDip / yyAccident
        HOMDipAll = math.nan if allAccident == 0 else allDip / allAccident
        QBERXXCorrect = sum([e.QBERs[(1 * 4 + 1) * 2 + (0)] for e in filteredQBEREntries])
        QBERXXWrong = sum([e.QBERs[(1 * 4 + 1) * 2 + (1)] for e in filteredQBEREntries])
        QBERYYCorrect = sum([e.QBERs[(2 * 4 + 2) * 2 + (0)] for e in filteredQBEREntries])
        QBERYYWrong = sum([e.QBERs[(2 * 4 + 2) * 2 + (1)] for e in filteredQBEREntries])
        QBERZZCorrect = sum([e.QBERs[(3 * 4 + 3) * 2 + (0)] for e in filteredQBEREntries])
        QBERZZWrong = sum([e.QBERs[(3 * 4 + 3) * 2 + (1)] for e in filteredQBEREntries])
        return [HOMDipXX, xxAccident, HOMDipYY, yyAccident, HOMDipAll, allAccident, QBERXXCorrect, QBERXXWrong, QBERYYCorrect, QBERYYWrong, QBERZZCorrect, QBERZZWrong]

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
    import sys
    import os
    monitor = ParserMonitor('D:\\Experiments\\MDIQKD\\RealTimeData\\Dumped', 'D:\\Experiments\\MDIQKD\\RealTimeData\\Result', fromNow=True)
    if len(sys.argv) == 1:
        monitor.begin()
    else:
        monitor.parse(sys.argv[1:])