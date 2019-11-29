import Pydra
import time
import socket
import sys
import numpy as np
import matplotlib.pyplot as plt
import threading
import math
from math import isnan

class TimeSyncer:
    def __init__(self):
        self.session = Pydra.Session.newSession(('172.16.60.199', 20102), None, 'MDI-QKD-Time-Tuner')
        self.dacAlice = self.session.blockingInvoker('FTDC-Alice')
        self.dacBob = self.session.blockingInvoker('FTDC-Bob')
        self.clockAlice = self.session.blockingInvoker('HMC7044EvalAlice')
        self.clockBob = self.session.blockingInvoker('HMC7044EvalBob')
        self.clockCharlie = self.session.blockingInvoker('HMC7044EvalCharlie')
        self.groundTDCService = self.session.blockingInvoker('GroundTDCService')

    def stop(self):
        self.session.stop()

    def setAliceDelay(self, Toffset):
            if Toffset >= 0:
                self.clockAlice.setDelay(0, Toffset)
            else:
                self.clockBob.setDelay(0, -Toffset)
                self.clockCharlie.setDelay(0, -Toffset)

    def setBobDelay(self, Toffset):
            if Toffset >= 0:
                self.clockBob.setDelay(0, Toffset)
            else:
                self.clockAlice.setDelay(0, -Toffset)
                self.clockCharlie.setDelay(0, -Toffset)

    def setAliceClock(self, speedInPsPerS):
        code = speedInPsPerS / 0.119
        self.dacAlice.increaseCode(1, int(code))

    def setBobClock(self, speedInPsPerS):
        code = speedInPsPerS / 0.119
        self.dacBob.increaseCode(1, int(code))

    def setParserChannel(self, channel):
        if channel == 9:
            self.groundTDCService.configureAnalyser("MDIQKDEncoding", {"SignalChannel": 8})
        elif channel == 10:
            self.groundTDCService.configureAnalyser("MDIQKDEncoding", {"SignalChannel": 9})
        else:
            raise RuntimeError('Wrong channel')

if __name__ == '__main__':
    tuner = TimeSyncer()
    # tuner.setAliceDelay(1.0)      #set delay within 10 ns
    tuner.setBobDelay(-0.4)      #set delay within 10 ns

    # negative if drift left
    # tuner.setBobClock(92)       #set VCO frequency

    tuner.setParserChannel(10)

    tuner.stop()