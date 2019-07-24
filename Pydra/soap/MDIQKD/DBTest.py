from pymongo import MongoClient
import math
import sys
import matplotlib.pyplot as plt


class ResultAnalysiser:
    def __init__(self, db, dir):
        self.db = db
        self.dir = dir
        self.reports = self.db['TDCReport-20190518']
        # self.monitor = self.db['ChannelMonitor-20190418']

    def getDocumentCount(self):
        return self.reports.count()

    def getNewestDocumentTime(self):
        docSet = self.reports.find().sort("SystemTime", -1).limit(1)
        return docSet[0]["SystemTime"]

    def getNewestDocumentBySort(self):
        docSet = self.reports.find().sort("$natural",-1).limit(100)
        return docSet[0]["SystemTime"]


if __name__ == '__main__':
    import time

    client = MongoClient('mongodb://MDIQKD:freespace@192.168.25.27:27019')
    db = client['Freespace_MDI_QKD']
    ana = ResultAnalysiser(db, dir)
    print('{} documents found in DB.'.format(ana.getDocumentCount()))

    t1 = time.time()
    lastTime = ana.getNewestDocumentTime()
    t2 = time.time()
    print('Last Time is {}'.format(lastTime))
    print('Finished in {} s'.format(t2-t1))

    t1 = time.time()
    lastTime = ana.getNewestDocumentBySort()
    t2 = time.time()
    print('Last Time is {}'.format(lastTime))
    print('Finished in {} s'.format(t2-t1))
