from Pydra import Session
import time

if __name__ == '__main__':
    session = Session.newSession(('192.168.25.27', 20102), None, "ServiceTest")
    try:
        invoker = session.blockingInvoker("GroundTDCServer")
        # invoker.turnOnAnalyser("Counter", {"StorageElement": "/Hwaipy/Hydra/20180906/test.counts"})
        print(invoker.fetchResults("Counter"))
        time.sleep(3)
    except Exception as e:
        print('e')
        print(e)
        pass
    session.stop()
