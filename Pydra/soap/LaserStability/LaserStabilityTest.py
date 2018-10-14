from Services.MultiMeter.KeySightMultiMeter import KeySight_MultiMeter_34465A, KeySight_MultiMeter_34470A, \
    MeasureQuantity
from Pydra import Session
from Services.Storage import StorageService, HBTFileElement
import time

if __name__ == '__main__':
    session = Session.newSession(('192.168.25.27', 20102), None, "LaserStabilityTest")
    dmm1 = session.asynchronousInvoker('KeySightMultiMeter_34470A_1')
    dmm2 = session.asynchronousInvoker('KeySightMultiMeter_34470A_2')
    dmm1.setDCVoltageMeasurement(range=1, autoRange=False, aperture=0.001).sync()
    dmm2.setDCVoltageMeasurement(range=1, autoRange=False, aperture=0.001).sync()


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
        ppm = 1e6 * (ratio / refRatio - 1)
        print(ppm)
    session.stop()