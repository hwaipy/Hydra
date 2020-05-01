from Pydra import Session
import time
import msgpack
import random

if __name__ == '__main__':
    session = Session.newSession(("192.168.25.27", 20102))

    # session.blockingInvoker('GroundTDCService').configureAnalyser('Histogram', {'SyncFrac': 10})
    session.blockingInvoker('GroundTDCService').configureAnalyser('MDIQKDEncoding', {'Period': 4000})
    # session.blockingInvoker('GroundTDCService').configureAnalyser('MDIQKDQBER', {'Period': 4000})


  # configuration("RandomNumbers") = List(1)
  # configuration("Period") = 10000.0
  # configuration("Delay") = 0
  # configuration("TriggerChannel") = 0
  # configuration("SignalChannel") = 1
  # configuration("TimeAliceChannel") = 4
  # configuration("TimeBobChannel") = 5
  # configuration("BinCount") = 100
  # configuration("TriggerFrac") = 1


  # configuration("Delay") = 3000.0
  # configuration("TriggerChannel") = 0
  # configuration("TriggerFrac") = 1
  # configuration("Channel 1") = 1
  # configuration("Channel 2") = 3
  # configuration("Channel Monitor Alice") = 4
  # configuration("Channel Monitor Bob") = 5
  # configuration("Gate") = 2000.0
  # configuration("PulseDiff") = 3000.0
  # configuration("QBERSectionCount") = 1000
  # configuration("ChannelMonitorSyncChannel") = 2


    session.stop()