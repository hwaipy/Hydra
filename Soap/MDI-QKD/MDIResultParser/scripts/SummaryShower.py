import os
from datetime import datetime
import math
import matplotlib.pyplot as plt
import sys
import numpy as np

NOT_GOOD_CORRECTION = {'20190731': {'XX Correct': 0.782, 'YY Wrong': 0.75, 'ZZ Correct': 0.787}}


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
            head = head = [h.strip() for h in file.readline()[:-1].split(',')]
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

        date = [i for i in self.dir.replace('\\','/').split('/') if len(i) > 0][-2]
        if NOT_GOOD_CORRECTION.__contains__(date):
            print('Correcting {}'.format(self.dir))
            corrections = NOT_GOOD_CORRECTION[date]
            for c in corrections:
                data[:, head.index(c)] *= corrections[c]

        file = open('{}/HOMandQBERs.csv'.format(self.showDir), 'w')
        file.write('{}\n'.format(', '.join(head)))
        for row in range(conditions.shape[0]):
            file.write('{}\n'.format(', '.join([str(d) for d in data[row]])))
        file.close()

        thresholds = list(set(data[:, 0]))
        thresholds.sort()
        for threshold in [max(thresholds)]:
            selectedData = np.array([d for d in data if d[0] == threshold])
            for bases in ['XX', 'YY', 'All']:
                self.__plot(selectedData, head, threshold, 'Threshold', 'Ratio', 'HOM', '{}Dip'.format(bases),
                            '{}Act'.format(bases), bases, True)
            for bases in ['XX', 'YY', 'ZZ']:
                self.__plot(selectedData, head, threshold, 'Threshold', 'Ratio', 'QBER', '{} Correct'.format(bases),
                            '{} Wrong'.format(bases), bases, True)

        selectedRatio = self.__min(np.array([d for d in data if d[0] == max(thresholds)]), head, 'HOM',
                            'AllDip', 'AllAct')[0]
        selectedData = np.array([row for row in data if row[1] == selectedRatio])
        for bases in ['XX', 'YY', 'All']:
            self.__plot(selectedData, head, selectedRatio, 'Ratio', 'Threshold', 'HOM', '{}Dip'.format(bases),
                        '{}Act'.format(bases), bases, True, False)
        for bases in ['XX', 'YY', 'ZZ']:
            self.__plot(selectedData, head, selectedRatio, 'Ratio', 'Threshold', 'QBER', '{} Correct'.format(bases),
                        '{} Wrong'.format(bases), bases, True, False)

    def __plot(self, data, head, eigenValue, eigenValueName, independentName, mode, head1, head2, bases, save,
               log=True):
        independents = data[:, head.index(independentName)]
        c1 = data[:, head.index(head1)]
        c2 = data[:, head.index(head2)]
        saveFileName = '{}-{}={}-{}.png'.format(mode, eigenValueName, eigenValue, bases)
        if mode == 'HOM':
            y2 = c2
            y1 = c1 / (c2 + 1e-10)
            y1Label = 'HOM Dip ({})'.format(bases)
            y2Label = 'Side Coincidences'
        elif mode == 'QBER':
            y2 = c1 + c2
            y1 = c2 / (y2 + 1e-10)
            y1Label = 'QBER ({})'.format(bases)
            y2Label = 'Coincidences'
        else:
            raise RuntimeError('Mode not valid.')

        fig = plt.figure()
        ax1 = fig.add_subplot(111)
        if log:
            ax1.semilogx(independents, y1, label=y1Label)
        else:
            ax1.plot(independents, y1, label=y1Label)
        ax1.set_ylabel(y1Label)
        ax1.set_xlabel(independentName)
        ax1.grid(True, which="both", color="k", ls="--", lw=0.3)
        ax2 = ax1.twinx()
        if log:
            ax2.semilogx(independents, y2, 'green', label=y2Label)
        else:
            ax2.plot(independents, y2, 'green', label=y2Label)
        ax2.set_ylabel(y2Label)
        plt.legend()
        if save:
            plt.savefig('{}/{}'.format(self.showDir, saveFileName), dpi=300)
        else:
            plt.show()
        plt.close()

    def __min(self, data, head, mode, head1, head2):
        ratios = data[:, 1]
        c1 = data[:, head.index(head1)]
        c2 = data[:, head.index(head2)]
        if mode == 'HOM':
            y = c1 / (c2 + 1e-10)
        elif mode == 'QBER':
            y = c2 / (c1 + c2 + 1e-10)
        else:
            raise RuntimeError('Mode not valid.')
        z = [z for z in zip(y, ratios)]
        z.sort()
        validZ = [zz for zz in z if zz[1]>0.1 and zz[1]<10][0]
        return (validZ[1], validZ[0])

    def __listValidResults(self):
        files = [f for f in os.listdir(self.dir) if f.lower().endswith('.png')]
        files.sort()
        dirs = ['{}/{}'.format(self.dir, file[:15]) for file in files]
        return dirs


if __name__ == '__main__':
    resultDir = 'E:\\MDIQKD_Parse\\RawData/20190807/run-C-parsed/' if len(sys.argv) == 1 else sys.argv[1]
    shower = Shower(resultDir, '{}/Summary'.format(resultDir))
    shower.show()

    # root = 'E:\\MDIQKD_Parse\\RawData\\'
    # dates = ['{}\\{}'.format(root, file) for file in os.listdir(root) if not file.startswith("_")]
    # runs = []
    # for date in dates:
    #     runsInDay = ['{}\\{}'.format(date, file) for file in os.listdir(date) if file.endswith('parsed')]
    #     runs += runsInDay
    # print('{} runs to parse.'.format(len(runs)))
    #
    # for run in runs:
    #     shower = Shower(run, '{}/Summary'.format(run))
    #     shower.show()
