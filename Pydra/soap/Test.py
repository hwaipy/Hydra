from Pydra import Session
import time
import msgpack
import random

if __name__ == '__main__':
    session = Session.newSession(("192.168.25.27", 20102))

    session.blockingInvoker('GroundTDCService').configureAnalyser('Histogram', {'SyncFrac': 10})
    session.blockingInvoker('GroundTDCService').configureAnalyser('MDIQKDEncoding', {'TriggerFrac': 10})
    session.blockingInvoker('GroundTDCService').configureAnalyser('MDIQKDQBER', {'TriggerFrac': 10})

    session.stop()