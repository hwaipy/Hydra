import Pydra
import time
import socket
import sys
import numpy as np
import matplotlib.pyplot as plt
import threading
import math
from math import isnan

class Syncer:
    def __init__(self, codeIndex):
        self.dTs = []
        self.maxLength = 20
        self.codeIndex = codeIndex

    def feedDT(self, dT):
        self.dTs.append(dT)
        while len(self.dTs) > self.maxLength:
            self.dTs = self.dTs[1:]
        if len(self.dTs) == self.maxLength:
            self.dTs = self.dTs[2:]
            xs = np.array([i for i in range(len(self.dTs))])
            ys = np.array(self.dTs)
            fitting = np.polyfit(xs, ys, 1)
            codeIncreasement = fitting[0]*1000/0.119
            dTFinal = fitting[1]
            self.dTs = []
            return [codeIncreasement, dTFinal]

class TimeSyncer:
    def __init__(self):
        self.session = Pydra.Session.newSession(('172.16.60.199', 20102), None, 'MDI-QKD-Time-Syncer')
        self.storage = self.session.blockingInvoker("StorageService")
        self.reportPath = '/test/tdc/mdireport.fs'
        self.currentTDCReportSize = -1
        self.dacAlice = self.session.blockingInvoker('FTDC-Alice')
        self.dacBob = self.session.blockingInvoker('FTDC-Bob')
        self.clockAlice = self.session.blockingInvoker('HMC7044EvalAlice')
        self.clockBob = self.session.blockingInvoker('HMC7044EvalBob')
        self.clockCharlie = self.session.blockingInvoker('HMC7044EvalCharlie')
        self.syncerAlice = Syncer(0.119)
        self.syncerBob = Syncer(0.105)
        self.delayTarget = 2

    def stop(self):
        self.session.stop()

    def test(self):
        # self.dacAlice.increaseCode(1, 3675) # negative if drift left
        while True:
            currentMeta = self.__getCurrentMeta()

            feedBackAlice = self.syncerAlice.feedDT(currentMeta[0])
            feedBackBob = self.syncerBob.feedDT(currentMeta[1])

            file = open('Syncer.csv', 'a')
            file.write('{}, {}\n'.format(currentMeta[0], currentMeta[1]))
            file.close()

            if feedBackAlice != None:
                print('Alice Feedback: {}'.format(feedBackAlice))
                if feedBackAlice[0] > 100 or feedBackAlice[0] < -100 or feedBackAlice[1] > 4 or feedBackAlice[1] < 0:
                    print('ALERT !!!!!!!!!!!! abandon.')
                else:
                    self.dacAlice.increaseCode(1, int(feedBackAlice[0])) # negative if drift left
                    Toffset = self.delayTarget - feedBackAlice[1]
                    if Toffset >= 0:
                        self.clockAlice.setDelay(0, Toffset)
                    else:
                        self.clockBob.setDelay(0, -Toffset)
                        self.clockCharlie.setDelay(0, -Toffset)
            if feedBackBob != None:
                print('Bob Feedback: {}'.format(feedBackBob))
                if feedBackBob[0] > 300 or feedBackBob[0] < -300 or feedBackBob[1] > 4 or feedBackBob[1] < 0:
                    print('ALERT !!!!!!!!!!!! abandon.')
                else:
                    self.dacBob.increaseCode(1, int(feedBackBob[0])) # negative if drift left
                    Toffset = self.delayTarget - feedBackBob[1]
                    if Toffset >= 0:
                        self.clockBob.setDelay(0, Toffset)
                    else:
                        self.clockAlice.setDelay(0, -Toffset)
                        self.clockCharlie.setDelay(0, -Toffset)


    def __getCurrentMeta(self):
        report = self.__getCurrentTDCReport()
        if report == None:
            return None
        else:
            return [report.get('aliceRise'), report.get('bobRise')]

    def __getCurrentTDCReport(self, timeout=5):
        startTime = time.time()
        while True:
            currentSize = int(self.storage.metaData("", self.reportPath, False)['Size'])
            if self.currentTDCReportSize == -1:
                self.currentTDCReportSize = currentSize
                continue
            if self.currentTDCReportSize >= currentSize:
                if timeout <= 0:
                    time.sleep(0.3)
                    continue
                else:
                    timePass = time.time() - startTime
                    if timePass > timeout:
                        return None
            else:
                self.currentTDCReportSize = currentSize
                break

        frameBytes = self.storage.FSFileReadTailFrames("", self.reportPath, 0, 1)[0]
        report = Pydra.Message.unpack(frameBytes)
        return report

if __name__ == '__main__':
    syncer = TimeSyncer()
    syncer.test()
    syncer.stop()