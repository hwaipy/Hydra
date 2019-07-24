package com.hydra.services.tdc.application

import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import com.hydra.services.tdc.{DataAnalyser, DataBlock, Histogram}
import com.hydra.`type`.NumberTypeConversions._

import scala.util.Random
//import org.mongodb.scala.{Completed, MongoClient, Observer}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class MDIQKDEncodingAnalyser(channelCount: Int) extends DataAnalyser {
  configuration("RandomNumbers") = List(1)
  configuration("Period") = 10000.0
  configuration("Delay") = 0
  configuration("TriggerChannel") = 0
  configuration("SignalChannel") = 1
  configuration("TimeAliceChannel") = 4
  configuration("TimeBobChannel") = 5
  configuration("BinCount") = 100

  override def configure(key: String, value: Any) = key match {
    case "RandomNumbers" => value.asInstanceOf[List[Int]] != null
    case "Period" => {
      val sc: Double = value
      sc > 0
    }
    case "Delay" => {
      val sc: Double = value
      true
    }
    case "SignalChannel" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    case "TriggerChannel" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    case "TimeAliceChannel" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    case "TimeBobChannel" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    case "BinCount" => {
      val sc: Int = value
      sc > 0 && sc < 2000
    }
  }

  override protected def analysis(dataBlock: DataBlock) = {
    val randomNumbers = configuration("RandomNumbers").asInstanceOf[List[Int]].map(r => new RandomNumber(r)).toArray
    val period: Double = configuration("Period")
    val delay: Double = configuration("Delay")
    val triggerChannel: Int = configuration("TriggerChannel")
    val timeAliceChannel: Int = configuration("TimeAliceChannel")
    val timeBobChannel: Int = configuration("TimeBobChannel")
    val signalChannel: Int = configuration("SignalChannel")
    val binCount: Int = configuration("BinCount")
    val map = mutable.HashMap[String, Any]("TriggerChannel" -> triggerChannel, "SignalChannel" -> signalChannel, "Delay" -> delay, "Period" -> period)

    val signalList = dataBlock.content(signalChannel)
    val triggerList = dataBlock.content(triggerChannel)
    val timeAliceList = dataBlock.content(timeAliceChannel)
    val timeBobList = dataBlock.content(timeBobChannel)
    val meta = this.meta(signalList, triggerList, delay, period, randomNumbers)
    RandomNumber.ALL_RANDOM_NUMBERS.foreach(rn => {
      val validMeta = meta.filter(z => z._1 == rn)
      val histoPulse = new Histogram(validMeta.map(_._2), binCount, 0, period.toLong, 1)
      map.put(s"Histogram With RandomNumber[${rn.RN}]", histoPulse.yData.toList)
      map.put(s"Count of RandomNumber[${rn.RN}]", randomNumbers.map(_.RN).count(_ == rn.RN))
    })
    val metaTimeAlice = this.meta(timeAliceList, triggerList, delay, period, randomNumbers)
    val histoTimeAlice = new Histogram(metaTimeAlice.map(_._2), binCount, 0, period.toLong, 1)
    map.put(s"Histogram Alice Time", histoTimeAlice.yData.toList)
    val metaTimeBob = this.meta(timeBobList, triggerList, delay, period, randomNumbers)
    val histoTimeBob = new Histogram(metaTimeBob.map(_._2), binCount, 0, period.toLong, 1)
    map.put(s"Histogram Bob Time", histoTimeBob.yData.toList)

    map.toMap
  }

  private def meta(signalList: Array[Long], triggerList: Array[Long], delay: Double, period: Double, randomNumbers: Array[RandomNumber]) = {
    val triggerIterator = triggerList.iterator
    val currentTriggerRef = new AtomicLong(if (triggerIterator.hasNext) triggerIterator.next() else 0)
    val nextTriggerRef = new AtomicLong(if (triggerIterator.hasNext) triggerIterator.next() else Long.MaxValue)
    signalList.map(time => {
      while (time >= nextTriggerRef.get) {
        currentTriggerRef set nextTriggerRef.get
        nextTriggerRef.set(if (triggerIterator.hasNext) triggerIterator.next() else Long.MaxValue)
      }
      val pulseIndex = ((time - delay - currentTriggerRef.get) / period).toLong
      val randomNumberIndex = (pulseIndex % randomNumbers.size).toInt
      val randomNumber = randomNumbers(if (randomNumberIndex >= 0) randomNumberIndex else randomNumberIndex + randomNumbers.size)
      val delta = (time - delay - currentTriggerRef.get - period * pulseIndex).toLong
      (randomNumber, delta)
    })
  }
}


class MDIQKDQBERAnalyser(channelCount: Int) extends DataAnalyser {
  private val random = new Random()
  configuration("AliceRandomNumbers") = Range(0, 1000).map(_ => random.nextInt(8)).toList
  configuration("BobRandomNumbers") = Range(0, 1000).map(_ => random.nextInt(8)).toList
  configuration("Period") = 10000.0
  configuration("Delay") = 3000.0
  configuration("TriggerChannel") = 0
  configuration("Channel 1") = 8
  configuration("Channel 2") = 9
  configuration("Channel Monitor Alice") = 4
  configuration("Channel Monitor Bob") = 5
  configuration("Gate") = 2000.0
  configuration("PulseDiff") = 3000.0
  configuration("QBERSectionCount") = 1000
  configuration("ChannelMonitorSyncChannel") = 2

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
    case "QBERSectionCount" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    case "ChannelMonitorSyncChannel" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
  }

  override protected def analysis(dataBlock: DataBlock) = {
    val randomNumbersAlice = configuration("AliceRandomNumbers").asInstanceOf[List[Int]].map(r => new RandomNumber(r)).toArray
    val randomNumbersBob = configuration("BobRandomNumbers").asInstanceOf[List[Int]].map(r => new RandomNumber(r)).toArray
    val period: Double = configuration("Period")
    val delay: Double = configuration("Delay")
    val triggerChannel: Int = configuration("TriggerChannel")
    val channel1: Int = configuration("Channel 1")
    val channel2: Int = configuration("Channel 2")
    val channelMonitorAlice: Int = configuration("Channel Monitor Alice")
    val channelMonitorBob: Int = configuration("Channel Monitor Bob")
    val gate: Double = configuration("Gate")
    val pulseDiff: Double = configuration("PulseDiff")
    val qberSectionCount: Int = configuration("QBERSectionCount")
    val channelMonitorSyncChannel: Int = configuration("ChannelMonitorSyncChannel")

    val triggerList = dataBlock.content(triggerChannel)
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
    val basisMatchedCoincidences = coincidences.filter(_.basisMatched)
    val validCoincidences = coincidences.filter(_.valid)

    val map = mutable.HashMap[String, Any]("Delay" -> delay, "Period" -> period, "Gate" -> gate, "PulseDiff" -> pulseDiff)
    map.put("Count 1", item1s.size)
    map.put("Valid Count 1", validItem1s.size)
    map.put("Count 2", item2s.size)
    map.put("Valid Count 2", validItem2s.size)
    map.put("Coincidence Count", coincidences.size)
    map.put("Basis Matched Coincidence Count", basisMatchedCoincidences.size)
    map.put("Valid Coincidence Count", validCoincidences.size)

    println(s"qberSectionCount: $qberSectionCount")
    val basisStrings = List("O", "X", "Y", "Z")
    val qberSections = Range(0, qberSectionCount).toArray.map(i => new Array[Int](4 * 4 * 2))
    Range(0, 4).foreach(basisAlice => Range(0, 4).foreach(basisBob => {
      val coincidences = validCoincidences.filter(c => c.randomNumberAlice.intensity == basisAlice && c.randomNumberBob.intensity == basisBob)
      map.put(s"${basisStrings(basisAlice)}-${basisStrings(basisBob)}, Correct", coincidences.filter(_.isCorrect).size)
      map.put(s"${basisStrings(basisAlice)}-${basisStrings(basisBob)}, Wrong", coincidences.filterNot(_.isCorrect).size)
    }))
    validCoincidences.foreach(c => {
      val sectionIndex = ((c.triggerTime - dataBlock.dataTimeBegin).toDouble / (dataBlock.dataTimeEnd - dataBlock.dataTimeBegin) * qberSectionCount).toInt
      val category = (c.randomNumberAlice.intensity * 4 + c.randomNumberBob.intensity) * 2 + (if (c.isCorrect) 0 else 1)
      if (sectionIndex >= 0 && sectionIndex < qberSections.size) qberSections(sectionIndex)(category) += 1
    })
    map.put(s"QBER Sections", qberSections)
    map.put(s"QBER Sections Detail", s"100*32 Array. 100 for 100 sections. 32 for (Alice[O,X,Y,Z] * 4 + Bob[O,X,Y,Z]) * 2 + (0 for Correct and 1 for Wrong)")

    val ccs0Coincidences = basisMatchedCoincidences.filter(c => c.randomNumberAlice.isX && c.randomNumberBob.isX).filter(c => (c.r1 == 0) && (c.r2 == 0))
    val ccsOtherCoincidences = Range(10, 15).toList.map(delta => generateCoincidences(validItem1s.iterator, validItem2s.map(i => (i._1 + delta, i._2, i._3, i._4)).iterator)
      .filter(_.basisMatched).filter(c => c.randomNumberAlice.isX && c.randomNumberBob.isX).filter(c => (c.r1 == 0) && (c.r2 == 0)))
    val ccs0 = ccs0Coincidences.size
    val ccsOther = ccsOtherCoincidences.map(_.size)
    map.put("X-X, 0&0 with delays", List(ccs0, ccsOther.sum.toDouble / ccsOther.size))
    val ccsAll0Coincidences = coincidences.filter(c => (c.r1 == 0) && (c.r2 == 0))
    val ccsAllOtherCoincidences = Range(10, 15).toList.map(delta => generateCoincidences(validItem1s.iterator, validItem2s.map(i => (i._1 + delta, i._2, i._3, i._4)).iterator).filter(c => (c.r1 == 0) && (c.r2 == 0)))
    val ccsAll0 = ccsAll0Coincidences.size
    val ccsAllOther = ccsAllOtherCoincidences.map(_.size)
    map.put("All, 0&0 with delays", List(ccsAll0, ccsAllOther.sum.toDouble / ccsAllOther.size))

    def statisticCoincidenceSection(cll: List[List[Coincidence]]) = {
      val sections = new Array[Int](qberSectionCount)
      cll.foreach(cl => cl.foreach(c => {
        val sectionIndex = ((c.triggerTime - dataBlock.dataTimeBegin).toDouble / (dataBlock.dataTimeEnd - dataBlock.dataTimeBegin) * qberSectionCount).toInt
        if (sectionIndex >= 0 && sectionIndex < qberSections.size) sections(sectionIndex) += 1
      }))
      sections.map(c => c.toDouble / cll.size)
    }

    val homSections = Array(List(ccs0Coincidences), ccsOtherCoincidences, List(ccsAll0Coincidences), ccsAllOtherCoincidences).map(statisticCoincidenceSection)
    map.put(s"HOM Sections", homSections)
    map.put(s"HOM Sections Detail", s"4*100 Array. 100 for 100 sections. 4 for: X-X, 0&0 without and with delays; All, 0&0 without and with delays")

    val channelMonitorSyncList = dataBlock.content(channelMonitorSyncChannel)
    map.put("ChannelMonitorSync", Array[Long](dataBlock.dataTimeBegin, dataBlock.dataTimeEnd) ++ (channelMonitorSyncList.size match {
      case s if s > 10 => {
        println("Error: counting rate at ChannelMonitorSyncChannel exceed 10!")
        new Array[Long](0)
      }
      case s => channelMonitorSyncList
    }))

    val countSections = Range(0, qberSectionCount).toArray.map(i => new Array[Int](2))
    List(signalListAlice, signalListBob).zipWithIndex.foreach(z => z._1.foreach(event => {
      val sectionIndex = ((event - dataBlock.dataTimeBegin).toDouble / (dataBlock.dataTimeEnd - dataBlock.dataTimeBegin) * qberSectionCount).toInt
      if (sectionIndex >= 0 && sectionIndex < qberSections.size) countSections(sectionIndex)(z._2) += 1
    }))
    map.put(s"Count Sections", countSections)
    map.put(s"Count Sections Detail", s"100*2 Array. 100 for 100 sections. 2 for signalList1 and signalList2")

    map.put("Time", dataBlock.creationTime)
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

object RandomNumber {
  def apply(rn: Int) = new RandomNumber(rn)

  val ALL_RANDOM_NUMBERS = Array(0, 1, 2, 3, 4, 5, 6, 7).map(RandomNumber(_))

  //  private val random = new Random()
  //
  //  def generateRandomNumbers(length: Int, signalRatio: Double, decoyRatio: Double) = Range(0, length).map(_ => {
  //    val amp = random.nextDouble() match {
  //      case d if d < signalRatio => 0xC
  //      case d if d < signalRatio + decoyRatio => 0x8
  //      case _ => 0x0
  //    }
  //    val encode = random.nextInt(4)
  //    new RandomNumber(amp | encode)
  //  }).toArray
}

class RandomNumber(val RN: Int) {
  def isVacuum = (RN / 2) == 0

  def isX = (RN / 2) == 1

  def isY = (RN / 2) == 2

  def isZ = (RN / 2) == 3

  def intensity = RN / 2

  def encode = RN % 2

  override def equals(obj: scala.Any): Boolean = obj match {
    case other: RandomNumber => other.RN == RN
    case _ => false
  }
}

class Coincidence(val r1: Int, val r2: Int, val randomNumberAlice: RandomNumber, val randomNumberBob: RandomNumber, val triggerTime: Long, val triggerIndex: Long, val pulseIndex: Long) {
  val basisMatched = randomNumberAlice.intensity == randomNumberBob.intensity
  val valid = (r1 == 0 && r2 == 1) || (r1 == 1 && r2 == 0)
  val isCorrect = randomNumberAlice.encode != randomNumberBob.encode
}