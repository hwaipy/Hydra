from Pydra import Session
import time

if __name__ == '__main__':
    session = Session.newSession(('192.168.25.27', 20102))

    # apd = session.blockingInvoker('APD-MDI-Alice-PL')
    # dc = session.blockingInvoker('DC-MDI-Alice-PL')
    #
    # # initialDelay = apd.getDelay()   #5.6
    # # print(initialDelay)
    # # for i in range(20,120):
    # #     apd.setDelay(i/10)
    # #     print('{}\t{}'.format(apd.getDelay(), apd.detectorFrequency(1)))
    # apd.setDelay(10)
    #
    # for i in range(0, 6):
    #     v = i
    #     dc.setVoltage(0, v)
    #     time.sleep(1)
    #     f = apd.detectorFrequency(1)
    #     print('{}\t{}'.format(v, f))
    # dc.setVoltage(0, 5.12)
    #
    # session.stop()

    config = session.blockingInvoker("GroundTDCService").getAnalyserConfiguration("MDIQKDQBER")
    print(config)

    session.blockingInvoker("GroundTDCService").configureAnalyser("MDIQKDQBER", {"Channel 1": 8, "Channel 2": 9})

    session.stop()