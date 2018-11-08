__author__ = 'Hwaipy'

from Instruments import Instrument
import socket
import time
import math


class CTekQKDSystem(Instrument):

    def __init__(self, wait=0.2):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect(("192.168.25.71", int(35099)))
        self.wait = wait
        self.modulators = {
            "Decoy": CTekQKDSystem.Modulator(True, True),
            "TimeA": CTekQKDSystem.Modulator(True, True),
            "TimeB": CTekQKDSystem.Modulator(True, True),
            "Phase": CTekQKDSystem.Modulator(False, False),
            "Norm": CTekQKDSystem.Modulator(False, True)
        }
        self.modulatorsBiasChannel = {"Decoy": 1, "TimeA": 5, "TimeB": 4, "Norm": 3}

    def sendCMD(self, data):
        if not isinstance(data, list): data = [data]
        bs = self.word2DoubleBytes(2 * len(data))
        for d in data:
            bs += self.word2DoubleBytes(d)
        print(bytes(bs))
        self.socket.send(bytes(bs))
        time.sleep(self.wait)

    def word2DoubleBytes(self, word):
        if word < 0 or word >= 256 * 256:
            raise RuntimeError("not valid word: {}".format(word))
        return [int(word % 256), int(word / 256)]

    def startWithRND(self):
        self.sendCMD(0x9602)

    def enableAll(self):
        self.sendCMD(0x8800)
        self.sendCMD(0x8802)
        self.sendCMD(0x8804)
        self.sendCMD(0x8806)
        self.sendCMD(0x8808)

    def disableAll(self):
        self.sendCMD(0x8801)
        self.sendCMD(0x8803)
        self.sendCMD(0x8805)
        self.sendCMD(0x8807)
        self.sendCMD(0x8809)

    def sendRandomNumbers(self, rnd):
        self.sendCMD(0x9600)
        bs = self.word2DoubleBytes(len(rnd))
        bs += rnd
        self.socket.send(bytes(bs))
        time.sleep(self.wait)

    def __dacVoltageToShort(self, v):
        exp = math.log10(v / 3.2)
        return int(1310.72 * (exp + 0.75))

    def setDacVoltage(self, value, modulatorName, channel=0):
        modulator = self.modulators[modulatorName]
        if channel == 0: modulator.DAC = value
        if channel == 1: modulator.DAC1 = value
        if channel == 2: modulator.DAC2 = value

        values = [self.modulators["Decoy"].DAC2, self.modulators["TimeA"].DAC1, self.modulators["Decoy"].DAC1,
                  self.modulators["Phase"].DAC, self.modulators["Norm"].DAC, self.modulators["TimeA"].DAC2,
                  self.modulators["TimeB"].DAC1, self.modulators["TimeB"].DAC2]

        def convert(v, i):
            N = self.__dacVoltageToShort(v)
            if N < 0: N = 0
            r1 = 0x80 | i << 5 | ((N & 0x0FFF) >> 7)
            r2 = 0x80 | (N & 0x007F)
            return [r1, r2]

        cmd = [0]
        for i in range(0, 4):
            cmd += convert(values[i], i)
        cmd += [2, 1]
        for i in range(0, 4):
            cmd += convert(values[i + 4], i)
        cmd += [3]
        self.sendCMD([0x2100, len(cmd)] + cmd)

    def setFineDelayDif(self, value, modulatorName):
        modulator = self.modulators[modulatorName]
        modulator.delayFineDif
        modulator.delayFineDif = int(value)
        channel = self.modulatorsBiasChannel[modulatorName]
        #
        # volData = int(value / 15 * 4095)
        # cmd = [0x00, 0x09, 0x10, 0x02, 0x20 | (int((channel - 1) / 3) + 1), 0x00 | (channel - 1) % 3 + 1,
        #        (volData & 0xFF00) >> 8, volData & 0x00FF]
        # framHead = [0xA0, 0xA1, 0xA2, 0xA3]
        # frameTail = [0xA4, 0xA5, 0xA6, 0xA7]
        # contentLen = 2
        # checkBitsLen = 2
        # lenth = 4
        #
        # frame = framHead + [(lenth & 0xFF00) >> 8, lenth & 0x00FF] + cmd
        # i = 0
        # sum = 0
        # while i < 2 * lenth + contentLen:
        #     tmp = frame[len(framHead) + i] << 8 | frame[len(framHead) + i + 1]
        #     sum += tmp
        #     i += 2
        # frame += [(sum & 0xFF00) >> 8, sum & 0x00FF] + frameTail
        # self.sendCMD([0x2200, len(frame)] + frame)
        # if value > 15: value = 15
        # if value < 0: value = 0
        #
        # modulator = self.modulators[modulatorName]
        # modulator.bias
        # modulator.bias = value
        # channel = self.modulatorsBiasChannel[modulatorName]
        #
        # volData = int(value / 15 * 4095)
        # cmd = [0x00, 0x09, 0x10, 0x02, 0x20 | (int((channel - 1) / 3) + 1), 0x00 | (channel - 1) % 3 + 1,
        #        (volData & 0xFF00) >> 8, volData & 0x00FF]
        # framHead = [0xA0, 0xA1, 0xA2, 0xA3]
        # frameTail = [0xA4, 0xA5, 0xA6, 0xA7]
        # contentLen = 2
        # checkBitsLen = 2
        # lenth = 4
        #
        # frame = framHead + [(lenth & 0xFF00) >> 8, lenth & 0x00FF] + cmd
        # i = 0
        # sum = 0
        # while i < 2 * lenth + contentLen:
        #     tmp = frame[len(framHead) + i] << 8 | frame[len(framHead) + i + 1]
        #     sum += tmp
        #     i += 2
        # frame += [(sum & 0xFF00) >> 8, sum & 0x00FF] + frameTail
        # self.sendCMD([0x2200, len(frame)] + frame)

    class Modulator:
        def __init__(self, doublePulse, hasBias):
            if not doublePulse:
                self.DAC = 1
                self.delayFine = 0
                self.delayCoarse = 0
            else:
                self.DAC1 = 1
                self.DAC2 = 1
                self.delayFineDif = 0
                self.delayCoarseDif = 0
            if hasBias:
                self.bias = 0

    class RND:
        def __init__(self, decoy, basis, code):
            self.decoy = decoy
            self.basis = basis
            self.code = code

        def value(self):
            if (self.decoy != self.DECOY and self.decoy != self.SIGNAL and self.decoy != self.VACUUM) \
                    or (self.basis != self.TIME and self.basis != self.PHASE) \
                    or (self.code != self.CODE0 and self.code != self.CODE1):
                raise RuntimeError("Wrong RNG!")
            return self.decoy | self.basis | self.code

    RND.DECOY = 0b1000
    RND.SIGNAL = 0b1100
    RND.VACUUM = 0b0000
    RND.TIME = 0b10
    RND.PHASE = 0b00
    RND.CODE1 = 0b1
    RND.CODE0 = 0b0

    RND.SIGNAL_TIME_1 = RND(RND.SIGNAL, RND.TIME, RND.CODE1).value()
    RND.SIGNAL_TIME_0 = RND(RND.SIGNAL, RND.TIME, RND.CODE0).value()
    RND.SIGNAL_PHASE_1 = RND(RND.SIGNAL, RND.PHASE, RND.CODE1).value()
    RND.SIGNAL_PHASE_0 = RND(RND.SIGNAL, RND.PHASE, RND.CODE0).value()
    RND.DECOY_TIME_1 = RND(RND.DECOY, RND.TIME, RND.CODE1).value()
    RND.DECOY_TIME_0 = RND(RND.DECOY, RND.TIME, RND.CODE0).value()
    RND.DECOY_PHASE_1 = RND(RND.DECOY, RND.PHASE, RND.CODE1).value()
    RND.DECOY_PHASE_0 = RND(RND.DECOY, RND.PHASE, RND.CODE0).value()
    RND.VACUUM_TIME_1 = RND(RND.VACUUM, RND.TIME, RND.CODE1).value()
    RND.VACUUM_TIME_0 = RND(RND.VACUUM, RND.TIME, RND.CODE0).value()
    RND.VACUUM_PHASE_1 = RND(RND.VACUUM, RND.PHASE, RND.CODE1).value()
    RND.VACUUM_PHASE_0 = RND(RND.VACUUM, RND.PHASE, RND.CODE0).value()


if __name__ == '__main__':
    import sys
    import Utils
    import Pydra

    # sysArgs = Utils.SystemArguments(sys.argv)
    # server = sysArgs.get('server', 'localhost')
    # port = sysArgs.get('port', '20102')
    # wydraPort = sysArgs.get('wydraPort', '20080')
    # clientName = sysArgs.get('clientName', 'test')
    #
    # invoker = HMC7044Eval()
    #
    # session = Pydra.Session.newSession((server, int(port)), invoker, clientName)
    #
    # while True:
    #     import time
    #
    #     time.sleep(5)
    #     invoker.setReg(0xC9, 0x0)
    #     time.sleep(5)
    #     invoker.setReg(0xC9, 0x10)
    # #     if sys.stdin.readline() == 'q\n':
    # #         print('Stopping VirtualDCSupply.')
    # #         session.stop()
    # #         break

    cTekQKDSystem = CTekQKDSystem()
    # cTekQKDSystem.startWithRND()
    # # cTekQKDSystem.enableAll()
    #
    # rnd = [CTekQKDSystem.RND.VACUUM_PHASE_0, CTekQKDSystem.RND.VACUUM_PHASE_0, CTekQKDSystem.RND.VACUUM_PHASE_0,
    #        CTekQKDSystem.RND.VACUUM_PHASE_0, CTekQKDSystem.RND.VACUUM_PHASE_0,
    #        CTekQKDSystem.RND.DECOY_PHASE_0, CTekQKDSystem.RND.DECOY_PHASE_0,
    #        CTekQKDSystem.RND.SIGNAL_PHASE_0, CTekQKDSystem.RND.SIGNAL_PHASE_0, CTekQKDSystem.RND.SIGNAL_PHASE_0] * 51200
    # rnd = [CTekQKDSystem.RND.VACUUM_PHASE_0] + [CTekQKDSystem.RND.SIGNAL_PHASE_0] * 1195 + \
    #       [CTekQKDSystem.RND.VACUUM_PHASE_0] * 2 + [CTekQKDSystem.RND.SIGNAL_PHASE_0] * 2
    rnd = [CTekQKDSystem.RND.DECOY_PHASE_0, CTekQKDSystem.RND.SIGNAL_TIME_0] * 50000
    # rnd = ([CTekQKDSystem.RND.DECOY_TIME_1] * 1 + [CTekQKDSystem.RND.SIGNAL_TIME_0] * 1 +
    #        [CTekQKDSystem.RND.SIGNAL_PHASE_1] * 1 + [CTekQKDSystem.RND.SIGNAL_PHASE_0] * 2) * 50000
    print(rnd[:10])
    cTekQKDSystem.sendRandomNumbers(rnd[:50000])

    # print(CTekQKDSystem.Modulator(False, True).bais)
    cTekQKDSystem.setDacVoltage(8, "Decoy", 1)
    cTekQKDSystem.setDacVoltage(4, "Decoy", 2)
    cTekQKDSystem.setDacVoltage(8, "TimeA", 1)
    cTekQKDSystem.setDacVoltage(8, "TimeA", 2)
    cTekQKDSystem.setDacVoltage(8, "TimeB", 1)
    cTekQKDSystem.setDacVoltage(8, "TimeB", 2)
    cTekQKDSystem.setDacVoltage(8, "Norm", 2)
    # cTekQKDSystem.setBiasVoltage(10, "Decoy")
