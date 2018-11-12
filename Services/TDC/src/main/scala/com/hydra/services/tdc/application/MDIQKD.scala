package com.hydra.services.tdc.application

import java.util.concurrent.atomic.AtomicLong

import com.hydra.services.tdc.{DataAnalyser, DataBlock, Histogram}
import com.hydra.`type`.NumberTypeConversions._
import io.netty.handler.codec.http2.Http2HeadersEncoder.Configuration

import scala.collection.mutable
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
      val sc: Long = value
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
    val delay: Long = configuration("Delay")
    val triggerChannel: Int = configuration("TriggerChannel")
    val signalChannel: Int = configuration("SignalChannel")
    val binCount: Int = configuration("BinCount")

    val signalList = dataBlock.content(signalChannel)
    val triggerList = dataBlock.content(triggerChannel)
    val triggerIterator = triggerList.iterator
    val currentTriggerRef = new AtomicLong(if(triggerIterator.hasNext)triggerIterator.next() else 0)
    val nextTriggerRef = new AtomicLong(if(triggerIterator.hasNext)triggerIterator.next() else Long.MaxValue)
//    println("-----------------------------------------------------------------")
    val meta = signalList.map(time => {
      while(time>=nextTriggerRef.get){
        currentTriggerRef set nextTriggerRef.get
        nextTriggerRef.set(if(triggerIterator.hasNext)triggerIterator.next() else Long.MaxValue)
      }
      val pulseIndex = ((time - delay - currentTriggerRef.get) / period).toLong
      val randomNumberIndex = (pulseIndex % randomNumbers.size).toInt
      val randomNumber = randomNumbers(if (randomNumberIndex >= 0) randomNumberIndex else randomNumberIndex + randomNumbers.size)
      val delta = (time - delay - currentTriggerRef.get - period * pulseIndex).toLong
//      println(s"${time} - ${currentTriggerRef.get}, ${pulseIndex}, ${delta}")
      (randomNumber, delta)
    })
//    println(meta.map(_._2).toList)
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
  configuration("PulseWidth") = 2000
  configuration("PulseDiff") = 3000

  override def configure(key: String, value: Any) = key match {
    case "AliceRandomNumbers" => value.asInstanceOf[List[Int]] != null
    case "BobRandomNumbers" => value.asInstanceOf[List[Int]] != null
    case "Period" => {
      val sc: Double = value
      sc > 0
    }
    case "Delay" => {
      val sc: Long = value
      true
    }
    case "PulseWidth" => {
      val sc: Long = value
      true
    }
    case "PulseDiff" => {
      val sc: Long = value
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
    val delay: Long = configuration("Delay")
    val triggerChannel: Int = configuration("TriggerChannel")
    val channel1: Int = configuration("Channel 1")
    val channel2: Int = configuration("Channel 2")
    val pulseWidth = configuration("PulseWidth")
    val pulseDiff = configuration("PulseDiff")

    val triggerList = dataBlock.content(triggerChannel)
    val signalList1 = dataBlock.content(channel1)
    val signalList2 = dataBlock.content(channel2)
    val item1s = analysisSingleChannel(triggerList, signalList1,period, delay)
//    val currentTriggerRef = new AtomicLong(if(triggerIterator.hasNext)triggerIterator.next() else 0)
//    val nextTriggerRef = new AtomicLong(if(triggerIterator.hasNext)triggerIterator.next() else Long.MaxValue)
//    val meta = signalList.map(time => {
//      if(time>=nextTriggerRef.get){
//        currentTriggerRef set nextTriggerRef.get
//        nextTriggerRef.set(if(triggerIterator.hasNext)triggerIterator.next() else Long.MaxValue)
//      }
//      val pulseIndex = ((time - delay - currentTriggerRef.get) / period).toLong
//      val randomNumberIndex = (pulseIndex % randomNumbers.size).toInt
//      val randomNumber = randomNumbers(if (randomNumberIndex >= 0) randomNumberIndex else randomNumberIndex + randomNumbers.size)
//      val delta = (time - delay - period * pulseIndex).toLong
//      (randomNumber, delta)
//    })
//    val map = mutable.HashMap[String, Any]("TriggerChannel" -> triggerChannel,"SignalChannel" -> signalChannel, "Delay" -> delay, "Period" -> period)
//    RandomNumber.ALL_RANDOM_NUMBERS.foreach(rn => {
//      val validMeta = meta.filter(z => z._1 == rn)
//      val histoPulse = new Histogram(validMeta.map(_._2), binCount, 0, period.toLong, 1)
//      map.put(s"Histogram With RandomNumber[${rn.RN}]", histoPulse.yData.toList)
//      map.put(s"Count of RandomNumber[${rn.RN}]", randomNumbers.map(_.RN).count(_ == rn.RN))
//    })
//    map.toMap
  }

  private def analysisSingleChannel(triggerList: Array[Long], signalList: Array[Long], period:Double, delay:Long) = {
    val triggerIterator = triggerList.iterator
    val currentTriggerRef = new AtomicLong(if(triggerIterator.hasNext)triggerIterator.next() else 0)
    val nextTriggerRef = new AtomicLong(if(triggerIterator.hasNext)triggerIterator.next() else Long.MaxValue)
    val meta = signalList.map(time => {
      if(time>=nextTriggerRef.get){
        currentTriggerRef set nextTriggerRef.get
        nextTriggerRef.set(if(triggerIterator.hasNext)triggerIterator.next() else Long.MaxValue)
      }
      val pulseIndex = ((time - delay - currentTriggerRef.get) / period).toLong
      val delta = (time - delay - period * pulseIndex).toLong
      (pulseIndex, delta)
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
