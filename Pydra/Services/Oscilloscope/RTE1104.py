__author__ = 'Hwaipy'

from Instruments import DeviceException, VISAInstrument
import enum
import numpy as np


class RTE1104(VISAInstrument):
    manufacturer = 'Rohde&Schwarz'
    model = 'RTE'

    def __init__(self, resourceID):
        super().__init__(resourceID)

    def setVoltageRange(self, channel, lower, upper):
        self.scpi.__getattr__('CHANnel{}'.format(channel)).OFFSet.write((lower + upper) / 2)
        self.scpi.__getattr__('CHANnel{}'.format(channel)).RANGe.write(upper - lower)

    def single(self, range, sampleCount, channels):
        for channel in channels:
            self.scpi.__getattr__('CHAN{}'.format(channel)).write('ON')
        self.scpi.ACQuire.POINts.AUTO.write('RECLength')
        self.scpi.ACQuire.POINts.write(sampleCount)
        self.scpi.TIMebase.RANGe.write(range)
        self.scpi.TIMebase.HORizontal.POSition.write(0)
        self.scpi.FORMat.DATA.write('REAL', 32)
        self.scpi.SINGLE.write()
        self.scpi._OPC.query()
        unitCount = 500
        waveforms = []
        for channel in channels:
            p1 = 0
            p2 = min(p1 + unitCount, sampleCount)
            w = []
            while p1 < p2:
                w += self.scpi.__getattr__('CHANnel{}'.format(channel)).DATA.queryB(p1, p2 - p1)
                p1 = p2
                p2 = min(p1 + unitCount, sampleCount)
            waveforms.append(w)
        return waveforms


if __name__ == '__main__':
    resource = 'TCPIP0::172.16.20.111::inst0::INSTR'
    rte = RTE1104(resource)

    rte.setVoltageRange(1, -2.5, 4)
    rte.setVoltageRange(2, -0.05, 0.05)
    rte.setVoltageRange(3, -1, 2)
    waveforms = rte.single(1e-5, 10000, [1, 2, 3])

    times = np.linspace(0, 1e-5, 10000)
    import matplotlib.pyplot as plt

    plt.plot(times, waveforms[0])
    plt.plot(times, waveforms[1])
    plt.plot(times, waveforms[2])
    plt.show()
