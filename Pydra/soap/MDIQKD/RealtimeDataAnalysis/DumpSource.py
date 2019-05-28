import Pydra
import time
import os
import msgpack

class DumpSource:
    def __init__(self, session, dir):
        self.session = session
        self.analysiserOnline = False
        self.__loadDumpData(dir)
        print('loaded')
        self.__checkAnalysiserStatus()

    def start(self):
        while True:
            try:
                self.__checkAnalysiserStatus()
                time.sleep(0.1)
            except BaseException as e:
                print("Exception caught: {}".format(e))

    def __checkAnalysiserStatus(self):
        sessionInformation = self.session.blockingInvoker().sessionsInformation()
        sessionNames = [si[1] for si in sessionInformation]
        isOnline = sessionNames.__contains__('MDIQKD-Analysiser')
        if (not self.analysiserOnline) and isOnline:
            self.__sendDumpData()
        self.analysiserOnline = isOnline

    def __sendDumpData(self):
        print('sending...')
        for i in range(0, max(len(self.QBERs), len(self.channels))):
            if i < len(self.QBERs):
                self.session.blockingInvoker('MDIQKD-Analysiser').dumpQBER(self.QBERs[i])
            if i < len(self.channels):
                self.session.blockingInvoker('MDIQKD-Analysiser').dumpChannel(self.channels[i])
            time.sleep(0.2)
        print('done')

    def __loadDumpData(self, dir):
        self.QBERs = self.__loadMsgpackEntries('{}/QBERs.dump'.format(dir))
        self.channels = self.__loadMsgpackEntries('{}/Channel.dump'.format(dir))

    def __loadMsgpackEntries(self, path):
        file = open(path, 'rb')
        data = file.read(os.path.getsize(path))
        file.close()
        unpacker = msgpack.Unpacker(raw=False)
        unpacker.feed(data)
        entries = []
        for packed in unpacker:
            entries.append(packed)
        return entries


if __name__ == '__main__':
    session = Pydra.Session.newSession(('localhost', 20102))
    dumpSource = DumpSource(session, '/Users/Hwaipy/Downloads/MDIQKD-shaixuan/')
    dumpSource.start()
    session.stop()
