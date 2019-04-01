from pymongo import MongoClient
import time

class ResultAnalysiser:
    def __init__(self, db):
        self.db = db
        self.reports = self.db['TDCReport']
        self.monitor = self.db['ChannelMonitor']

    def getDocumentCount(self):
        return self.reports.count()

    def getNewestDocumentTime(self):
        docSet = self.reports.find().sort("SystemTime", -1).limit(1)
        return docSet[0]["SystemTime"]

    def getQbers(self, start, stop):
        docSet = self.getDocumentsInRange(self.reports, "SystemTime", start, stop)
        print(len(docSet))

    def getDocumentsInRange(self, collection, keyWord, start, stop):
        docSet = collection.find({"$and": [{keyWord: { "$gt": start } },{keyWord: { "$lt": stop } }]})
        docs = [doc for doc in docSet]
        return docs

if __name__ == '__main__':
    client = MongoClient('mongodb://MDIQKD:freespace@192.168.25.27:27019')
    db = client['Freespace_MDI_QKD']
    ana = ResultAnalysiser(db)
    # print(ana.getDocumentCount())
    lastTime = ana.getNewestDocumentTime()
    print('Last Time is {}'.format(lastTime))

    validDocs = ana.getQbers(lastTime-15000, lastTime-10000)
    # print(reports.count())
    # print(monitor.count())
    # print(reports.find_one())
    # print(monitor.find_one())
    # print(time.time()/3600-1553952980524/3600000)
    #
