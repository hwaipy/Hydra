from Pydra import Session
import time

if __name__ == '__main__':
    session = Session.newSession(('192.168.25.27', 20102), None, "PolarizationControlTestLocal")
    try:
        invoker = session.blockingInvoker("PolarizationControlService")
        print(invoker.hello("23"))
        time.sleep(3)
    except Exception:
        print('e')
        pass
    session.stop()
