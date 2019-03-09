import sys
import Utils
import Pydra
import time
from scipy import optimize
import threading

class FittingStrategy:
    def __init__(self):
        self.historyLength = 6
        self.historyPriorities = [4, 10, 20, 50, 80, 100]
        self.histories = []
        self.predict = 0
        self.fitParas = None

    def __func(self, time, A, B):
        return A * time + B

    def update(self, time, delta):
        if delta is not None:
            self.histories.append([time, delta + self.predict])
            if len(self.histories) > self.historyLength:
                self.histories.__delitem__(0)
            self.__updateFitting()
        if self.fitParas is not None:
            self.predict = self.__func(time, self.fitParas[0], self.fitParas[1])
        return self.predict

    def __updateFitting(self):
        if len(self.histories) < 2: return
        newEntries = []
        for i in range(len(self.histories)):
            newEntries += ([self.histories[i]] * self.historyPriorities[i])
        times = [e[0] for e in newEntries]
        deltas = [e[1] for e in newEntries]
        self.fitParas = optimize.curve_fit(self.__func, times, deltas)[0]

    def debugPlotData(self, currentTime):
        d1 = [e[0] for e in self.histories], [e[1] for e in self.histories]
        d2 = [[], []]
        if len(self.histories) > 0 and self.fitParas is not None:
            times = [(currentTime - self.histories[0][0]) / 1000 * i + self.histories[0][0] for i in range(0, 1000)]
            predicts = [self.__func(time, self.fitParas[0], self.fitParas[1]) for time in times]
            d2 = [times, predicts]
        return d1, d2

class EchoStrategy:
    def __init__(self):
        print('new echo strategy')
        self.delay = 0

    def update(self, time, delta):
        if delta is not None:
            self.delay = delta
        return self.delay

class SyncService():
    def __init__(self):
        self.strategyAlice = EchoStrategy()
        self.strategyBob = EchoStrategy()
        self.aliceOn = False
        self.bobOn = False
        self.HMC7044EvalAlice = None
        self.HMC7044EvalBob = None
        self.HMC7044EvalCharlie = None
        self.predictAlice = 0
        self.predictBob = 0
        self.channelAlice = 0
        self.channelBob = 0
        self.channelCharlie = 0
        threading.Thread(target=self.__run).start()

    def __run(self):
        while True:
            time.sleep(1)
            currentTime = time.time()
            aliceDelay = self.predictAlice
            bobDelay = self.predictBob
            charlieDelay = 0
            if self.aliceOn:
                newPredictAlice = self.strategyAlice.update(currentTime, None)
                aliceDelay = newPredictAlice - self.predictAlice
                self.predictAlice = newPredictAlice
            if self.bobOn:
                newPredictBob = self.strategyBob.update(currentTime, None)
                bobDelay = newPredictBob - self.predictBob
                self.predictBob = newPredictBob
            if aliceDelay < 0:
                bobDelay -= aliceDelay
                charlieDelay -= aliceDelay
                aliceDelay = 0
            if bobDelay < 0:
                aliceDelay -= bobDelay
                charlieDelay -= bobDelay
                bobDelay = 0
            if self.aliceOn or self.bobOn:
                self.log('adjust delays: alice {}, bob {}, charlie {}.'.format(aliceDelay, bobDelay, charlieDelay))
                print('adjust delays: alice {}, bob {}, charlie {}.'.format(aliceDelay, bobDelay, charlieDelay))
                try:
                    self.HMC7044EvalAlice.setDelay(self.channelAlice, aliceDelay)
                    self.HMC7044EvalBob.setDelay(self.channelBob, bobDelay)
                    self.HMC7044EvalCharlie.setDelay(self.channelCharlie, charlieDelay)
                except BaseException as e:
                    self.log('Error in set delays: {}.'.format(e))

    def updateAliceDelay(self, delay):
        self.strategyAlice.update(time.time(), -delay)
        self.log('Alice Delay updated: {}'.format(-delay))
        print('Alice Delay updated: {}'.format(-delay))

    def updateBobDelay(self, delay):
        self.strategyBob.update(time.time(), -delay)
        self.log('Bob Delay updated: {}'.format(-delay))

    def turnOnAlice(self, channel):
        print('turn on alice')
        self.aliceOn = True
        self.strategyAlice = FittingStrategy()
        self.predictAlice = 0
        self.channelAlice = channel

    def turnOffAlice(self):
        print('turn off alice')
        self.aliceOn = False

    def turnOnBob(self, channel):
        print('turn on bob')
        self.bobOn = True
        self.strategyBob = FittingStrategy()
        self.predictBob = 0
        self.channelBob = channel

    def turnOffBob(self):
        print('turn off bob')
        self.bobOn = False

    def configCharlie(self, channel):
        self.channelCharlie = channel

    def log(self, msg):
        file = open('log.txt', mode='a')
        file.write('[{}]{}'.format(time.time(), msg))
        if msg[-1] is not '\n':
            file.write('\n')
        file.close()


if __name__ == '__main__':
    sysArgs = Utils.SystemArguments(sys.argv)
    server = sysArgs.get('server', '192.168.25.27')
    port = sysArgs.get('port', '20102')
    clientName = sysArgs.get('clientName', 'SyncService')

    invoker = SyncService()
    session = Pydra.Session.newSession((server, int(port)), invoker, clientName)
    invoker.HMC7044EvalAlice = session.blockingInvoker('HMC7044EvalAlice')
    invoker.HMC7044EvalBob = session.blockingInvoker('HMC7044EvalBob')
    invoker.HMC7044EvalCharlie = session.blockingInvoker('HMC7044EvalCharlie')


    # while True:
    #     invoker.HMC7044EvalBob.setDelay(0, 0.05)
    #     time.sleep(3)
    # invoker.HMC7044EvalCharlie.setDelay(0, 0)
    # session.stop()