__author__ = 'Hwaipy'

from Instruments import Instrument


class HMC7044Eval(Instrument):

    def __init__(self):
        pass

    def setReg(self, reg, value):
        if isinstance(reg, int) and isinstance(value, int) and reg >= 0 and value >= 0:
            print('{},{}'.format(reg, value))


if __name__ == '__main__':
    import sys
    import Utils
    import Pydra

    sys.path.append("../../")

    sysArgs = Utils.SystemArguments(sys.argv)
    server = sysArgs.get('server', '192.168.25.27')
    port = sysArgs.get('port', '20102')
    wydraPort = sysArgs.get('wydraPort', '20080')
    clientName = sysArgs.get('clientName', 'test')

    invoker = HMC7044Eval()

    print('c')
    session = Pydra.Session.newSession((server, int(port)), invoker, clientName)
    print('d')

    while True:
        import time

        time.sleep(5)
        invoker.setReg(0xC9, 0x0)
        time.sleep(5)
        invoker.setReg(0xC9, 0x10)
    #     if sys.stdin.readline() == 'q\n':
    #         print('Stopping VirtualDCSupply.')
    #         session.stop()
    #         break
