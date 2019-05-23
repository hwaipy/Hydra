from Pydra import Session

from Instruments import DeviceException, VISAInstrument
import enum
import time
import math


class KeySight_MultiMeter_34470A(VISAInstrument):
    manufacturer = 'Keysight Technologies'
    model = '34470A'

    def __init__(self, resourceID):
        super().__init__(resourceID)

    def setMeasureQuantity(self, mq, range=0, autoRange=True, aperture=0.001):
        if mq is MeasureQuantity.VoltageDC:
            self.scpi.CONF.VOLT.DC.write('AUTO' if autoRange else range)
            self.scpi.VOLT.APER.write(aperture)
        elif mq is MeasureQuantity.CurrentDC:
            self.scpi.CONF.CURR.DC.write('AUTO' if autoRange else range)
            self.scpi.CURR.APER.write(aperture)
        elif mq is MeasureQuantity.Resistance:
            self.scpi.CONF.RES.write('AUTO' if autoRange else range)
            self.scpi.RES.APER.write(aperture)
        else:
            raise DeviceException('MeasureQuantity {} can not be recognized.'.format(mq))

    def directMeasure(self, count=1):
        self.scpi.TRIG.SOURCE.write('BUS')
        self.scpi.SAMP.COUN.write(count)
        self.scpi.INIT.write()
        self.scpi._TRG.write()
        values = self.scpi.FETC.query()
        return [float(v) for v in values.split(',')]

    def directMeasureAndFetchLater(self, count=1):
        self.scpi.TRIG.SOURCE.write('BUS')
        self.scpi.SAMP.COUN.write(count)
        self.scpi.INIT.write()
        self.scpi._TRG.write()

        def fetch():
            values = self.scpi.FETC.query()
            return [float(v) for v in values.split(',')]

        return fetch


class KeySight_MultiMeter_34465A(VISAInstrument):
    manufacturer = 'Keysight Technologies'
    model = '34465A'

    def __init__(self, resourceID):
        super().__init__(resourceID)

    def setMeasureQuantity(self, mq, range=0, autoRange=True, aperture=0.001):
        if mq is MeasureQuantity.VoltageDC:
            self.scpi.CONF.VOLT.DC.write('AUTO' if autoRange else range)
            self.scpi.VOLT.APER.write(aperture)
        elif mq is MeasureQuantity.CurrentDC:
            self.scpi.CONF.CURR.DC.write('AUTO' if autoRange else range)
            self.scpi.CURR.APER.write(aperture)
        else:
            raise DeviceException('MeasureQuantity {} can not be recognized.'.format(mq))

    def directMeasure(self, count=1):
        self.scpi.TRIG.SOURCE.write('BUS')
        self.scpi.SAMP.COUN.write(count)
        self.scpi.INIT.write()
        self.scpi._TRG.write()
        values = self.scpi.FETC.query()
        return [float(v) for v in values.split(',')]

    def directMeasureAndFetchLater(self, count=1):
        self.scpi.TRIG.SOURCE.write('BUS')
        self.scpi.SAMP.COUN.write(count)
        self.scpi.INIT.write()
        self.scpi._TRG.write()

        def fetch():
            values = self.scpi.FETC.query()
            return [float(v) for v in values.split(',')]

        return fetch


class MeasureQuantity(enum.Enum):
    VoltageDC = 1
    CurrentDC = 2
    Resistance = 3


if __name__ == '__main__':
    dev = KeySight_MultiMeter_34470A('TCPIP0::192.168.25.51::inst0::INSTR')

    dev.setMeasureQuantity(MeasureQuantity.VoltageDC, autoRange=True, range=0.1, aperture=0.0002)
    m = dev.directMeasureAndFetchLater(100)
    print('fetching')
    startTime = time.time()
    m = m()
    stopTime = time.time()
    print(f'finished in {stopTime-startTime} s.')


    def average(data):
        return sum(data) / len(data)


    def stdev(data):
        sum = 0
        ave = average(data)
        for d in data:
            sum += (d - ave) ** 2
        return math.sqrt(sum / (len(data) - 1))


    print(average(m))
    print((stdev(m) / average(m)) * 1e6)
