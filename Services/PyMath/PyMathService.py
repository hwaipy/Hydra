import sys

sys.path.append('D:/GitHub/Hydra/Pydra/')

import Utils
import time
import Pydra
import GaussianFit
import RiseTimeFit

sysArgs = Utils.SystemArguments(sys.argv)
server = sysArgs.get('server', '192.168.25.27')
port = sysArgs.get('port', '20102')
clientName = sysArgs.get('clientName', 'PyMathService')


class PyMathService:
    def __init__(self):
        pass

    def singlePeakGaussianFit(self, xs, ys):
        return GaussianFit.singlePeakGaussianFit(xs, ys)

    def riseTimeFit(self, xs, ys):
        try:
            return RiseTimeFit.riseTimeFit(xs, ys)
        except Exception as e:
            import traceback
            msg = traceback.format_exc() # 方式1
            print(msg)

invoker = PyMathService()
session = Pydra.Session.newSession((server, int(port)), invoker, clientName)

# popt = invoker.singlePeakGaussianFit([x for x in range(0, 100)], [0] * 45 + [1, 4, 10, 40, 200, 100, 30, 10, 3, 1] + [0] * 45)
# print(popt)