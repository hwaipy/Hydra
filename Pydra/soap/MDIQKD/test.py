from Pydra import Session
import time

if __name__ == '__main__':
    session = Session.newSession(('192.168.25.27', 20102))

    # while True:
    #     session.blockingInvoker('ArduinoClient').setGains("1", "2", "3", "4", "5", "6", "7", "0")
    #     t1 = time.time()
    #     print(session.blockingInvoker('ArduinoClient').measure("100"))
    #     t2 = time.time()
    #     print(t2-t1)
    #     time.sleep(2)
    session.blockingInvoker('ArduinoClientTest').setClientName("BSPS8")
    time.sleep(1)
    session.stop()