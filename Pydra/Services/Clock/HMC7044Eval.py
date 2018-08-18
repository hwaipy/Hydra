__author__ = 'Hwaipy'

from Instruments import Instrument
from socketserver import BaseRequestHandler, TCPServer


class HMC7044Eval(Instrument):

    def __init__(self):
        self.devices = {}
        self.channelDivider = [0xC9, 0xD3, 0xDD]

    def setDivider(self, channel, divide):
        self.checkChannel(channel)
        self.checkRange(divide, 2, 4094)
        if divide % 2 is not 0:
            raise RuntimeError('Only even are supported for divider.')
        self.setReg(0xC9 + channel * 10, divide)

    def checkChannel(self, channel):
        self.checkRange(channel, 0, 13, 'channel')

    def checkRange(self, value, min, max, valueName='value'):
        if value < min or value > max:
            raise RuntimeError('{} {} out of range: [{}, {}]'.format(valueName, value, min, max))

    def setReg(self, reg, value):
        if isinstance(reg, int) and isinstance(value, int) and reg >= 0 and value >= 0:
            print('{},{}'.format(reg, value))

    def deviceConnected(self):
        return len(self.devices.keys()) > 0

    def newDevice(self, request):
        self.devices[request] = request

    def deleteDevice(self, request):
        del self.devices[request]


if __name__ == '__main__':
    import sys
    import Utils
    import Pydra
    import time

    sysArgs = Utils.SystemArguments(sys.argv)
    server = sysArgs.get('server', '192.168.25.27')
    port = sysArgs.get('port', '20102')
    clientName = sysArgs.get('clientName', 'test')

    invoker = HMC7044Eval()
    session = Pydra.Session.newSession((server, int(port)), invoker, clientName)


    class EchoHandler(BaseRequestHandler):
        def handle(self):
            print('Got connection from', self.client_address)
            invoker.newDevice(self.request)
            try:
                while True:
                    msg = self.request.recv(8192)
            finally:
                invoker.deleteDevice(self.request)


    serv = TCPServer(('', 25001), EchoHandler)
    serv.serve_forever()
