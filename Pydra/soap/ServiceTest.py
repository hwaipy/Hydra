from Pydra import Session
import time

if __name__ == '__main__':
    session = Session.newSession(('192.168.25.27', 20102))
    invoker = session.blockingInvoker("ArduinoClient")
    invoker.setGains("7", "4", "7", "0", "7", "7", "0", "0")
    while True:
        result = invoker.measure(100)
        rss = result.split(',')[:8]
        # rsses = ['{}'.format(float(r)/1024) for r in rss]
        # print(',  '.join(rsses))
        print(',  '.join(rss))
        time.sleep(1)

