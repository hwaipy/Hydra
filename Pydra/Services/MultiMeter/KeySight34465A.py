__author__ = 'Hwaipy'

from Instruments import DeviceException, VISAInstrument
import enum

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