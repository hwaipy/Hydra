from Services.MultiMeter.KeySightMultiMeter import KeySight_MultiMeter_34465A, KeySight_MultiMeter_34470A, MeasureQuantity
from Pydra import Session
from Services.Storage import StorageService, HBTFileElement
import time

if __name__ == '__main__':
    session = Session.newSession(('192.168.25.27', 20102), None, "LaserStabilityTest")
    dev1 = KeySight_MultiMeter_34465A('TCPIP0::192.168.25.103::inst0::INSTR')
    dev1.setMeasureQuantity(MeasureQuantity.VoltageDC, autoRange=False, range=0.1, aperture=1)
    dev2 = KeySight_MultiMeter_34465A('TCPIP0::192.168.25.107::inst0::INSTR')
    dev2.setMeasureQuantity(MeasureQuantity.VoltageDC, autoRange=False, range=0.05, aperture=1)
    dev3 = KeySight_MultiMeter_34470A('TCPIP0::192.168.25.52::inst0::INSTR')
    dev3.setMeasureQuantity(MeasureQuantity.CurrentDC, autoRange=False, range=0.02, aperture=1)

    service = StorageService(session)
    timeStamp = time.strftime("%Y-%m-%d %H-%M-%S", time.localtime())
    hbtFile = service.getElement(
        '/Hwaipy/4K_EOE/20171230-LaserStability/{}.hip/VoltageRecord.hbt'.format(timeStamp)).toHBTFileElement()
    hbtFile.initialize(
        [["Time (ms)", HBTFileElement.LONG], ["PD1 (V)", HBTFileElement.DOUBLE], ["PD2 (V)", HBTFileElement.DOUBLE],
         ["LD Current(A)", HBTFileElement.DOUBLE]])

    while True:
        f1 = dev1.directMeasureAndFetchLater(count=1)
        f2 = dev2.directMeasureAndFetchLater(count=1)
        f3 = dev3.directMeasureAndFetchLater(count=1)
        r1 = f1()[0]
        r2 = f2()[0]
        r3 = f3()[0]
        currentTime = time.time()
        hbtFile.appendRow([currentTime * 1000, r1, r2, r3])
        print([currentTime * 1000, r1, r2, r3])
