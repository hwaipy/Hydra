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
from datetime import datetime

session = Pydra.Session.newSession(('192.168.25.27', 20102))
storage = session.blockingInvoker('StorageService')
unitCount = 1
unitMetaSize = 16
offset = 71210057792
# offset = 0

while True:
    fetched = storage.FSFileReadTailFramesFrom("", "/test/tdc/default_old.fs", 0, unitCount, offset)
    fetchedSize = sum([len(f) for f in fetched])
    offset += fetchedSize + unitCount * unitMetaSize

    for f in fetched:
        unpacker = msgpack.Unpacker(raw=False)
        unpacker.feed(f)
        for packed in unpacker:
            break
        t = packed['MDIQKDQBER']['Time']
        ft = datetime.fromtimestamp(t / 1000.0).strftime('%Y%m%d-%H%M%S.%f')[:-3]
    # # dataTime = time.asctime(time.localtime(t/1000))
    #     # print('Current time: {}. Next offset should be {}.'.format(dataTime, offset)
        file = open('D:/TEMP/MDI_Track/{}.qber'.format(ft), 'wb')
        file.write(f)
        file.close()
    print('Current time: {}. Next offset should be {}.'.format(ft, offset))
    time.sleep(0.01)

session.stop()
