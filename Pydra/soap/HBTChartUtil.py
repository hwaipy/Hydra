__author__ = 'Hwaipy'

import sys
from Pydra import Message, ProtocolException, Session
import socket
import threading
import time
from Services.Storage import StorageService, HBTFileElement
import random
import math

if __name__ == '__main__':
    mc = Session(("localhost", 20102), None)
    mc.start()
    service = StorageService(mc)
    hbtFile = service.getElement('HBTChartTest.hbt').toHBTFileElement()
    hbtFile.storageElement.delete()
    hbtFile.initialize(
        [["Column 1", HBTFileElement.BYTE], ["Column 2", HBTFileElement.SHORT], ["Column 3", HBTFileElement.INT],
         ["Column 4", HBTFileElement.LONG], ["Column 5", HBTFileElement.FLOAT], ["Column 6", HBTFileElement.DOUBLE]])
    for i in range(0, 10):
        d1 = i
        d2 = i * i
        d3 = -i
        d4 = i * i * i
        d5 = math.sin(i)
        d6 = math.exp(i)
        hbtFile.appendRow([d1, d2, d3, d4, d5, d6])
    mc.stop()
