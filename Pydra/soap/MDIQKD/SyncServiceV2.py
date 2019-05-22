import sys
import Utils
import Pydra
import time
import math
import threading
from pymongo import MongoClient


class Syncer:
    def __init__(self, db, period):
        self.db = db
        self.period = period
        self.reports = self.db['TDCReport']
        self.syncDB = self.db['TimeSync']
        self.session = None
        self.aliceOn = False
        self.bobOn = False
        self.channelAlice = 0
        self.channelBob = 0
        self.channelCharlie = 0
        self.running = True
        self.HMC7044EvalAlice = None
        self.HMC7044EvalBob = None
        self.HMC7044EvalCharlie = None

    def start(self):
        threading.Thread(target=self.__run).start()

    def __run(self):
        while self.running:
            time.sleep(self.period)
            rises = self.__fetchRecentReports(1)[0]
            self.__updateDelays(rises[0]-2, rises[1]-2, rises[2])

    def __fetchRecentReports(self, count):
        docSet = [[doc['aliceRise'], doc['bobRise'], doc['Time']]
                  for doc in self.reports.find().sort("SystemTime", -1).limit(count)]
        return docSet

    def __updateDelays(self, aliceDelay, bobDelay, tdcTime):
        print('update delays {}, {}'.format(aliceDelay, bobDelay))
        charlieDelay = 0
        if (not self.aliceOn) or math.isnan(aliceDelay): aliceDelay = 0
        if (not self.bobOn) or math.isnan(bobDelay): bobDelay = 0
        if aliceDelay < 0:
            bobDelay -= aliceDelay
            charlieDelay -= aliceDelay
            aliceDelay = 0
        if bobDelay < 0:
            aliceDelay -= bobDelay
            charlieDelay -= bobDelay
            bobDelay = 0
        if self.aliceOn or self.bobOn:
            # self.log('adjust delays: alice {}, bob {}, charlie {}.'.format(aliceDelay, bobDelay, charlieDelay))
            print('adjust delays: alice {}, bob {}, charlie {}.'.format(aliceDelay, bobDelay, charlieDelay))
            #try:
            self.HMC7044EvalAlice.setDelay(0, -aliceDelay)
            self.HMC7044EvalBob.setDelay(0, -bobDelay)
            print(self.channelCharlie)
            print(charlieDelay)
            self.HMC7044EvalCharlie.setDelay(0, -charlieDelay)
            x = self.syncDB.insert_one({"TDCTime": tdcTime, "AliceDelay": (aliceDelay - charlieDelay),
                                    "BobDelay": (bobDelay - charlieDelay)})
            print(x.inserted_id)
            #except BaseException as e:
            #   print('Error in set delays: {}.'.format(e))

    def stop(self):
        self.running = False
        self.session.stop()

    def turnOnAlice(self, channel):
        print('turn on alice')
        self.aliceOn = True
        self.predictAlice = 0
        self.channelAlice = channel

    def turnOffAlice(self):
        print('turn off alice')
        self.aliceOn = False

    def turnOnBob(self, channel):
        print('turn on bob')
        self.bobOn = True
        self.predictBob = 0
        self.channelBob = channel

    def turnOffBob(self):
        print('turn off bob')
        self.bobOn = False

    def configCharlie(self, channel):
        self.channelCharlie = channel

    def log(self, msg):
        file = open('log.txt', mode='a')
        file.write('[{}]{}'.format(time.time(), msg))
        if msg[-1] is not '\n':
            file.write('\n')
        file.close()


if __name__ == '__main__':
    client = MongoClient('mongodb://MDIQKD:freespace@192.168.25.27:27019')
    db = client['Freespace_MDI_QKD']
    syncer = Syncer(db, 1)
    sysArgs = Utils.SystemArguments(sys.argv)
    server = sysArgs.get('server', '192.168.25.27')
    port = sysArgs.get('port', '20102')
    clientName = sysArgs.get('clientName', 'SyncService')
    session = Pydra.Session.newSession((server, int(port)), syncer, "MDI-Syncer")
    syncer.session = session
    syncer.HMC7044EvalAlice = session.blockingInvoker('HMC7044EvalAlice')
    syncer.HMC7044EvalBob = session.blockingInvoker('HMC7044EvalBob')
    syncer.HMC7044EvalCharlie = session.blockingInvoker('HMC7044EvalCharlie')
    syncer.turnOnAlice(0)
    syncer.turnOnBob(0)
    syncer.start()
    # time.sleep(5)
    # syncer.stop()
