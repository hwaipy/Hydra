package com.hydra.services.tdc.test

import java.net.Socket
import java.io.{PrintWriter, RandomAccessFile}
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import com.hydra.services.tdc.{DataAnalyser, DataBlock, TDCProcessService}
import com.hydra.`type`.NumberTypeConversions._
import com.hydra.services.tdc.application.{Coincidence, RandomNumber}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.Random

object LocalTDCDataFeeder {
  private val localDataFilePath = "C:\\Users\\Administrator\\Desktop\\20190807212438-A.dat"
  val localDataStorage = "D:\\Experiments\\MDIQKD\\Local\\raw"
  private val rndFilePath = "D:\\Dropbox\\Labwork\\Projects\\_2017-10-26 MDI-QKD\\2019-09-12 10km文章\\4.其它数据\\2.HOM计算\\3.RandomNumbers\\rnd.csv"
  val rnds = Source.fromFile(rndFilePath).getLines().toList.map(line => line.split(", *").map(_.toInt))
  val offset = 4343l * 6250l * 1 << 28 //7286344908800000

  def feed(process: TDCProcessService, port: Int) = {

    process.turnOnAnalyser("MDIQKDReviewer", Map())
    process.turnOnAnalyser("Counter")
    process.turnOnAnalyser("Histogram", Map("Sync" -> 0, "Signal" -> 8, "ViewStart" -> 0, "ViewStop" -> 10000000, "Divide" -> 1000))
    process.setDelays(process.getDelays().map(_ => offset))
    process.setDelay(0, 2100 + offset - 1428 * 10000)
    process.setDelay(8, 0 + offset)
    process.setDelay(9, -507400 + offset)
    process.dataTDA.unitEndTime = 7291000015499875l - offset

    val socket = new Socket("localhost", port)
    val outputStream = socket.getOutputStream
    val raf = new RandomAccessFile(localDataFilePath, "r")
    raf.skipBytes(4096 * 500000)
    raf.skipBytes(4096 * 500000)
    raf.skipBytes(4096 * 500000)
    raf.skipBytes(4096 * 500000)
    raf.skipBytes(4096 * 70000)
    val buffer = new Array[Byte](20000000)
    //    val size = new File(localDataFilePath).length()

    val deepth = 200000000
    Range(0, deepth).foreach(_ => {
      raf.read(buffer)
      outputStream.write(buffer)
      Thread.sleep(3000)
    })
    raf.close()
    outputStream.flush()
    socket.close()
  }
}


class MDIQKDReviewer(channelCount: Int) extends DataAnalyser {
  configuration("AliceRandomNumbers") = LocalTDCDataFeeder.rnds.map(_ (0))
  configuration("BobRandomNumbers") = LocalTDCDataFeeder.rnds.map(_ (1))
  configuration("Period") = 10000.0
  configuration("Delay") = 3000.0
  configuration("TriggerChannel") = 0
  configuration("TriggerFrac") = 1
  configuration("Channel 1") = 8
  configuration("Channel 2") = 9
  configuration("Channel Monitor Alice") = 4
  configuration("Channel Monitor Bob") = 5
  configuration("Gate") = 2000.0
  configuration("PulseDiff") = 3000.0
  configuration("QBERSectionCount") = 1000
  //  configuration("ChannelMonitorSyncChannel") = 2

  override def configure(key: String, value: Any) = key match {
    case "AliceRandomNumbers" => value.asInstanceOf[List[Int]] != null
    case "BobRandomNumbers" => value.asInstanceOf[List[Int]] != null
    case "Period" => {
      val sc: Double = value
      sc > 0
    }
    case "Delay" => {
      val sc: Double = value
      true
    }
    case "Gate" => {
      val sc: Double = value
      true
    }
    case "PulseDiff" => {
      val sc: Double = value
      true
    }
    case "Channel 1" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    case "Channel 2" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    case "Channel Monitor Alice" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    case "Channel Monitor Bob" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    case "TriggerChannel" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    case "TriggerFrac" => {
      val sc: Int = value
      sc > 0
    }
    case "QBERSectionCount" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    //    case "ChannelMonitorSyncChannel" => {
    //      val sc: Int = value
    //      sc >= 0 && sc < channelCount
    //    }
  }

  private val DEBUG_QBERs = Range(0, 10000).map(_ => Array(0, 0)).toArray
  private val DEBUG_DISTRIBUTIONS = Range(0, 10000).map(_ => Array(0, 0, 0)).toArray
  private var DEBUG_DATABLOCK_COUNT = 0
  private var DEBUG_HOM_CENTER = 0.0
  private var DEBUG_HOM_SIDE = 0.0

  override protected def analysis(dataBlock: DataBlock) = {
    println(s"-------- DataBlock ${DEBUG_DATABLOCK_COUNT} --------")
    DEBUG_DATABLOCK_COUNT += 1
    val randomNumbersAlice = configuration("AliceRandomNumbers").asInstanceOf[List[Int]].map(r => new RandomNumber(r)).toArray
    val randomNumbersBob = configuration("BobRandomNumbers").asInstanceOf[List[Int]].map(r => new RandomNumber(r)).toArray
    val period: Double = configuration("Period")
    val delay: Double = configuration("Delay")
    val triggerChannel: Int = configuration("TriggerChannel")
    val triggerFrac: Int = configuration("TriggerFrac")
    val channel1: Int = configuration("Channel 1")
    val channel2: Int = configuration("Channel 2")
    val channelMonitorAlice: Int = configuration("Channel Monitor Alice")
    val channelMonitorBob: Int = configuration("Channel Monitor Bob")
    val gate: Double = configuration("Gate")
    val pulseDiff: Double = configuration("PulseDiff")
    val qberSectionCount: Int = configuration("QBERSectionCount")
    //    val channelMonitorSyncChannel: Int = configuration("ChannelMonitorSyncChannel")

    val triggerList = dataBlock.content(triggerChannel).zipWithIndex.filter(_._2 % triggerFrac == 0).map(_._1)
    val signalList1 = dataBlock.content(channel1)
    val signalList2 = dataBlock.content(channel2)
    val signalListAlice = dataBlock.content(channelMonitorAlice)
    val signalListBob = dataBlock.content(channelMonitorBob)

    val item1s = analysisSingleChannel(triggerList, signalList1, period, delay, gate, pulseDiff, randomNumbersAlice.size)
    val item2s = analysisSingleChannel(triggerList, signalList2, period, delay, gate, pulseDiff, randomNumbersBob.size)
    val validItem1s = item1s.filter(_._3 >= 0)
    val validItem2s = item2s.filter(_._3 >= 0)

    def generateCoincidences(iterator1: Iterator[Tuple4[Long, Long, Int, Long]], iterator2: Iterator[Tuple4[Long, Long, Int, Long]]) = {
      val item1Ref = new AtomicReference[Tuple4[Long, Long, Int, Long]]()
      val item2Ref = new AtomicReference[Tuple4[Long, Long, Int, Long]]()

      def fillRef = {
        if (item1Ref.get == null && iterator1.hasNext) item1Ref set iterator1.next
        if (item2Ref.get == null && iterator2.hasNext) item2Ref set iterator2.next
        item1Ref.get != null && item2Ref.get != null
      }

      val resultBuffer = new ArrayBuffer[Coincidence]()
      while (fillRef) {
        val item1 = item1Ref.get
        val item2 = item2Ref.get
        if (item1._1 > item2._1) item2Ref set null
        else if (item1._1 < item2._1) item1Ref set null
        else if (item1._2 > item2._2) item2Ref set null
        else if (item1._2 < item2._2) item1Ref set null
        else {
          resultBuffer += new Coincidence(item1._3, item2._3, randomNumbersAlice(item1._2 % randomNumbersAlice.size), randomNumbersBob(item1._2 % randomNumbersBob.size), item1._4, item1._1, item1._2)
          item1Ref set null
          item2Ref set null
        }
      }
      resultBuffer.toList
    }

    val coincidences = generateCoincidences(validItem1s.iterator, validItem2s.iterator)
    //    println(s"${validItem1s.size}, ${validItem2s.size}, ${validItem1s.size.toDouble * validItem2s.size.toDouble / 1e8}, ${coincidences.size}, ")
    //    coincidences.foreach(coincidence => {
    //      val pulseIndex = coincidence.pulseIndex.toInt
    //      DEBUG_QBERs(pulseIndex)(if (coincidence.r1 == coincidence.r2) 1 else 0) += 1
    //    })

    //    (item1s ++ item2s).foreach(item => if (item._2 >= 0 && item._2 < DEBUG_DISTRIBUTIONS.size) DEBUG_DISTRIBUTIONS(item._2)((item._3 + 3) % 3) += 1)
    //    if (Random.nextDouble() < 0.1) {
    //      println("Saved")
    //      val out = new PrintWriter("C:\\Users\\Administrator\\Desktop\\20190807212438-A.csv")
    //      //      DEBUG_QBERs.foreach(qber => out.println(s"${qber(0)}, ${qber(1)}"))
    //      DEBUG_DISTRIBUTIONS.foreach(d => out.println(s"${d(0)}, ${d(1)}, ${d(2)}"))
    //      out.close()
    //    }

    val basisMatchedCoincidences = coincidences.filter(_.basisMatched)
    //    val validCoincidences = coincidences.filter(_.valid)

    val map = mutable.HashMap[String, Any]("Delay" -> delay, "Period" -> period, "Gate" -> gate, "PulseDiff" -> pulseDiff)
    map.put("Count 1", item1s.size)
    map.put("Valid Count 1", validItem1s.size)
    map.put("Count 2", item2s.size)
    map.put("Valid Count 2", validItem2s.size)
    //    map.put("Coincidence Count", coincidences.size)
    //    map.put("Basis Matched Coincidence Count", basisMatchedCoincidences.size)
    //    map.put("Valid Coincidence Count", validCoincidences.size)
    //
    //    println(qberSectionCount)
    //    val basisStrings = List("O", "X", "Y", "Z")
    //    val qberSections = Range(0, qberSectionCount).toArray.map(i => new Array[Int](4 * 4 * 2))
    //    Range(0, 4).foreach(basisAlice => Range(0, 4).foreach(basisBob => {
    //      val coincidences = validCoincidences.filter(c => c.randomNumberAlice.intensity == basisAlice && c.randomNumberBob.intensity == basisBob)
    //      map.put(s"${basisStrings(basisAlice)}-${basisStrings(basisBob)}, Correct", coincidences.filter(_.isCorrect).size)
    //      map.put(s"${basisStrings(basisAlice)}-${basisStrings(basisBob)}, Wrong", coincidences.filterNot(_.isCorrect).size)
    //    }))
    //    validCoincidences.foreach(c => {
    //      val sectionIndex = ((c.triggerTime - dataBlock.dataTimeBegin).toDouble / (dataBlock.dataTimeEnd - dataBlock.dataTimeBegin) * qberSectionCount).toInt
    //      val category = (c.randomNumberAlice.intensity * 4 + c.randomNumberBob.intensity) * 2 + (if (c.isCorrect) 0 else 1)
    //      if (sectionIndex >= 0 && sectionIndex < qberSections.size) qberSections(sectionIndex)(category) += 1
    //    })
    //    map.put(s"QBER Sections", qberSections)
    //    map.put(s"QBER Sections Detail", s"1000*32 Array. 1000 for 1000 sections. 32 for (Alice[O,X,Y,Z] * 4 + Bob[O,X,Y,Z]) * 2 + (0 for Correct and 1 for Wrong)")
    //
//    val ccs0XXCoincidences = basisMatchedCoincidences.filter(c => c.randomNumberAlice.isX && c.randomNumberBob.isX).filter(c => (c.r1 == 0) && (c.r2 == 0))
//    val ccsOXXtherCoincidences = Range(10, 15).toList.map(delta => generateCoincidences(validItem1s.iterator, validItem2s.map(i => (i._1 + delta, i._2, i._3, i._4)).iterator)
//      .filter(_.basisMatched).filter(c => c.randomNumberAlice.isX && c.randomNumberBob.isX).filter(c => (c.r1 == 0) && (c.r2 == 0)))
//    val ccs0XX = ccs0XXCoincidences.size
//    val ccsOXXther = ccsOXXtherCoincidences.map(_.size)
//    map.put("X-X, 0&0 with delays", List(ccs0XX, ccsOXXther.sum.toDouble / ccsOXXther.size))
//    DEBUG_HOM_CENTER += ccs0XX
//    DEBUG_HOM_SIDE += ccsOXXther.sum.toDouble / ccsOXXther.size
//    println(s"X-X, 0&0: ${DEBUG_HOM_CENTER}/${DEBUG_HOM_SIDE} = ${DEBUG_HOM_CENTER / DEBUG_HOM_SIDE}")

    val ccs0Z0Z0Coincidences = basisMatchedCoincidences.filter(c => c.randomNumberAlice.isZ && c.randomNumberBob.isZ).filter(c => (c.r1 == 0) && (c.r2 == 0))
    val ccsOZ0Z0OtherCoincidences = Range(10, 15).toList.map(delta => generateCoincidences(validItem1s.iterator, validItem2s.map(i => (i._1 + delta, i._2, i._3, i._4)).iterator)
      .filter(_.basisMatched).filter(c => c.randomNumberAlice.isZ && c.randomNumberBob.isZ).filter(c => (c.r1 == 0) && (c.r2 == 0)))
    val ccs0Z0Z0 = ccs0Z0Z0Coincidences.size
    val ccsOZ0Z0Other = ccsOZ0Z0OtherCoincidences.map(_.size)
    DEBUG_HOM_CENTER += ccs0Z0Z0
    DEBUG_HOM_SIDE += ccsOZ0Z0Other.sum.toDouble / ccsOZ0Z0Other.size
    println(s"Z-Z, 0&0: ${DEBUG_HOM_CENTER}/${DEBUG_HOM_SIDE} = ${DEBUG_HOM_CENTER / DEBUG_HOM_SIDE}")

    //    val ccs0YYCoincidences = basisMatchedCoincidences.filter(c => c.randomNumberAlice.isY && c.randomNumberBob.isY).filter(c => (c.r1 == 0) && (c.r2 == 0))
    //    val ccsOYYtherCoincidences = Range(10, 15).toList.map(delta => generateCoincidences(validItem1s.iterator, validItem2s.map(i => (i._1 + delta, i._2, i._3, i._4)).iterator)
    //      .filter(_.basisMatched).filter(c => c.randomNumberAlice.isY && c.randomNumberBob.isY).filter(c => (c.r1 == 0) && (c.r2 == 0)))
    //    val ccs0YY = ccs0YYCoincidences.size
    //    val ccsOYYther = ccsOYYtherCoincidences.map(_.size)
    //    map.put("Y-Y, 0&0 with delays", List(ccs0YY, ccsOYYther.sum.toDouble / ccsOYYther.size))
    //
    //    val ccsAll0Coincidences = coincidences.filter(c => (c.r1 == 0) && (c.r2 == 0))
    //    val ccsAllOtherCoincidences = Range(10, 15).toList.map(delta => generateCoincidences(validItem1s.iterator, validItem2s.map(i => (i._1 + delta, i._2, i._3, i._4)).iterator).filter(c => (c.r1 == 0) && (c.r2 == 0)))
    //    val ccsAll0 = ccsAll0Coincidences.size
    //    val ccsAllOther = ccsAllOtherCoincidences.map(_.size)
    //    //    map.put("All, 0&0 with delays", List(ccsAll0, ccsAllOther.sum.toDouble / ccsAllOther.size))
    //    println(s"ALL00: $ccsAll0, ${ccsAllOther.sum.toDouble / ccsAllOther.size}")
    //    println(Range(-21, 22).toList.map(delta => generateCoincidences(validItem1s.iterator, validItem2s.map(i => (i._1 + delta, i._2, i._3, i._4)).iterator).filter(c => (c.r1 == 0) && (c.r2 == 0))).map(_.size))
    //    def statisticCoincidenceSection(cll: List[List[Coincidence]]) = {
    //      val sections = new Array[Int](qberSectionCount)
    //      cll.foreach(cl => cl.foreach(c => {
    //        val sectionIndex = ((c.triggerTime - dataBlock.dataTimeBegin).toDouble / (dataBlock.dataTimeEnd - dataBlock.dataTimeBegin) * qberSectionCount).toInt
    //        if (sectionIndex >= 0 && sectionIndex < qberSections.size) sections(sectionIndex) += 1
    //      }))
    //      sections.map(c => c.toDouble / cll.size)
    //    }
    //
    //    val homSections = Array(List(ccs0XXCoincidences), ccsOXXtherCoincidences, List(ccs0YYCoincidences), ccsOYYtherCoincidences, List(ccsAll0Coincidences), ccsAllOtherCoincidences).map(statisticCoincidenceSection)
    //    map.put(s"HOM Sections", homSections)
    //    map.put(s"HOM Sections Detail", s"4*100 Array. 100 for 100 sections. 4 for: X-X, 0&0 without and with delays; All, 0&0 without and with delays")
    //
    //    val ccsAllOtherLeftCoincidences = Range(-14, 10).toList.map(delta => generateCoincidences(validItem1s.iterator, validItem2s.map(i => (i._1 + delta, i._2, i._3, i._4)).iterator).filter(c => (c.r1 == 0) && (c.r2 == 0)))
    //    val ccsAllOtherOverallCoincidences = ccsAllOtherLeftCoincidences ++ ccsAllOtherCoincidences
    //
    //    def statisticCoincidenceSections(cl: List[Coincidence]) = {
    //      val sections = new Array[Int](qberSectionCount)
    //      cl.foreach(c => {
    //        val sectionIndex = ((c.triggerTime - dataBlock.dataTimeBegin).toDouble / (dataBlock.dataTimeEnd - dataBlock.dataTimeBegin) * qberSectionCount).toInt
    //        if (sectionIndex >= 0 && sectionIndex < qberSections.size) sections(sectionIndex) += 1
    //      })
    //      sections
    //    }
    //    val ccsAllOtherOverallCoincidencesSections = ccsAllOtherOverallCoincidences.map(statisticCoincidenceSections)
    //    map.put(s"HOM Detailed Sections", ccsAllOtherOverallCoincidencesSections)
    //
    //    val channelMonitorSyncList = dataBlock.content(channelMonitorSyncChannel)
    //    map.put("ChannelMonitorSync", Array[Long](dataBlock.dataTimeBegin, dataBlock.dataTimeEnd) ++ (channelMonitorSyncList.size match {
    //      case s if s > 10 => {
    //        println("Error: counting rate at ChannelMonitorSyncChannel exceed 10!")
    //        new Array[Long](0)
    //      }
    //      case s => channelMonitorSyncList
    //    }))

    val countSections = Range(0, qberSectionCount).toArray.map(i => new Array[Int](2))
    List(signalListAlice, signalListBob).zipWithIndex.foreach(z => z._1.foreach(event => {
      val sectionIndex = ((event - dataBlock.dataTimeBegin - LocalTDCDataFeeder.offset).toDouble / (dataBlock.dataTimeEnd - dataBlock.dataTimeBegin) * qberSectionCount).toInt
      if (sectionIndex >= 0 && sectionIndex < qberSectionCount) countSections(sectionIndex)(z._2) += 1
    }))
    map.put(s"Count Sections", countSections)
    map.put(s"Count Sections Detail", s"100*2 Array. 100 for 100 sections. 2 for signalList1 and signalList2")

    map.toMap
  }

  private def analysisSingleChannel(triggerList: Array[Long], signalList: Array[Long], period: Double, delay: Double, gate: Double, pulseDiff: Double, randomNumberSize: Int) = {
    val triggerIterator = triggerList.iterator
    val currentTriggerRef = new AtomicLong(if (triggerIterator.hasNext) triggerIterator.next() else 0)
    val nextTriggerRef = new AtomicLong(if (triggerIterator.hasNext) triggerIterator.next() else Long.MaxValue)
    val meta = signalList.map(time => {
      while (time >= nextTriggerRef.get) {
        currentTriggerRef set nextTriggerRef.get
        nextTriggerRef.set(if (triggerIterator.hasNext) triggerIterator.next() else Long.MaxValue)
      }
      val pulseIndex = ((time - currentTriggerRef.get) / period).toLong
      val delta = (time - currentTriggerRef.get - period * pulseIndex).toLong
      val p = if (math.abs(delta - delay) < gate / 2) 0 else if (math.abs(delta - delay - pulseDiff) < gate / 2) 1 else -1
      ((currentTriggerRef.get / period / randomNumberSize).toLong, pulseIndex, p, currentTriggerRef.get)
    })
    meta
  }
}
