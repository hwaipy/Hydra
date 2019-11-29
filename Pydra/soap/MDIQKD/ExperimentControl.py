import Pydra
import time
import socket
import sys
import numpy as np
import matplotlib.pyplot as plt
import math
from math import isnan
import os
import threading
import queue

class ExperimentControl:
    def __init__(self, randomNumbersAlice, randomNumbersBob, configRoot):
        self.session = Pydra.Session.newSession(('172.16.60.199', 20102), None, 'MDI-QKD-Controller-H')
        self.groundTDCService = self.session.blockingInvoker('GroundTDCService')
        self.clockAlice = self.session.blockingInvoker('HMC7044EvalAlice')
        self.clockBob = self.session.blockingInvoker('HMC7044EvalBob')
        self.clockCharlie = self.session.blockingInvoker('HMC7044EvalCharlie')
        self.randomNumbersAlice = randomNumbersAlice
        self.randomNumbersBob = randomNumbersBob
        self.configRoot = configRoot
        self.__awgParameters = {}
        self.__dcParameters = {}

    def stop(self):
        self.session.stop()

    def initTDCServer(self):
        self.groundTDCService.configureAnalyser('MDIQKDEncoding', {'BinCount': 1000})
        self.groundTDCService.configureAnalyser("MDIQKDEncoding", {"SignalChannel": 8})
        self.groundTDCService.configureAnalyser("MDIQKDQBER", {"Channel 1": 8, "Channel 2": 9})  # 设置TDC通道改变1/8,2/9
        self.groundTDCService.configureAnalyser("MDIQKDQBER", {
            "AliceRandomNumbers": self.randomNumbersAlice,
            "BobRandomNumbers": self.randomNumbersBob})

    def loadConfig(self):
        self.tdcConfig = VolatileConfiguration('{}/TDC.config'.format(self.configRoot), self.__TDCConfigChanged)
        self.dcConfig = VolatileConfiguration('{}/DC.config'.format(self.configRoot), self.__DCConfigChanged)
        self.awgConfig = VolatileConfiguration('{}/AWG.config'.format(self.configRoot), self.__AWGConfigChanged, False)

    def setTDCDelay(self, channel, delay):
        self.groundTDCService.setDelay(channel, int(delay * 1000))

    def setDC(self, serviceName, channel, value):
        self.session.blockingInvoker(serviceName).setVoltage(channel, value)

    def setChannelMode(self, mode):
        if not (['Alice', 'Bob', 'Both'].__contains__(mode)): print('Wrong Mode: {}'.format(mode))
        else:
            self.setChannelEnabled('Alice', mode == 'Alice' or mode == 'Both')
            self.setChannelEnabled('Bob', mode == 'Bob' or mode == 'Both')
            randomNumber = self.randomNumbersAlice if (mode == 'Alice' or mode == 'Both') else self.randomNumbersBob
            self.groundTDCService.configureAnalyser("MDIQKDEncoding", {"RandomNumbers": randomNumber, "SignalChannel": 9})

    def setChannelEnabled(self, target, enabled):
        dc = self.session.blockingInvoker('DC-MDI-Alice-Time1' if target == 'Alice' else 'DC-MDI-Bob-Time')
        # para = ('ENABLED_FP' if self.__awgParameters.get('FIRST_PULSE_MODE') == 'True' else 'ENABLED') if enabled else 'DISABLED'
        # dc.setVoltage(2, float(self.__dcParameters['DC_ATT_{}_{}'.format(target.upper(), para)]))
        dc.setVoltage(2, float(self.__dcParameters['DC_ATT_{}'.format(target.upper())]) if enabled else 6)

    def setDelay(self, target, delay):
        targetClock = self.clockAlice if target == 'Alice' else self.clockBob
        reversedClock = self.clockBob if target == 'Alice' else self.clockAlice
        if delay >= 0:
            targetClock.setDelay(0, delay)
        else:
            reversedClock.setDelay(0, -delay)
            self.clockCharlie.setDelay(0, -delay)

    def __TDCConfigChanged(self, key, value):
        print('TDCConfig {} changed to {}'.format(key, value))
        if key.startswith('DELAY_'):
            channel = int(key[6:])
            ec.setTDCDelay(channel, float(value))
        else:
            print('Unrecognized config key: {}'.format(key))

    def __DCConfigChanged(self, key, value):
        print('DCConfig {} changed to {}'.format(key, value))
        channels = {
            # 'BIAS_ALICE_DECOY': ['None', 0],
            'BIAS_ALICE_TIME0': ['DC-MDI-Alice-Time1', 1],
            'BIAS_ALICE_TIME1': ['DC-MDI-Alice-Time1', 0],
            # 'BIAS_BOB_DECOY': ['None', 0],
            'BIAS_BOB_TIME0': ['DC-MDI-Bob-Time', 0],
            'BIAS_BOB_TIME1': ['DC-MDI-Bob-Time', 1],
            'DC_ATT_ALICE': ['DC-MDI-Alice-Time1', 2],
            'DC_ATT_BOB': ['DC-MDI-Bob-Time', 2],
        }
        target = channels.get(key)
        if not target == None:
            ec.setDC(target[0], target[1], float(value))
        elif key == 'CHANNEL_MODE':
            self.setChannelMode(value)
        else:
            print('Unrecognized config key: {}'.format(key))
        if key.startswith('DC_ATT_'):
            self.__dcParameters[key] = value

    def __AWGConfigChanged(self, newConfig):
        print('SRCConfig changed.')
        self.__awgParameters = newConfig

    def __configAWG(self, target):
        firstPulseMode = self.__awgParameters['FIRST_PULSE_MODE'] == 'True'
        awg = self.session.blockingInvoker('AWG-MDI-{}'.format(target))
        overallDelay = float(self.__awgParameters['AWG_DELAY_{}_OVERALL'.format(target.upper())])

        for dwKey in ['Decoy', 'Sync', 'Laser', 'PM', 'Time0', 'Time1']:
            awg.configure('delay{}'.format(dwKey),
                          float(self.__awgParameters['AWG_DELAY_{}_{}'.format(target.upper(), dwKey.upper())]) + overallDelay)
            awg.configure('pulseWidth{}'.format(dwKey),
                          float(self.__awgParameters['AWG_PULSE_WITDH_{}_{}'.format(target.upper(), dwKey.upper())]))
        awg.configure('interferometerDiff', float(self.__awgParameters['AWG_DELAY_{}_PULSE_PAIR'.format(target.upper())]))
        awg.configure('syncPeriod', float(self.__awgParameters['AWG_SYNC_PERIOD_{}'.format(target.upper())]))
        waveformLength = len(self.randomNumbersAlice if (target == 'Alice') else self.randomNumbersBob) * 250
        awg.configure("waveformLength", waveformLength)
        awg.configure("firstLaserPulseMode", firstPulseMode)
        if firstPulseMode:
            awg.setRandomNumbers([6] * 1 + [0] * (len(self.randomNumbersAlice) - 1))
        else:
            awg.setRandomNumbers(self.randomNumbersAlice if (target == 'Alice') else self.randomNumbersBob)
        # awg.setRandomNumbers([6] * 1 + [6] * (len(self.randomNumbersAlice) - 1))
        for dwKey in ['DecoyZ', 'DecoyX', 'DecoyY', 'Phase']:
            awg.configure('amp{}'.format(dwKey),
                          float(self.__awgParameters['AWG_AMP_{}_{}'.format(target.upper(), dwKey.upper())]))
        awg.configure("advancedPM", False)
        awg.configure("betterAMTimeA", False)
        awg.configure("ampDecoyZSlope", 0)
        awg.configure("ampDecoyXSlope", 0)
        awg.configure("ampDecoyYSlope", 0)
        awg.configure("pulsePairMode", True)

    def configAWG(self, target):
        self.setChannelEnabled(target, False)
        self.__configAWG(target)
        awg = self.session.blockingInvoker('AWG-MDI-{}'.format(target), 10)
        awg.stopPlay()
        awg.generateNewWaveform()
        awg.startPlay()
        self.setChannelEnabled(target, True)

class VolatileConfiguration:
    def __init__(self, file, listener, independent=True):
        self.file = file
        self.listener = listener
        self.independent = independent
        self.lastModified = os.stat(self.file).st_mtime_ns
        self.config = {}
        self.__reload()

        def monitorLoop():
            while True:
                time.sleep(0.5)
                currentMTime = os.stat(self.file).st_mtime_ns
                if currentMTime > self.lastModified:
                    self.lastModified = currentMTime
                    self.__reload()

        thread = threading.Thread(target=monitorLoop)
        thread.start()

    def __reload(self):
        f = open(self.file)
        lines = f.readlines()
        f.close()
        lines = [line.strip() for line in lines]
        validLines = [line for line in lines if not line.startswith('#') and line.__contains__('=')]
        newConfig = {}
        for validLine in validLines:
            sp = validLine.split('=')
            newConfig[sp[0].strip()] = sp[1].strip()
        if self.independent:
            for key in newConfig:
                if newConfig[key] != self.config.get(key):
                    self.config[key] = newConfig[key]
                    self.listener(key, self.config.get(key))
        else:
            self.listener(newConfig)


if __name__ == '__main__':
    rndRoot = 'C:\\Users\\Administrator\\Desktop\\MDIConfig\\'
    configRoot = 'C:\\Users\\Administrator\\Desktop\\MDIConfig\\'
    # randomNumbersAlice = [int(i) for i in list(np.load(r"{}A.npy".format(rndRoot)))]
    # randomNumbersBob = [int(i) for i in list(np.load(r"{}B.npy".format(rndRoot)))]

    randomNumbersAlice = ([6]*10000)[:10000]
    randomNumbersBob = ([6]*10000)[:10000]

    # import random
    # rnd = random.Random()
    # randomNumbersBob = [rnd.randint(4, 7) for i in range(10000)]
    # print(randomNumbersBob)
    # randomNumbersAlice = [4] * 10000

    ec = ExperimentControl(randomNumbersAlice, randomNumbersBob, configRoot)
    ec.initTDCServer()
    ec.loadConfig()

    while True:
        cmd = sys.stdin.readline()
        if cmd.strip().upper() == 'A':
            try:
                ec.configAWG('Alice')
                print('Done')
            except Exception as e:
                print('Failed')
                print(e)
        if cmd.strip().upper() == 'B':
            try:
                ec.configAWG('Bob')
                print('Done')
            except Exception as e:
                print('Failed')
                print(e)
    # ec.setChannelMode('Alice')
    # ec.configAWG('Alice', False)
    # ec.configAWG('Bob', True)
    # ec.setDelay('Alice', -1)
    # ec.test()

    #
    # ec.stop()
