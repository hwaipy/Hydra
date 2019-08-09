import os
from datetime import datetime
import math
import matplotlib.pyplot as plt
import sys
import numpy as np


class Shower:
    def __init__(self, dir, showDir):
        self.dir = dir
        self.showDir = showDir
        if not os.path.exists(showDir): os.mkdir(showDir)

    def show(self):
        validResultDirs = self.__listValidResults()
        self.merge(validResultDirs)

    def merge(self, dirs):
        conditions = None
        mergedData = None
        for dir in dirs:
            file = open('{}/HOMandQBERs.csv'.format(dir), 'r')
            head = file.readline()[:-1]
            lines = file.readlines()[1:]
            file.close()
            data = np.array([[float(d) for d in line.split(',')] for line in lines])
            condition = data[:, :2]
            tobeMerged = data[:, 2:]
            if conditions is None:
                conditions = condition
                mergedData = tobeMerged
            else:
                mergedData += tobeMerged

        data = np.hstack((conditions, mergedData))

        file = open('{}/HOMandQBERs.csv'.format(self.showDir), 'w')
        file.write('{}\n'.format(head))
        for row in range(conditions.shape[0]):
            file.write('{}\n'.format(', '.join([str(d) for d in data[row]])))
        file.close()

        thresholds = list(set(data[:, 0]))
        thresholds.sort()
        for threshold in thresholds:
            self.__plot((data, head), threshold, 'HOM', 'XXDip', 'XXAct', 'XX', True)
            self.__plot((data, head), threshold, 'HOM', 'YYDip', 'YYAct', 'YY', True)
            self.__plot((data, head), threshold, 'HOM', 'AllDip', 'AllAct', 'All', True)
            self.__plot((data, head), threshold, 'QBER', 'XX Correct', 'XX Wrong', 'XX', True)
            self.__plot((data, head), threshold, 'QBER', 'YY Correct', 'YY Wrong', 'YY', True)
            self.__plot((data, head), threshold, 'QBER', 'ZZ Correct', 'ZZ Wrong', 'ZZ', True)

    def __plot(self, dataAndHead, threshold, mode, head1, head2, bases, save):
        data = np.array([d for d in dataAndHead[0] if d[0] == threshold])
        head = [h.strip() for h in dataAndHead[1].split(',')]
        ratios = data[:, 1]
        c1 = data[:, head.index(head1)]
        c2 = data[:, head.index(head2)]
        if mode == 'HOM':
            y2 = c2
            y1 = c1 / (c2 + 1e-10)
            y1Label = 'HOM Dip ({})'.format(bases)
            y2Label = 'Side Coincidences'
            saveFileName = 'HOM-r{}-{}.png'.format(threshold, bases)
        elif mode == 'QBER':
            y2 = c1 + c2
            y1 = c2 / (y2 + 1e-10)
            y1Label = 'QBER ({})'.format(bases)
            y2Label = 'Coincidences'
            saveFileName = 'QBER-r{}-{}.png'.format(threshold, bases)
        else:
            raise RuntimeError('Mode not valid.')

        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        ax1.semilogx(ratios, y1, label=y1Label)
        ax1.set_ylabel(y1Label)
        ax1.set_xlabel('ratios')
        ax1.grid(True, which="both", color="k", ls="--", lw=0.3)
        ax2 = ax1.twinx()
        ax2.semilogx(ratios, y2, 'green', label=y2Label)
        ax2.set_ylabel(y2Label)
        plt.legend()
        if save:
            plt.savefig('{}/{}'.format(self.showDir, saveFileName), dpi=300)
        else:
            plt.show()
        plt.close()

    def __listValidResults(self):
        files = [f for f in os.listdir(self.dir) if f.lower().endswith('.png')]
        files.sort()
        dirs = ['{}/{}'.format(self.dir, file[:15]) for file in files]
        return dirs


if __name__ == '__main__':
    resultDir = 'E:\\MDIQKD_Parse\\RawData\\20190731\\run-1-parsed' if len(sys.argv) == 1 else sys.argv[1]
    shower = Shower(resultDir, '{}/Summary'.format(resultDir))
    shower.show()
