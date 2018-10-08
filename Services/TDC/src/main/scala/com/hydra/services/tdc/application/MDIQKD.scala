package com.hydra.services.tdc.application

import com.hydra.services.tdc.{DataAnalyser, DataBlock, Histogram}
import com.hydra.`type`.NumberTypeConversions._

import scala.collection.mutable

class MDIQKDEncodingAnalyser(channelCount: Int) extends DataAnalyser {
  configuration("RandomNumbers") = List(1)
  configuration("Period") = 10000.0
  configuration("StartTime") = 0
  configuration("Channel") = 2
  configuration("TimeDifference") = 3000
  configuration("BinCount") = 100

  override def configure(key: String, value: Any) = key match {
    case "RandomNumbers" => value.asInstanceOf[List[Int]] != null
    case "Period" => {
      val sc: Double = value
      sc > 0
    }
    case "StartTime" => {
      val sc: Long = value
      true
    }
    case "Channel" => {
      val sc: Int = value
      sc >= 0 && sc < channelCount
    }
    case "TimeDifference" => {
      val sc: Double = value
      true
    }
    case "BinCount" => {
      val sc: Int = value
      sc > 0 && sc < 2000
    }
  }

  override protected def analysis(dataBlock: DataBlock) = {
    val randomNumbers = configuration("RandomNumbers").asInstanceOf[List[Int]].map(r => new RandomNumber(r))
    val period: Double = configuration("Period")
    val startTime: Long = configuration("StartTime")
    val channel: Int = configuration("Channel")
    val timeDifference: Double = configuration("TimeDifference")
    val binCount: Int = configuration("BinCount")

    val dataList = dataBlock.content(channel)
    val meta = dataList.map(time => {
      val pulseIndex = ((time - startTime) / period).toLong
      val randomNumberIndex = (pulseIndex % randomNumbers.size).toInt
      val randomNumber = randomNumbers(if (randomNumberIndex >= 0) randomNumberIndex else randomNumberIndex + randomNumbers.size)
      val delta = (time - startTime - period * pulseIndex).toLong
      (randomNumber, delta)
    })

    val map = mutable.HashMap[String, Any]("Channel" -> channel, "StartTime" -> startTime, "Period" -> period)
    RandomNumber.ALL_RANDOM_NUMBERS.foreach(rn => {
      val validMeta = meta.filter(z => z._1 == rn)
      val histoPulse = new Histogram(validMeta.map(_._2), binCount, 0, period.toLong, 1)
      map.put(s"Histogram With RandomNumber[${rn.RN}]", histoPulse.yData.toList)
    })
    map.toMap
  }
}

object RandomNumber {
  def apply(rn: Int) = new RandomNumber(rn)

  val ALL_RANDOM_NUMBERS = Array(0, 1, 2, 3, 8, 9, 10, 11, 12, 13, 14, 15).map(RandomNumber(_))
}

class RandomNumber(val RN: Int) {
  def isVacuum = (RN & 0x8) == 0

  def isSignal = (RN & 0x4) > 0

  def isTime = (RN & 0x2) > 0

  def encode = RN & 0x1

  override def equals(obj: scala.Any): Boolean = obj match {
    case other: RandomNumber => other.RN == RN
    case _ => false
  }
}
