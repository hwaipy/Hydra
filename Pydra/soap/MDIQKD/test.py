from Pydra import Session

if __name__ == '__main__':
    session = Session.newSession(('192.168.25.27', 20102))

    session.blockingInvoker('GroundTDCService').configureAnalyser('MDIQKDEncoding', {'BinCount': 1000})

    import time

    time.sleep(1)
    session.stop()
