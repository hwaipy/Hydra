from Pydra import Session


class PID:
    """
    Discrete PID control
    """

    def __init__(self, P=2.0, I=0.0, D=1.0, Derivator=0, Integrator=0, Integrator_max=500, Integrator_min=-500):

        self.Kp = P
        self.Ki = I
        self.Kd = D
        self.Derivator = Derivator
        self.Integrator = Integrator
        self.Integrator_max = Integrator_max
        self.Integrator_min = Integrator_min

        self.set_point = 0.0
        self.error = 0.0

    def update(self, current_value):
        """
        Calculate PID output value for given reference input and feedback
        """

        self.error = self.set_point - current_value

        self.P_value = self.Kp * self.error
        self.D_value = self.Kd * (self.error - self.Derivator)
        self.Derivator = self.error

        self.Integrator = self.Integrator + self.error

        if self.Integrator > self.Integrator_max:
            self.Integrator = self.Integrator_max
        elif self.Integrator < self.Integrator_min:
            self.Integrator = self.Integrator_min

        self.I_value = self.Integrator * self.Ki

        # print('PID: {}, {}, {}'.format(self.P_value, self.I_value, self.D_value))
        # print('{}\t{}\t{}\t'.format(self.P_value, self.I_value, self.D_value),end='')
        PID = self.P_value + self.I_value + self.D_value

        return PID

    def setPoint(self, set_point):
        """
        Initilize the setpoint of PID
        """
        self.set_point = set_point
        self.Integrator = 0
        self.Derivator = 0



import pyvisa as visa
from Instruments import DeviceException, VISAInstrument
import enum
import time
import numpy.fft as fft
import math
import sys
import matplotlib.pyplot as plt

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


if __name__ == '__main__33':
    dev1 = KeySight_MultiMeter_34465A('TCPIP0::192.168.25.103::inst0::INSTR')
    dev1.setMeasureQuantity(MeasureQuantity.VoltageDC, autoRange=False, range=0.1, aperture=0.5)
    dev2 = KeySight_MultiMeter_34465A('TCPIP0::192.168.25.107::inst0::INSTR')
    dev2.setMeasureQuantity(MeasureQuantity.VoltageDC, autoRange=False, range=0.3, aperture=0.5)

    def average(data):
        return sum(data) / len(data)
    def std(data):
        mean = average(data)
        ss = sum([math.pow(d - mean, 2) for d in data])
        return math.sqrt(ss / (len(data) - 1)) / mean * 1000000

    f=open('out.csv','w')

    rs1 = []
    rs2 = []
    ratio = []
    count = 0

    while True:
        f1 = dev1.directMeasureAndFetchLater(count=2)
        f2 = dev2.directMeasureAndFetchLater(count=2)

        r1 = f1()
        r2 = f2()
        ra = [r1[0] / r2[0], r1[1] / r2[1]]

        rs1.append(r1[0])
        rs1.append(r1[1])
        rs2.append(r2[0])
        rs2.append(r2[1])
        ratio.append(ra[0])
        ratio.append(ra[1])

        count += 1
        # f.write('{}, {}\n'.format(time.time(), r))
        # f.flush()
        output = '{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}'.format(count, time.time(), average(r1), average(r2), average(ra), std(rs1), std(rs2), std(ratio))
        print(output)
        f.write(output.replace('\t',',',100))
        f.write('\n')
        f.flush()

        print('{}'.format((average(ra)/0.2457241868309823 - 1)*1000000))

        # print('{}\t{}\t{}'.format(count, average(r2), std(r2)))

        # ffted = fft.fft(r)
        # file = open('outoutout.csv','a')
        # for f in ffted:
        #     print(math.sqrt(f.real * f.real + f.imag * f.imag))
        #     file.write('{},'.format(math.sqrt(f.real * f.real + f.imag * f.imag)))
        # file.write('\n')
        # file.close()
        #
        # import sys
        # if count is 20:
        #     sys.exit()

    pass

class Control:
    def __init__(self):
        self.PIDs = [PID() for i in range(0, 3)]

    def setSession(self, session):
        self.session = session

    def start(self):
        dmm1 = KeySight_MultiMeter_34470A('TCPIP0::192.168.25.52::inst0::INSTR')
        dmm2 = KeySight_MultiMeter_34470A('TCPIP0::192.168.25.108::inst0::INSTR')
        dmm3 = KeySight_MultiMeter_34470A('TCPIP0::192.168.25.109::inst0::INSTR')
        dmm1.setMeasureQuantity(MeasureQuantity.VoltageDC, autoRange=False, range=1, aperture=1)
        dmm2.setMeasureQuantity(MeasureQuantity.CurrentDC, autoRange=False, range=0.001, aperture=1)
        dmm3.setMeasureQuantity(MeasureQuantity.Resistance, autoRange=False, range=15000, aperture=1)
        dc = self.session.asynchronousInvoker('')
        file = open('stab.csv', 'w')

        startTime = time.time()
        ref1 = dmm1.directMeasureAndFetchLater(1)()[0]
        ref2 = dmm2.directMeasureAndFetchLater(1)()[0]
        refR = ref1 / ref2
        vs = [0.143, 0.0, 0.140]
        dc.setVoltages(vs)
        dc.setOutputStatuses([True, True, True])
        pids = [PID(0.005, 0.000, 0.003, 0, 0, 1, -1), PID(0.005, 0.000, 0.003, 0, 0, 1, -1),
                PID(0.005, 0.000, 0.003, 0, 0, 1, -1)]
        setPoints = [0, 0, 0]
        for i in range(0, 3):
            pids[i].setPoint(setPoints[i])

        import serial
        dac = serial.Serial('COM3', baudrate=1228800)

        def readTemperatures(times):
            v1 = 0
            v2 = 0
            v3 = 0
            for i in range(0, times):
                dac.write(b'\xAA\x09\x00\x63\x00\x01\xBB')
                dac.flush()
                r = dac.read(24)
                c1hex = r[0:3]
                v1 += (c1hex[0] * 256 * 256 + c1hex[1] * 256 + c1hex[2]) / 16777215 * 10
                c1hex = r[3:6]
                v2 += (c1hex[0] * 256 * 256 + c1hex[1] * 256 + c1hex[2]) / 16777215 * 10
                c1hex = r[6:9]
                v3 += (c1hex[0] * 256 * 256 + c1hex[1] * 256 + c1hex[2]) / 16777215 * 10
            v1 /= times
            v2 /= times
            v3 /= times
            # print('{}, {}, {}'.format(v1, v2, v3))
            return [temp(v1 / 0.0001), temp(v2 / 0.0001), temp(v3 / 0.0001)]

        def tempSetDelta(timeFromStart, i):
            if i is 20:
                return int(timeFromStart / 600)
            else:
                return 0

        while True:
            f1 = dmm1.directMeasureAndFetchLater(1)
            f2 = dmm2.directMeasureAndFetchLater(1)
            f3 = dmm3.directMeasureAndFetchLater(1)
            r1 = f1()[0]
            r2 = f2()[0]
            r3 = f3()[0]
            ppm1 = (r1 / ref1 - 1) * 1000000
            ppm2 = (r2 / ref2 - 1) * 1000000
            ratio = r1 / r2
            ratioPPM = (ratio / refR - 1) * 1000000
            roomTemp = temp(r3)

            t = time.time() - startTime

            temperatures = readTemperatures(3)
            pidPara = readPidPara()
            for i in range(0, 3):
                temperature = temperatures[i]
                pids[i].Kp = pidPara[i][0]
                pids[i].Ki = pidPara[i][1]
                pids[i].Kd = pidPara[i][2]
                vs[i] += pids[i].update(temperature)
                pids[i].setPoint(pidPara[i][3] + tempSetDelta(t, i))
                if (vs[i] > 1.5):
                    vs[i] = 1.5
                if (vs[i] < 0):
                    vs[i] = 0
            dc.setVoltages(vs)

            print(
                '{:.1f}\t{:.3f}\t{:.3f}\t{:.1f}\t{:.1f}\t{:.1f}\t{:.3f}\t{:.3f}\t{:.3f}\t{:.3f}\t{:.3f}\t{:.3f}\t{:.3f}'.format(
                    t, r1, r2, ppm1, ppm2, ratioPPM, roomTemp, temperatures[0], vs[0], temperatures[1], vs[1],
                    temperatures[2], vs[2]))
            file.write(
                '{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}\n'.format(t, r1, r2, ppm1, ppm2, ratioPPM, roomTemp,
                                                                              temperatures[0], vs[0], temperatures[1],
                                                                              vs[1], temperatures[2], vs[2]))
            file.flush()


if __name__ == '__main__':
    tc = Control()
    tc.setSession(Session.newSession(('192.168.25.27', 20102), tc, 'Temperature Control For BS and PDs'))
    tc.start()
