from Pydra import HttpSession
import time

if __name__ == '__main__':
    while True:
        # try:
        #     fetcher = HttpSession.create("http://localhost:9000/hydra/message")
        #     print(fetcher.getServiceList())
        #     time.sleep(10)
        # except Exception as e:
        #     print(e)
        fetcher = HttpSession.create("http://localhost:9000/hydra/message")
        print(fetcher.target1.fetch("->", "<-"))
        time.sleep(3)
