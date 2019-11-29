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
import msgpack

session = Pydra.Session.newSession(('192.168.25.27', 20102))
storage = session.blockingInvoker('StorageService')

fetched = storage.FSFileReadTailFrames("", "/test/tdc/default.fs", 0, 1)
unpacker = msgpack.Unpacker(raw=False)
unpacker.feed(fetched[0])
for packed in unpacker:
    pass

qbers = packed['MDIQKDQBER']
hom = qbers['HOM Detailed Sections']
print(len(hom))
print(len(hom[0]))

session.stop()