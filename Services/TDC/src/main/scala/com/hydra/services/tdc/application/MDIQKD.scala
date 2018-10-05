package com.hydra.services.tdc.application

import com.hydra.services.tdc.{DataAnalyser, DataBlock}
import com.hydra.`type`.NumberTypeConversions._

class MDIQKDAnalyser(channelCount: Int) extends DataAnalyser {
  configuration("RandomNumbers") = List(1)
  configuration("Period") = 10000.0
  configuration("StartTime") = 0

  override def configure(key: String, value: Any) = key match {
    case "RandomNumbers" => {
      val sc = value.asInstanceOf[List[Int]]
      true
    }
    case "Period" => {
      val sc: Double = value
      sc > 0
    }
    case "StartTime" => {
      val sc: Long = value
      true
    }
  }

  override protected def analysis(dataBlock: DataBlock) = {
    //    val deltas = new ArrayBuffer[Long]()
    //    val syncChannel: Int = configuration("Sync")
    //    val signalChannel: Int = configuration("Signal")
    //    val viewStart: Long = configuration("ViewStart")
    //    val viewStop: Long = configuration("ViewStop")
    //    val binCount: Int = configuration("BinCount")
    //    val divide: Int = configuration("Divide")
    //    val tList = dataBlock.content(syncChannel)
    //    val sList = dataBlock.content(signalChannel)
    //    val viewFrom = viewStart
    //    val viewTo = viewStop
    //    if (tList.size > 0 && sList.size > 0) {
    //      var preStartT = 0
    //      val lengthT = tList.size
    //      sList.foreach(s => {
    //        var cont = true
    //        while (preStartT < lengthT && cont) {
    //          val t = tList(preStartT)
    //          val delta = s - t
    //          if (delta > viewTo) {
    //            preStartT += 1
    //          } else cont = false
    //        }
    //        var tIndex = preStartT
    //        cont = true
    //        while (tIndex < lengthT && cont) {
    //          val t = tList(tIndex)
    //          val delta = s - t
    //          if (delta > viewFrom) {
    //            deltas += delta
    //            tIndex += 1
    //          } else cont = false
    //        }
    //      })
    //    }
    //    val histo = new Histogram(deltas.toArray, binCount, viewFrom, viewTo, divide)
    //    Map[String, Any]("SyncChannel" -> syncChannel, "SignalChannel" -> signalChannel,
    //      "ViewFrom" -> viewFrom, "ViewTo" -> viewTo, "Divide" -> divide, "Histogram" -> histo.yData.toList)
  }


  class RandomNumber(private val RN: Int) {
    def isVacuum = (RN & 0x8) == 0

    def isSignal = (RN & 0x4) > 0

    def isTime = (RN & 0x2) > 0

    def encode = RN & 0x1
  }

}