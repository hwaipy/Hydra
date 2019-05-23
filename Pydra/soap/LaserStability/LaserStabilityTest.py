from Services.MultiMeter.KeySightMultiMeter import KeySight_MultiMeter_34465A, KeySight_MultiMeter_34470A, \
    MeasureQuantity
from Pydra import Session
from Services.Storage import StorageService, HBTFileElement
import time

if __name__ == '__main__':
    session = Session.newSession(('192.168.25.27', 20102), None, "LaserStabilityTest")
    dmm1 = session.asynchronousInvoker('DMM1')
    dmm2 = session.asynchronousInvoker('DMM2')
    # dmm1.setResistanceMeasurement(range=1, autoRange=False, aperture=0.001).sync()
    # dmm2.setResistanceMeasurement(range=1, autoRange=False, aperture=0.001).sync()
    dmm1.setResistanceMeasurement(range=2500, autoRange=False, aperture=0.001).sync()
    dmm2.setResistanceMeasurement(range=2500, autoRange=False, aperture=0.001).sync()


    def measure(count=1000):
        f1 = dmm1.directMeasure(count)
        f2 = dmm2.directMeasure(count)
        r1 = f1.sync()
        r2 = f2.sync()
        ar1 = sum(r1) / len(r1)
        ar2 = sum(r2) / len(r2)
        return [ar1, ar2]

    ref = measure(400)
    while True:
        r = measure(400)
        ratio = r[0] / r[1]
        refRatio = ref[0] / ref[1]
        ppmRatio = 1e6 * (ratio / refRatio - 1)
        ppm1 = 1e6 * (r[0] / ref[0] - 1)
        ppm2 = 1e6 * (r[1] / ref[1] - 1)
        # print('{}, {}'.format(r[0] / r[1], ppmRatio))
        print('{}, {}, {}, {}'.format(r[0], r[1], ppm1, ppm2))
        file = open('stab.csv', 'a')
        file.write('{}, {}, {}, {}, {}\n'.format(time.time(), r[0], r[1], ppm1, ppm2))
        file.close()
    session.stop()
