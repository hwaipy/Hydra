from Services.MultiMeter.KeySightMultiMeter import KeySight_MultiMeter_34465A, MultiMeterServiceWrap, KeySight_MultiMeter_34470A
import numpy as np
import pyvisa as visa
import time

if __name__ == '__main__':
    dev1 = MultiMeterServiceWrap(KeySight_MultiMeter_34465A('TCPIP0::192.168.25.110::inst0::INSTR'))
    dev2 = MultiMeterServiceWrap(KeySight_MultiMeter_34470A('TCPIP0::192.168.25.111::inst0::INSTR'))
    dev1.setDCCurrentMeasurement(2, True, 0.005)
    dev2.setDCCurrentMeasurement(2, True, 0.005)

    outputPath = 'D:/TEMP/XiaoPang.csv'
    while True:
        f1 = dev1.directMeasureAndFetchLater(200)
        f2 = dev2.directMeasureAndFetchLater(200)
        r1 = f1()
        r2 = f2()
        print('{}+-{},  {}+-{}'.format(np.average(np.array(r1)), np.std(np.array(r1)), np.average(np.array(r2)), np.std(np.array(r2))))
        file = open(outputPath, 'a')
        file.write('{}, PD_1, {}\n'.format(time.time(), ', '.join([str(r) for r in r1])))
        file.write('{}, PD_2, {}\n'.format(time.time(), ', '.join([str(r) for r in r2])))
        file.close()
