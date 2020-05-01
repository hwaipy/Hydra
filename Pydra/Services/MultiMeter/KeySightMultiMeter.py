__author__ = 'Hwaipy'

from Instruments import DeviceException, VISAInstrument
import enum
import numpy as np


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
    manufacturer = 'Agilent Technologies'
    model = '34410A'

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

    def directMeasureAndFetchLater(self, count=1):
        return self.dev.directMeasureAndFetchLater(count)


if __name__ == '__main__':
    # import argparse
    # import sys
    # import Pydra
    import time
    import pyvisa as visa

    print(visa.ResourceManager().list_resources())
    #
    # parser = argparse.ArgumentParser()
    # parser.add_argument('--model', '-m', help="Model of MultiMeter, should be 34465A or 34470A", type=str)
    # parser.add_argument('--resource', '-r',
    #                     help="Visa Resource. For TCP/IP resource, this argument could be TCPIP0::[ip address]::inst0::INSTR",
    #                     type=str)
    # parser.add_argument('--hydra_address', '-a', help="Hydra Host address", type=str)
    # parser.add_argument('--hydra_port', '-p', help="Hydra Host port, 20102 by default", type=int, default=20102)
    # parser.add_argument('--service_name', '-s', help="Hydra Service name", type=str)
    #
    # args = parser.parse_args()
    # model = args.model
    # visaResource = args.resource
    # hydraAddress = args.hydra_address
    # hydraPort = args.hydra_port
    # name = args.service_name
    #
    # model = '34470A'
    visaResource1 = 'TCPIP0::192.168.25.119::inst0::INSTR'
    visaResource2 = 'TCPIP0::192.168.25.120::inst0::INSTR'
    # hydraAddress = '192.168.25.27'
    # hydraPort = 20102
    # name = 'DMM2'
    #
    # print("here")

    # if model == '34470A':
    dev1 = KeySight_MultiMeter_34465A(visaResource1)
    dev2 = KeySight_MultiMeter_34470A(visaResource2)
    # elif model == '34465A':
    #     dev = KeySight_MultiMeter_34465A(visaResource)
    # else:
    #     raise RuntimeError('Model {model} not valid.')

    # session = Pydra.Session.newSession((hydraAddress, hydraPort), MultiMeterServiceWrap(dev), name)
    # print(f'KeySight MultiMeter started as MultiMeter Service [{name}]')
    # for line in sys.stdin:
    #     if line == 'q\n':
    #         break
    # session.stop()
    # dev.close()

    ref = 0

    wrap1 = MultiMeterServiceWrap(dev1)
    wrap2 = MultiMeterServiceWrap(dev2)
    wrap1.setDCCurrentMeasurement(0.001, True, 0.005)
    wrap2.setDCCurrentMeasurement(0.001, True, 0.005)
    while True:
        f1 = wrap1.directMeasureAndFetchLater(200)
        f2 = wrap2.directMeasureAndFetchLater(200)
        r1 = f1()
        r2 = f2()
        npr1 = np.array(r1)
        npr2 = np.array(r2)

        ave1 = np.average(npr1)
        ave2 = np.average(npr2)
        ratio = ave1 / ave2

        if ref == 0:
            ref = ratio
        ratioPPM = (ratio / ref - 1) * 1e6

        print('{}, {}, {}, {}'.format(ave1, ave2, ratio, ratioPPM))
        file = open('LS.csv'.format(time.time()), 'a')
        file.write('{},{},{},{}\n'.format(ave1, ave2, ratio, ratioPPM))
        file.close()
