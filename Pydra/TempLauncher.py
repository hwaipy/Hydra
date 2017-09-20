from Services.WaveformGenerator.PXIFrequencyDivider import PXIFrequencyDivider
import random
from Pydra import Session
import platform
import os


# print('Launching App...')
#
# if platform.node() == 'HwaipydeMacBook-Pro.local':
#     adapter = None
# else:
#     adapter = 'TDCTest.exe'
#
# PXIFrequencyDivider(None, 20154, ('192.168.25.27', 20102), 'PXIFrequencyDividerH')



def setPXIFrequencyDivider():
    session = Session.newSession(("192.168.25.27", 20102))
    invoker = session.blockingInvoker('PXIFrequencyDivider')
    invoker.setDelay(37, 100, 0)
    session.stop()


setPXIFrequencyDivider()
