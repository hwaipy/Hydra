package com.hydra.services.tdc.application

import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import com.hydra.services.tdc.{DataAnalyser, DataBlock, Histogram}
import com.hydra.`type`.NumberTypeConversions._
import io.netty.handler.codec.http2.Http2HeadersEncoder.Configuration

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class MDIQKDEncodingAnalyser(channelCount: Int) extends DataAnalyser {
  configuration("RandomNumbers") = List(1)
  configuration("Period") = 10000.0
  configuration("Delay") = 0
  configuration("TriggerChannel") = 0
  configuration("SignalChannel") = 1
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
    val signalChannel: Int = configuration("SignalChannel")
    val binCount: Int = configuration("BinCount")

    val signalList = dataBlock.content(signalChannel)
    val triggerList = dataBlock.content(triggerChannel)
    val triggerIterator = triggerList.iterator
    val currentTriggerRef = new AtomicLong(if(triggerIterator.hasNext)triggerIterator.next() else 0)
    val nextTriggerRef = new AtomicLong(if(triggerIterator.hasNext)triggerIterator.next() else Long.MaxValue)
    val meta = signalList.map(time => {
      while(time>=nextTriggerRef.get){
        currentTriggerRef set nextTriggerRef.get
        nextTriggerRef.set(if(triggerIterator.hasNext)triggerIterator.next() else Long.MaxValue)
      }
      val pulseIndex = ((time - delay - currentTriggerRef.get) / period).toLong
      val randomNumberIndex = (pulseIndex % randomNumbers.size).toInt
      val randomNumber = randomNumbers(if (randomNumberIndex >= 0) randomNumberIndex else randomNumberIndex + randomNumbers.size)
      val delta = (time - delay - currentTriggerRef.get - period * pulseIndex).toLong
      (randomNumber, delta)
    })
    val map = mutable.HashMap[String, Any]("TriggerChannel" -> triggerChannel,"SignalChannel" -> signalChannel, "Delay" -> delay, "Period" -> period)
    RandomNumber.ALL_RANDOM_NUMBERS.foreach(rn => {
      val validMeta = meta.filter(z => z._1 == rn)
      val histoPulse = new Histogram(validMeta.map(_._2), binCount, 0, period.toLong, 1)
      map.put(s"Histogram With RandomNumber[${rn.RN}]", histoPulse.yData.toList)
      map.put(s"Count of RandomNumber[${rn.RN}]", randomNumbers.map(_.RN).count(_ == rn.RN))
    })
    map.toMap
  }
}


class MDIQKDQBERAnalyser(channelCount: Int) extends DataAnalyser {
  configuration("AliceRandomNumbers") = List(1)
  configuration("BobRandomNumbers") = List(1)
  configuration("Period") = 10000.0
  configuration("Delay") = 0
  configuration("Channel 1") = 0
  configuration("Channel 2") = 1
  configuration("Gate") = 2000
  configuration("PulseDiff") = 3000

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
  }

  override protected def analysis(dataBlock: DataBlock) = {
    val randomNumbersAlice = configuration("AliceRandomNumbers").asInstanceOf[List[Int]].map(r => new RandomNumber(r)).toArray
    val randomNumbersBob = configuration("BobRandomNumbers").asInstanceOf[List[Int]].map(r => new RandomNumber(r)).toArray
    val period: Double = configuration("Period")
    val delay: Double = configuration("Delay")
    val triggerChannel: Int = configuration("TriggerChannel")
    val channel1: Int = configuration("Channel 1")
    val channel2: Int = configuration("Channel 2")
    val gate: Double = configuration("Gate")
    val pulseDiff: Double = configuration("PulseDiff")

    val triggerList = dataBlock.content(triggerChannel)
    val signalList1 = dataBlock.content(channel1)
    val signalList2 = dataBlock.content(channel2)
    val item1s = analysisSingleChannel(triggerList, signalList1, period, delay, gate, pulseDiff)
    val item2s = analysisSingleChannel(triggerList, signalList2, period, delay, gate, pulseDiff)
    val validItem1s = item1s.filter(_._3 >= 0)
    val validItem2s = item2s.filter(_._3 >= 0)

    val iterator1 = validItem1s.iterator
    val iterator2 = validItem2s.iterator
    val item1Ref = new AtomicReference[Tuple3[Long, Long, Int]]()
    val item2Ref = new AtomicReference[Tuple3[Long, Long, Int]]()
    def fillRef = {
      if (item1Ref.get == null && iterator1.hasNext) item1Ref set iterator1.next
      if (item2Ref.get == null && iterator2.hasNext) item2Ref set iterator2.next
      item1Ref.get != null && item2Ref.get != null
    }
    val resultBuffer = new ArrayBuffer[Coincidence]()
    while(fillRef) {
      val item1 = item1Ref.get
      val item2 = item2Ref.get
      if (item1._1 > item2._1) item2Ref set null
      else if (item1._1 < item2._1) item1Ref set null
      else if (item1._2 > item2._2) item2Ref set null
      else if (item1._2 < item2._2) item1Ref set null
      else resultBuffer += new Coincidence(item1._3, item2._3, randomNumbersAlice(item1._2), randomNumbersBob(item1._2), item1._1, item1._2)
    }
    val coincidences = resultBuffer.toList
    val basisMatchedCoincidences = coincidences.filter(_.basisMatched)
    val validCoincidences = basisMatchedCoincidences.filter(_.valid)

    val map = mutable.HashMap[String, Any]("Delay" -> delay, "Period" -> period, "Gate" -> gate, "PulseDiff" -> pulseDiff)
    map.put("Count 1", item1s.size)
    map.put("Valid Count 1", validItem1s.size)
    map.put("Count 2", item2s.size)
    map.put("Valid Count 2", validItem2s.size)
    map.put("Coincidence Count", coincidences.size)
    map.put("Basis Matched Coincidence Count", basisMatchedCoincidences.size)
    map.put("Valid Coincidence Count", validCoincidences.size)
    val coincidenceSignalSignalTime = validCoincidences.filter(c => c.randomNumberAlice.isSignal && c.randomNumberBob.isSignal && c.randomNumberAlice.isTime)
    val coincidenceSignalDecoyTime = validCoincidences.filter(c => c.randomNumberAlice.isSignal && c.randomNumberBob.isDecoy && c.randomNumberAlice.isTime)
    val coincidenceSignalVacuumTime = validCoincidences.filter(c => c.randomNumberAlice.isSignal && c.randomNumberBob.isVacuum && c.randomNumberAlice.isTime)
    val coincidenceDecoySignalTime = validCoincidences.filter(c => c.randomNumberAlice.isDecoy && c.randomNumberBob.isSignal && c.randomNumberAlice.isTime)
    val coincidenceDecoyDecoyTime = validCoincidences.filter(c => c.randomNumberAlice.isDecoy && c.randomNumberBob.isDecoy && c.randomNumberAlice.isTime)
    val coincidenceDecoyVacuumTime = validCoincidences.filter(c => c.randomNumberAlice.isDecoy && c.randomNumberBob.isVacuum && c.randomNumberAlice.isTime)
    val coincidenceVacuumSignalTime = validCoincidences.filter(c => c.randomNumberAlice.isVacuum && c.randomNumberBob.isSignal && c.randomNumberAlice.isTime)
    val coincidenceVacuumDecoyTime = validCoincidences.filter(c => c.randomNumberAlice.isVacuum && c.randomNumberBob.isDecoy && c.randomNumberAlice.isTime)
    val coincidenceVacuumVacuumTime = validCoincidences.filter(c => c.randomNumberAlice.isVacuum && c.randomNumberBob.isVacuum && c.randomNumberAlice.isTime)
    val coincidenceSignalSignalPhase = validCoincidences.filter(c => c.randomNumberAlice.isSignal && c.randomNumberBob.isSignal && c.randomNumberAlice.isPhase)
    val coincidenceSignalDecoyPhase = validCoincidences.filter(c => c.randomNumberAlice.isSignal && c.randomNumberBob.isDecoy && c.randomNumberAlice.isPhase)
    val coincidenceSignalVacuumPhase = validCoincidences.filter(c => c.randomNumberAlice.isSignal && c.randomNumberBob.isVacuum && c.randomNumberAlice.isPhase)
    val coincidenceDecoySignalPhase = validCoincidences.filter(c => c.randomNumberAlice.isDecoy && c.randomNumberBob.isSignal && c.randomNumberAlice.isPhase)
    val coincidenceDecoyDecoyPhase = validCoincidences.filter(c => c.randomNumberAlice.isDecoy && c.randomNumberBob.isDecoy && c.randomNumberAlice.isPhase)
    val coincidenceDecoyVacuumPhase = validCoincidences.filter(c => c.randomNumberAlice.isDecoy && c.randomNumberBob.isVacuum && c.randomNumberAlice.isPhase)
    val coincidenceVacuumSignalPhase = validCoincidences.filter(c => c.randomNumberAlice.isVacuum && c.randomNumberBob.isSignal && c.randomNumberAlice.isPhase)
    val coincidenceVacuumDecoyPhase = validCoincidences.filter(c => c.randomNumberAlice.isVacuum && c.randomNumberBob.isDecoy && c.randomNumberAlice.isPhase)
    val coincidenceVacuumVacuumPhase = validCoincidences.filter(c => c.randomNumberAlice.isVacuum && c.randomNumberBob.isVacuum && c.randomNumberAlice.isPhase)

    map.put("Signal-Signal, Time, Correct", coincidenceSignalSignalTime.filter(_.isCorrect).size)
    map.put("Signal-Decoy, Time, Correct", coincidenceSignalDecoyTime.filter(_.isCorrect).size)
    map.put("Signal-Vacuum, Time, Correct", coincidenceSignalVacuumTime.filter(_.isCorrect).size)
    map.put("Decoy-Signal, Time, Correct", coincidenceDecoySignalTime.filter(_.isCorrect).size)
    map.put("Decoy-Decoy, Time, Correct", coincidenceDecoyDecoyTime.filter(_.isCorrect).size)
    map.put("Decoy-Vacuum, Time, Correct", coincidenceDecoyVacuumTime.filter(_.isCorrect).size)
    map.put("Vacuum-Signal, Time, Correct", coincidenceVacuumSignalTime.filter(_.isCorrect).size)
    map.put("Vacuum-Decoy, Time, Correct", coincidenceVacuumDecoyTime.filter(_.isCorrect).size)
    map.put("Vacuum-Vacuum, Time, Correct", coincidenceVacuumVacuumTime.filter(_.isCorrect).size)
    map.put("Signal-Signal, Phase, Correct", coincidenceSignalSignalPhase.filter(_.isCorrect).size)
    map.put("Signal-Decoy, Phase, Correct", coincidenceSignalDecoyPhase.filter(_.isCorrect).size)
    map.put("Signal-Vacuum, Phase, Correct", coincidenceSignalVacuumPhase.filter(_.isCorrect).size)
    map.put("Decoy-Signal, Phase, Correct", coincidenceDecoySignalPhase.filter(_.isCorrect).size)
    map.put("Decoy-Decoy, Phase, Correct", coincidenceDecoyDecoyPhase.filter(_.isCorrect).size)
    map.put("Decoy-Vacuum, Phase, Correct", coincidenceDecoyVacuumPhase.filter(_.isCorrect).size)
    map.put("Vacuum-Signal, Phase, Correct", coincidenceVacuumSignalPhase.filter(_.isCorrect).size)
    map.put("Vacuum-Decoy, Phase, Correct", coincidenceVacuumDecoyPhase.filter(_.isCorrect).size)
    map.put("Vacuum-Vacuum, Phase, Correct", coincidenceVacuumVacuumPhase.filter(_.isCorrect).size)

    map.put("Signal-Signal, Time, Wrong", coincidenceSignalSignalTime.filterNot(_.isCorrect).size)
    map.put("Signal-Decoy, Time, Wrong", coincidenceSignalDecoyTime.filterNot(_.isCorrect).size)
    map.put("Signal-Vacuum, Time, Wrong", coincidenceSignalVacuumTime.filterNot(_.isCorrect).size)
    map.put("Decoy-Signal, Time, Wrong", coincidenceDecoySignalTime.filterNot(_.isCorrect).size)
    map.put("Decoy-Decoy, Time, Wrong", coincidenceDecoyDecoyTime.filterNot(_.isCorrect).size)
    map.put("Decoy-Vacuum, Time, Wrong", coincidenceDecoyVacuumTime.filterNot(_.isCorrect).size)
    map.put("Vacuum-Signal, Time, Wrong", coincidenceVacuumSignalTime.filterNot(_.isCorrect).size)
    map.put("Vacuum-Decoy, Time, Wrong", coincidenceVacuumDecoyTime.filterNot(_.isCorrect).size)
    map.put("Vacuum-Vacuum, Time, Wrong", coincidenceVacuumVacuumTime.filterNot(_.isCorrect).size)
    map.put("Signal-Signal, Phase, Wrong", coincidenceSignalSignalPhase.filterNot(_.isCorrect).size)
    map.put("Signal-Decoy, Phase, Wrong", coincidenceSignalDecoyPhase.filterNot(_.isCorrect).size)
    map.put("Signal-Vacuum, Phase, Wrong", coincidenceSignalVacuumPhase.filterNot(_.isCorrect).size)
    map.put("Decoy-Signal, Phase, Wrong", coincidenceDecoySignalPhase.filterNot(_.isCorrect).size)
    map.put("Decoy-Decoy, Phase, Wrong", coincidenceDecoyDecoyPhase.filterNot(_.isCorrect).size)
    map.put("Decoy-Vacuum, Phase, Wrong", coincidenceDecoyVacuumPhase.filterNot(_.isCorrect).size)
    map.put("Vacuum-Signal, Phase, Wrong", coincidenceVacuumSignalPhase.filterNot(_.isCorrect).size)
    map.put("Vacuum-Decoy, Phase, Wrong", coincidenceVacuumDecoyPhase.filterNot(_.isCorrect).size)
    map.put("Vacuum-Vacuum, Phase, Wrong", coincidenceVacuumVacuumPhase.filterNot(_.isCorrect).size)

    map.toMap
  }

  private def analysisSingleChannel(triggerList: Array[Long], signalList: Array[Long], period:Double, delay:Long, gate:Double, pulseDiff:Double) = {
    val triggerIterator = triggerList.iterator
    val currentTriggerRef = new AtomicLong(if(triggerIterator.hasNext)triggerIterator.next() else 0)
    val nextTriggerRef = new AtomicLong(if(triggerIterator.hasNext)triggerIterator.next() else Long.MaxValue)
    val meta = signalList.map(time => {
      if(time>=nextTriggerRef.get){
        currentTriggerRef set nextTriggerRef.get
        nextTriggerRef.set(if(triggerIterator.hasNext)triggerIterator.next() else Long.MaxValue)
      }
      val pulseIndex = ((time - delay - currentTriggerRef.get) / period).toLong
      val delta = (time - delay - currentTriggerRef.get - period * pulseIndex).toLong
      val p = if(math.abs(delta - delay) < gate/2) 0 else if (math.abs(delta - delay - pulseDiff) < gate/2) 1 else -1
      (currentTriggerRef.get, pulseIndex, p)
    })
    meta
  }
}

object RandomNumber {
  def apply(rn: Int) = new RandomNumber(rn)

  val ALL_RANDOM_NUMBERS = Array(0, 1, 2, 3, 8, 9, 10, 11, 12, 13, 14, 15).map(RandomNumber(_))

  private val random = new Random()

  def generateRandomNumbers(length: Int, signalRatio: Double, decoyRatio: Double) = Range(0, length).map(_ => {
    val amp = random.nextDouble() match {
      case d if d < signalRatio => 0xC
      case d if d < signalRatio + decoyRatio => 0x8
      case _ => 0x0
    }
    val encode = random.nextInt(4)
    new RandomNumber(amp | encode)
  }).toArray
}

class RandomNumber(val RN: Int) {
  def isVacuum = (RN & 0x8) == 0

  def isSignal = (RN & 0x4) > 0

  def isDecoy = (!isSignal) && (!isVacuum)

  def isTime = (RN & 0x2) > 0

  def isPhase = !isTime

  def encode = RN & 0x1

  override def equals(obj: scala.Any): Boolean = obj match {
    case other: RandomNumber => other.RN == RN
    case _ => false
  }
}

class Coincidence(val r1:Int, val r2:Int, val randomNumberAlice: RandomNumber, val randomNumberBob: RandomNumber, val triggerTime: Long, val pulseIndex: Long){
  val basisMatched = (randomNumberAlice.isTime && randomNumberBob.isTime) || (randomNumberAlice.isPhase && randomNumberBob.isPhase)
  val valid = (r1 == 0 && r2 == 1) || (r1 == 1 && r2 == 0)
  val isCorrect = randomNumberAlice.encode != randomNumberBob.encode
}