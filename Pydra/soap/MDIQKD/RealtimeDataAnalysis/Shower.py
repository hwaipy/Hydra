import os
from datetime import datetime
import math
import matplotlib.pyplot as plt

import sys
import os

debug = sys.platform == 'darwin'


class Shower:
    def __init__(self, start, stop, dir, showDir):
        self.start = datetime.strptime(start, "%Y%m%d-%H%M%S")
        self.stop = datetime.strptime(stop, "%Y%m%d-%H%M%S")
        self.dir = dir
        self.showDir = showDir

    def show(self):
        validResultDirs = self.__listValidResults()
        self.mergeHOM(validResultDirs)
        self.mergeQBER(validResultDirs)
        self.mergeMeta(validResultDirs)

    def mergeHOM(self, dirs):
        ratios = None
        cAll = None
        aAll = None
        cXX = None
        aXX = None
        xxCorrect = None
        xxWrong = None
        yyCorrect = None
        yyWrong = None
        zzCorrect = None
        zzWrong = None
        for dir in dirs:
            if not os.path.exists('{}/HOMDip.csv'.format(dir)):
                print(dir)
                print('run later')
                import sys
                sys.exit()
            file = open('{}/HOMDip.csv'.format(dir), 'r')
            lines = file.readlines()[1:]
            file.close()
            data = [[float(d) for d in line.split(',')] for line in lines]
            if ratios is None:
                ratios = [d[0] for d in data]
                cAll = [0] * len(ratios)
                aAll = [0] * len(ratios)
                cXX = [0] * len(ratios)
                aXX = [0] * len(ratios)
                cYY = [0] * len(ratios)
                aYY = [0] * len(ratios)
                xxCorrect = [0] * len(ratios)
                xxWrong = [0] * len(ratios)
                yyCorrect = [0] * len(ratios)
                yyWrong = [0] * len(ratios)
                zzCorrect = [0] * len(ratios)
                zzWrong = [0] * len(ratios)
            cAll = self.__mergeList(cAll, [d[1] * d[2] for d in data])
            aAll = self.__mergeList(aAll, [d[2] for d in data])
            cXX = self.__mergeList(cXX, [d[3] * d[4] for d in data])
            aXX = self.__mergeList(aXX, [d[4] for d in data])
            cYY = self.__mergeList(cYY, [d[5] * d[6] for d in data])
            aYY = self.__mergeList(aYY, [d[6] for d in data])
            xxCorrect = self.__mergeList(xxCorrect, [d[7] for d in data])
            xxWrong = self.__mergeList(xxWrong, [d[8] for d in data])
            yyCorrect = self.__mergeList(yyCorrect, [d[9] for d in data])
            yyWrong = self.__mergeList(yyWrong, [d[10] for d in data])
            zzCorrect = self.__mergeList(zzCorrect, [d[11] for d in data])
            zzWrong = self.__mergeList(zzWrong, [d[12] for d in data])

        HOMDipXXs = []
        HOMDipYYs = []
        HOMDipAlls = []
        for i in range(0, len(aXX)):
            HOMDipXXs.append(0 if aXX[i] == 0 else cXX[i] / aXX[i])
            HOMDipYYs.append(0 if aYY[i] == 0 else cYY[i] / aYY[i])
            HOMDipAlls.append(0 if aAll[i] == 0 else cAll[i] / aAll[i])

        file = open('{}/HOM.csv'.format(self.showDir), 'w')
        file.write(
            'ratio,HOM-All,Accidence-All,HOM-XX,Accidence-XX,HOM-YY,Accidence-YY,QBERCorrectXX,QBERWrongXX,QBERCorrectYY,QBERWrongYY,QBERCorrectZZ,QBERWrongZZ\n')
        for i in range(0, len(ratios)):
            file.write(
                '{},{},{},{},{},{},{},{},{},{},{},{},{}\n'.format(ratios[i], HOMDipAlls[i], aAll[i], HOMDipXXs[i],
                                                                  aXX[i], HOMDipYYs[i], aYY[i], xxCorrect[i],
                                                                  xxWrong[i], yyCorrect[i], yyWrong[i], zzCorrect[i],
                                                                  zzWrong[i]))
        file.close()

        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax1.semilogx(ratios, HOMDipAlls, label='HOM-All')
        ax1.set_ylabel('HOM Dip')
        ax1.set_xlabel('ratios')
        ax2 = ax1.twinx()
        ax2.semilogx(ratios, aAll, 'green', label='Side Coincidences')
        ax2.set_ylabel('Side Coincidences')
        plt.legend()
        plt.savefig('{}/HOMDip-all.png'.format(self.showDir), dpi=300)
        plt.close()

        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax1.semilogx(ratios, HOMDipXXs, label='HOM-XX')
        ax1.set_ylabel('HOM Dip')
        ax1.set_xlabel('ratios')
        ax2 = ax1.twinx()
        ax2.semilogx(ratios, aXX, 'green', label='Side Coincidences')
        ax2.set_ylabel('Side Coincidences')
        plt.legend()
        plt.savefig('{}/HOMDip-xx.png'.format(self.showDir), dpi=300)
        plt.close()

        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax1.semilogx(ratios, HOMDipYYs, label='HOM-YY')
        ax1.set_ylabel('HOM Dip')
        ax1.set_xlabel('ratios')
        ax2 = ax1.twinx()
        ax2.semilogx(ratios, aYY, 'green', label='Side Coincidences')
        ax2.set_ylabel('Side Coincidences')
        plt.legend()
        plt.savefig('{}/HOMDip-yy.png'.format(self.showDir), dpi=300)
        plt.close()

        zzTotal = []
        zzQBER = []
        for i in range(0, len(zzCorrect)):
            zzTotal.append(zzCorrect[i] + zzWrong[i])
            if zzWrong[i] + zzCorrect[i] == 0:
                zzQBER.append(0)
            else:
                zzQBER.append(zzWrong[i] / (zzWrong[i] + zzCorrect[i]))
        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax1.semilogx(ratios, zzQBER, label='QBER-ZZ')
        ax1.set_ylabel('QBER')
        ax1.set_xlabel('ratios')
        ax2 = ax1.twinx()
        ax2.semilogx(ratios, zzTotal, 'green', label='Side Coincidences')
        ax2.set_ylabel('Side Coincidences')
        plt.legend()
        plt.savefig('{}/QBER-ZZ.png'.format(self.showDir), dpi=300)
        plt.close()

        xxTotal = []
        xxQBER = []
        for i in range(0, len(xxCorrect)):
            xxTotal.append(xxCorrect[i] + xxWrong[i])
            if xxWrong[i] + xxCorrect[i] > 0:
                xxQBER.append(xxWrong[i] / (xxWrong[i] + xxCorrect[i]))
            else:
                xxQBER.append(0)
        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax1.semilogx(ratios, xxQBER, label='QBER-XX')
        ax1.set_ylabel('QBER')
        ax1.set_xlabel('ratios')
        ax2 = ax1.twinx()
        ax2.semilogx(ratios, xxTotal, 'green', label='Side Coincidences')
        ax2.set_ylabel('Side Coincidences')
        plt.legend()
        plt.savefig('{}/QBER-XX.png'.format(self.showDir), dpi=300)
        plt.close()

        yyTotal = []
        yyQBER = []
        for i in range(0, len(yyCorrect)):
            yyTotal.append(yyCorrect[i] + yyWrong[i])
            if yyWrong[i] + yyCorrect[i] > 0:
                yyQBER.append(yyWrong[i] / (yyWrong[i] + yyCorrect[i]))
            else:
                yyQBER.append(0)
        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax1.semilogx(ratios, yyQBER, label='QBER-YY')
        ax1.set_ylabel('QBER')
        ax1.set_xlabel('ratios')
        ax2 = ax1.twinx()
        ax2.semilogx(ratios, yyTotal, 'green', label='Side Coincidences')
        ax2.set_ylabel('Side Coincidences')
        plt.legend()
        plt.savefig('{}/QBER-YY.png'.format(self.showDir), dpi=300)
        plt.close()

    def mergeQBER(self, dirs):
        def parseLine(line):
            items = line.split(',')[:-1]
            return [float(item) for item in items]

        def loadQBERData(dir):
            file = open('{}/QBER.csv'.format(dir), 'r')
            lines = file.readlines()[1:]
            return [parseLine(line) for line in lines]

        data = [loadQBERData(dir) for dir in dirs]
        merged = [[0] * len(data[0][0]) for i in range(0, len(data[0]))]
        for d in data:
            for row in range(0, len(d)):
                for column in range(0, len(d[0])):
                    merged[row][column] += d[row][column]
        for r in range(0, len(merged)):
            merged[r][0] /= len(data)

        file = open('{}/QBER.csv'.format(self.showDir), 'w')
        file.write(
                        'ratio,filteredsection,totlasection,QBERCorrectOO,QBERWrongOO,QBERCorrectOX,QBERWrongOX,QBERCorrectOY,QBERWrongOY,QBERCorrectOZ,QBERWrongOZ,QBERCorrectXO,QBERWrongXO,QBERCorrectXX,QBERWrongXX,QBERCorrectXY,QBERWrongXY,QBERCorrectXZ,QBERWrongXZ,QBERCorrectYO,QBERWrongYO,QBERCorrectYX,QBERWrongYX,QBERCorrectYY,QBERWrongYY,QBERCorrectYZ,QBERWrongYZ,QBERCorrectZO,QBERWrongZO,QBERCorrectZX,QBERWrongZX,QBERCorrectZY,QBERWrongZY,QBERCorrectZZ,QBERWrongZZ\n')
        for row in merged:
            line = ('{}'.format(row))[1:-1]
            file.write('{}\n'.format(line))
        file.close()


    def mergeMeta(self, dirs):
        metas = []
        for dir in dirs:
            file = open('{}/meta.txt'.format(dir), 'r')
            lines = file.readlines()
            file.close()
            meta = {}
            for line in lines:
                split = line[:-1].split(': ')
                meta[split[0]] = float(split[1])
            metas.append(meta)
        aAll = sum([meta['Total Accident-All'] for meta in metas])
        aXX = sum([meta['Total Accident-XX'] for meta in metas])
        QBERZZCorrect = sum([meta['QBERZZCorrect'] for meta in metas])
        QBERZZWrong = sum([meta['QBERZZWrong'] for meta in metas])

        file = open('{}/meta.txt'.format(self.showDir), 'w')
        file.write('Total Accident-All: {}\n'.format(aAll))
        file.write('Total Accident-XX: {}\n'.format(aXX))
        file.write('QBERZZCorrect: {}\n'.format(QBERZZCorrect))
        file.write('QBERZZWrong: {}\n'.format(QBERZZWrong))
        file.close()

    def __mergeList(self, list1, list2):
        if len(list1) != len(list2): raise Exception
        list = []
        for i in range(0, len(list1)):
            l1 = list1[i]
            l2 = list2[i]
            if math.isnan(l1): l1 = 0
            if math.isnan(l2): l2 = 0
            list.append(l1 + l2)
        return list

    def __listValidResults(self):
        files = [f for f in os.listdir(self.dir) if f.lower().endswith('.png')]
        files.sort()
        entries = [(f, datetime.strptime(f[:15], "%Y%m%d-%H%M%S")) for f in files]
        validEntries = [e for e in entries if e[1] > self.start and e[1] < self.stop]
        print('{} valid files for parse.'.format(len(validEntries)))
        dirs = []
        for e in validEntries:
            dir = '{}/{}'.format(self.dir, e[0][:15])
            if os.path.exists(dir):
                dirs.append(dir)
        return dirs


if __name__ == '__main__':
    start = '20190718-024500'
    stop = '20190718-090000'

    resultDir = '/Users/Hwaipy/Desktop/MDI/results' if debug else 'D:\\Experiments\\MDIQKD\\RealTimeData\\Result'

    shower = Shower(start, stop, '{}/results'.format(resultDir), '{}/Show'.format(resultDir))
    shower.show()
