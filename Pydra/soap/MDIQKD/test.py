from Pydra import Session

if __name__ == '__main__':
    session = Session.newSession(('192.168.25.27', 20102))
    invoker = session.blockingInvoker('HMC7044EvalTest')
    print(invoker.deviceConnected())
    for i in range(0, 14):
        invoker.setDivider(i, 200)

    import time

    time.sleep(1)
    session.stop()
