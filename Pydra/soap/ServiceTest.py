from Pydra import HttpSession
import time

if __name__ == '__main__':
    class Invoker:
        def func(self):
            return "1rqwf"

    for i in range(0,10):
        service = HttpSession.create("http://sheldon.local/hydra/message", Invoker(), 'PydraTestService[{}]'.format(i))

    startTime = time.time()
    while True:
        try:
            fetcher = HttpSession.create("http://sheldon.local/hydra/message")
            for serviceName in fetcher.getServiceList():
                fetcher.blockingInvoker(serviceName).func()
            print('done {}'.format(time.time() - startTime))
            fetcher.stop()
            time.sleep(0.1)
        except Exception as e:
            print(e)