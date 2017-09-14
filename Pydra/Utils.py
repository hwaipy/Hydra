__author__ = 'Hwaipy'

import queue
import threading
import time


class Communicator:
    def __init__(self, channel, dataFetcher, dataSender):
        self.__channel = channel
        self.__dataFetcher = dataFetcher
        self.__dataSender = dataSender
        self.__sendQueue = queue.Queue()

    def start(self):
        self.__running = True
        threading.Thread(target=self.__receiveLoop, name="communicator_receive_loop", daemon=False).start()
        threading.Thread(target=self.__sendLoop, name="communicator_send_loop", daemon=False).start()

    def __receiveLoop(self):
        try:
            while self.__running:
                self.__dataFetcher(self.__channel)
        except BaseException as re:
            pass
        finally:
            self.__running = False

    def sendLater(self, message):
        self.__sendQueue.put(message)

    def __sendLoop(self):
        try:
            while self.__running:
                try:
                    message = self.__sendQueue.get(timeout=0.5)
                    self.__dataSender(self.__channel, message)
                except queue.Empty:
                    pass
        except BaseException as e:
            pass
        finally:
            self.__running = False

    def isRunning(self):
        return self.__running


class BlockingCommunicator(Communicator):
    def __init__(self, channel, dataFetcher, dataSender):
        Communicator.__init__(self, channel, self.dataQueuer, dataSender)
        self.dataQueue = queue.Queue()
        self.dataFetcherIn = dataFetcher

    def dataQueuer(self, channel):
        data = self.dataFetcherIn(channel)
        self.dataQueue.put(data)

    def query(self, message):
        self.sendLater(message)
        return self.dataQueue.get()


class SingleThreadProcessor:
    def __init__(self):
        self.queue = queue.Queue()
        threading.Thread(target=self.__loop, name='SingleTreadProcessorThread-{}'.format(time.time()),
                         daemon=True).start()

    def invokeLater(self, action, *args, **kwargs):
        self.queue.put((action, args, kwargs))

    def invokeAndWait(self, action, *args, **kwargs):
        semaphore = threading.Semaphore(0)
        result = [None, None]
        self.queue.put((action, args, kwargs, semaphore, result))
        semaphore.acquire()
        if result[1] is not None:
            raise result[1]
        return result[0]

    def __loop(self):
        while True:
            action, args, kwargs, semaphore, result = self.queue.get()
            try:
                ret = action(*args, **kwargs)
                result[0] = ret
            except BaseException as e:
                result[1] = e
            semaphore.release()


class SingleThreadWarpper:
    def __init__(self, invoker):
        self.invoker = invoker
        self.processor = SingleThreadProcessor()

    def __getattr__(self, item):
        method = self.invoker.__getattribute__(item)

        def action(*args, **kwargs):
            return self.processor.invokeAndWait(method, *args, **kwargs)

        return action
