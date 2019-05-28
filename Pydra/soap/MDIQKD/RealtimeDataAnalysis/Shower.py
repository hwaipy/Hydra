import os
from datetime import datetime
import math
import matplotlib.pyplot as plt


class Shower:
    def __init__(self, start, stop, dir, showDir):
        self.start = datetime.strptime(start, "%Y%m%d-%H%M%S")
        self.stop = datetime.strptime(stop, "%Y%m%d-%H%M%S")
        self.dir = dir
        self.showDir = showDir

    def show(self):
        validResultDirs = self.__listValidResults()
        self.mergeHOM(validResultDirs)
        self.mergeMeta(validResultDirs)

    def mergeHOM(self, dirs):
        ratios = None
        cAll = None
        aAll = None
        cXX = None
        aXX = None
        for dir in dirs:
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
            cAll = self.__mergeList(cAll, [d[1] * d[2] for d in data])
            aAll = self.__mergeList(aAll, [d[2] for d in data])
            cXX = self.__mergeList(cXX, [d[3] * d[4] for d in data])
            aXX = self.__mergeList(aXX, [d[4] for d in data])

        HOMDipXXs = []
        HOMDipAlls = []
        for i in range(0, len(ratios)):
            HOMDipXXs.append(0 if aXX[i] == 0 else cXX[i] / aXX[i])
            HOMDipAlls.append(0 if aAll[i] == 0 else cAll[i] / aAll[i])

        file = open('{}/HOM.csv'.format(self.showDir), 'w')
        file.write('ratio,HOM-All,Accidence-All,HOM-XX,Accidence-XX\n')
        for i in range(0, len(ratios)):
            file.write('{},{},{},{},{}\n'.format(ratios[i], HOMDipAlls[i], aAll[i], HOMDipXXs[i], aXX[i]))
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

        file = open('{}/meta.txt'.format(self.showDir), 'w')
        file.write('Total Accident-All: {}\n'.format(aAll))
        file.write('Total Accident-XX: {}\n'.format(aXX))
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
        dirs = []
        for e in validEntries:
            dir = '{}/{}'.format(self.dir, e[0][:15])
            if os.path.exists(dir):
                dirs.append(dir)
        return dirs


if __name__ == '__main__':
    start = '20190524-031633'
    stop = '20200101-000000'

    shower = Shower(start, stop,
                    '/Users/Hwaipy/Downloads/MDI-Result/results',
                    '/Users/Hwaipy/Downloads/MDI-Result/show')
    shower.show()
