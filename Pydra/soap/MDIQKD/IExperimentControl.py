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
    def __init__(self):
        self.session = Pydra.Session.newSession(('172.16.60.199', 20102), None, 'MDI-QKD-Controller-I')
        self.groundTDCService = self.session.blockingInvoker('GroundTDCService')
        self.clockAlice = self.session.blockingInvoker('HMC7044EvalAlice')
        self.clockBob = self.session.blockingInvoker('HMC7044EvalBob')
        self.clockCharlie = self.session.blockingInvoker('HMC7044EvalCharlie')

    def stop(self):
        self.session.stop()

    def setDelay(self, target, delay):
        targetClock = self.clockAlice if target == 'Alice' else self.clockBob
        reversedClock = self.clockBob if target == 'Alice' else self.clockAlice
        if delay >= 0:
            targetClock.setDelay(0, delay)
        else:
            reversedClock.setDelay(0, -delay)
            self.clockCharlie.setDelay(0, -delay)

if __name__ == '__main__':
    ec = ExperimentControl()

    # ec.setDelay('Alice', -3)
    ec.setDelay('Bob', -10)
    # ec.groundTDCService.configureAnalyser("MDIQKDEncoding", {"SignalChannel": 8})

    ec.stop()
