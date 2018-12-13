from Pydra import Session

if __name__ == '__main__':
    session = Session.newSession(('192.168.25.27', 20102))

    tdc = session.blockingInvoker('GroundTDCService')
    tdc.configureAnalyser('MDIQKDQBER', {'Delay': -000})

    import time

    time.sleep(1)
    session.stop()
