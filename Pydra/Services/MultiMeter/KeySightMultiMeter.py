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


class KeySight_MultiMeter_34470A(KeySight_MultiMeter_34465A):
    manufacturer = 'Keysight Technologies'
    model = '34470A'

    def __init__(self, resourceID):
        super().__init__(resourceID)


class MeasureQuantity(enum.Enum):
    VoltageDC = 1
    CurrentDC = 2
    Resistance = 3


class MultiMeterServiceWrap:
    def __init__(self, dev):
        self.dev = dev

    def setDCVoltageMeasurement(self, range=0, autoRange=True, aperture=0.001):
        self.dev.setMeasureQuantity(MeasureQuantity.VoltageDC, range, autoRange, aperture)

    def setDCCurrentMeasurement(self, range=0, autoRange=True, aperture=0.001):
        self.dev.setMeasureQuantity(MeasureQuantity.CurrentDC, range, autoRange, aperture)

    def setResistanceMeasurement(self, range=0, autoRange=True, aperture=0.001):
        self.dev.setMeasureQuantity(MeasureQuantity.Resistance, range, autoRange, aperture)

    def directMeasure(self, count=1):
        return self.dev.directMeasure(count)


if __name__ == '__main__':
    import argparse
    import sys
    import Pydra

    parser = argparse.ArgumentParser()
    parser.add_argument('--model', '-m', help="Model of MultiMeter, should be 34465A or 34470A", type=str)
    parser.add_argument('--resource', '-r',
                        help="Visa Resource. For TCP/IP resource, this argument could be TCPIP0::[ip address]::inst0::INSTR",
                        type=str)
    parser.add_argument('--hydra_address', '-a', help="Hydra Host address", type=str)
    parser.add_argument('--hydra_port', '-p', help="Hydra Host port, 20102 by default", type=int, default=20102)
    parser.add_argument('--service_name', '-s', help="Hydra Service name", type=str)

    args = parser.parse_args()
    model = args.model
    visaResource = args.resource
    hydraAddress = args.hydra_address
    hydraPort = args.hydra_port
    name = args.service_name

    if model == '34470A':
        dev = KeySight_MultiMeter_34470A(visaResource)
    elif model == '34465A':
        dev = KeySight_MultiMeter_34465A(visaResource)
    else:
        raise RuntimeError(f'Model {model} not valid.')

    session = Pydra.Session.newSession((hydraAddress, hydraPort), MultiMeterServiceWrap(dev), name)
    print(f'KeySight MultiMeter started as MultiMeter Service [{name}]')
    for line in sys.stdin:
        if line == 'q\n':
            break
    session.stop()
    dev.close()
