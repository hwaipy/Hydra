import Pydra
import time

class ExperimentControl:
    def __init__(self):
        self.session = Pydra.Session.newSession(('192.168.25.27', 20102), None, 'MDI-QKD-Controller-Test')
        self.currentTDCReportSize = -1

    def getCurrentTDCReport(self, timeout = 5):
        storage = self.session.blockingInvoker("StorageService")
        path = '/test/tdc/mdireport.fs'
        startTime = time.time()
        while True:
            currentSize = int(storage.metaData("", path, False)['Size'])
            if self.currentTDCReportSize == -1:
                self.currentTDCReportSize = currentSize
                continue
            if self.currentTDCReportSize >= currentSize:
                if timeout <= 0:
                    time.sleep(0.3)
                    continue
                else:
                    timePass = time.time() - startTime
                    if timePass > timeout:
                        return None
            else: break

        frameBytes = storage.FSFileReadTailFrames("", path, 0, 1)[0]
        report = Pydra.Message.unpack(frameBytes)
        return report

    def stop(self):
        self.session.stop()


if __name__ == '__main__':
    ec = ExperimentControl()

    while True:
        print(ec.getCurrentTDCReport())
        break

    ec.stop()
