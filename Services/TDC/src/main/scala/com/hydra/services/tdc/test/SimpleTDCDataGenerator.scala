package com.hydra.services.tdc.test

import java.io.{BufferedOutputStream, File, FileOutputStream}
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import com.hydra.services.tdc.application.RandomNumber
import com.hydra.services.tdc.test.TimeEvent._

object SimpleTDCDataGenerator {
  val channelBit = 4
  val startTime = 0l
  val timeUnit = 1000000000l
  val socketRef = new AtomicReference[Socket]()
  val random = new Random(100)

  //  val randomNumbers = Range(0, 10000).toArray.map(_ => 0).map(i => RandomNumber(i))
  //  randomNumbers(2300) = RandomNumber(1)
  val randomNumbers = RandomNumber.generateRandomNumbers(10000, 0.5, 0.3)
  val pulsePeriod = 10000
  val pulseDiff = 3000
  val pulseDelay = 3000

  var DEBUG_TIME_RESTED = 0l
  val DEBUG_SAVE_AS = "delayed1"
  val DEBUG_SAVE_STREAM = if (DEBUG_SAVE_AS == "") None else Some(new BufferedOutputStream(new FileOutputStream(new File(DEBUG_SAVE_AS)), 10000000))

  def launch(port: Int, delay: Long) = Future {
    Thread.sleep(delay)
    val socket = new Socket("localhost", port)
    socketRef.set(socket)
    val out = socket.getOutputStream
    val timeUnitIndex = new AtomicInteger(0)
    val realStartTime = System.currentTimeMillis()
    try {
      while (true) {
        val start = startTime + timeUnit * timeUnitIndex.getAndIncrement()
        val stop = start + timeUnit
        val data = generate(start, stop)
        out.write(data)
        DEBUG_SAVE_STREAM.foreach(_.write(data))
        if (timeUnitIndex.get % 1000 == 0) {
          val realPastTimeMillis = System.currentTimeMillis() - realStartTime
          val pastTimeMillis = (start - startTime) / 1000000000l
          if (realPastTimeMillis < pastTimeMillis) {
            Thread.sleep(pastTimeMillis - realPastTimeMillis)
            DEBUG_TIME_RESTED += (pastTimeMillis - realPastTimeMillis)
          }
          //          val calculatedTimeS = timeUnitIndex.get / 1000
          //          val usedTimeS = (System.currentTimeMillis() - realStartTime - DEBUG_TIME_RESTED).toDouble / 1000
          //          println(s"$calculatedTimeS s of data calculated within $usedTimeS s (${usedTimeS / calculatedTimeS})")
        }
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor((r) => {
    val t = new Thread(r)
    t.setDaemon(true)
    t.setUncaughtExceptionHandler((t: Thread, e: Throwable) => e.printStackTrace())
    t
  })))

  def shutdown() = {
    if (socketRef.get() != null) socketRef.get.close()
    DEBUG_SAVE_STREAM.foreach(o => o.close())
  }

  val modulations = generateModulation(pulseDelay, pulsePeriod, pulseDiff, randomNumbers)

  def generate(startTime: Long, stopTime: Long) = {
    val arrayBuffer = ArrayBuffer[Array[Long]]()
    arrayBuffer += timeEventsPulse(startTime, stopTime, 0, 100000000, pulseShapeGaussian(1000), 1) markeChannel 0 //CH0 10k period
    arrayBuffer += timeEventsRandom(startTime, stopTime, 1, 1) markeChannel 1 //CH1 darkcounts
    arrayBuffer += timeEventsSparsePulse(startTime, stopTime, pulseDelay, pulsePeriod, 100, pulseShapeGaussian(400)) modulate modulations markeChannel 1 //CH1 laserpulse

    val merged = merge(arrayBuffer.toArray)
    val buffer = ByteBuffer.allocate(merged.size * 8)
    merged.foreach(buffer.putLong)
    buffer.array()
  }

  def generateModulation(delay: Long, period: Long, diff: Long, randomNumbers: Array[RandomNumber]) = {
    val modulationBuffer = ListBuffer[Array[Long] => Array[Long]]()
    modulationBuffer += modulationLaser(delay, period, randomNumbers, "")
    //    modulationBuffer += modulationLaser(delay, period, randomNumbers, "FIRST_PULSE")
    //        modulationBuffer += modulationLaser(delay, period, randomNumbers, "RANDOM_NUMBER:1")
    modulationBuffer += modulationDecoy(delay, period, randomNumbers)
    modulationBuffer += modulationInterferometer(delay, period, diff, randomNumbers)
    modulationBuffer += modulationTimeEncoding(delay, period, diff, randomNumbers)

    (timeList: Array[Long]) => modulationBuffer.foldLeft(timeList)((A, B) => B(A))
  }

  def merge(lists: Array[Array[Long]]) = lists.flatten.sorted

  def timeEventsPulse(startTime: Long, stopTime: Long, delay: Long, period: Long, pulseShapeRandom: () => Int, efficiency: Double = 1) = {
    val firstPulseTime = startTime + delay % period - 10 * period
    val count = ((stopTime - firstPulseTime) / period).toInt + 10
    Range(0, count).map(i => firstPulseTime + i * period).filter(_ => random.nextDouble() < efficiency).map(t => t + pulseShapeRandom()).filter(t => t >= startTime && t < stopTime).toArray
  }

  def timeEventsSparsePulse(startTime: Long, stopTime: Long, delay: Long, period: Long, realCount: Int, pulseShapeRandom: () => Int) = {
    val firstPulseTime = startTime + delay % period - 10 * period
    val pulseCount = ((stopTime - firstPulseTime) / period).toInt + 10
    Range(0, realCount).map(i => random.nextInt(pulseCount)).sorted.map(i => firstPulseTime + i * period).map(t => t + pulseShapeRandom()).filter(t => t >= startTime && t < stopTime).toArray
  }

  def timeEventsRandom(startTime: Long, stopTime: Long, count: Int, efficiency: Double = 1) = {
    if (stopTime - startTime > Int.MaxValue) throw new RuntimeException("Unsupported.")
    Range(0, count).map(i => startTime + random.nextInt((stopTime - startTime).toInt)).filter(_ => random.nextDouble() < efficiency).filter(t => t >= startTime && t < stopTime).toArray
  }

  def pulseShapeDelta() = () => 0

  def pulseShapeSqure(width: Int) = () => random.nextInt(width) - width / 2

  def pulseShapeGaussian(sigma: Int) = () => (random.nextGaussian() * sigma).toInt

  def modulationLaser(delay: Long, period: Long, rns: Array[RandomNumber], mode: String) = {
    val laserPulseStatuses = mode match {
      case "" => rns.map(_ => true)
      case "FIRST_PULSE" => (List(true) ::: Range(1, rns.size).map(_ => false).toList).toArray
      case s if s.startsWith("RANDOM_NUMBER:") => {
        val rn = s.substring(14).toInt
        rns.map(_.RN == rn)
      }
      case _ => throw new RuntimeException("Unsupported")
    }
    (timeList: Array[Long]) => timeList.filter(time => laserPulseStatuses(((time / period) % rns.size).toInt))
  }

  def modulationDecoy(delay: Long, period: Long, rns: Array[RandomNumber]) = (timeList: Array[Long]) =>
    timeList.filter(time => {
      val rnd = rns(((time / period) % rns.size).toInt)
      val p = if (rnd.isVacuum) 0.001 else if (rnd.isDecoy) 0.6 else 1
      random.nextDouble() < p
    })

  def modulationInterferometer(delay: Long, period: Long, diff: Long, rns: Array[RandomNumber]) = (timeList: Array[Long]) => timeList.map(time => {
    val delta = if (random.nextInt(2) == 0) 0 else 3000
    time + delta
  })

  def modulationTimeEncoding(delay: Long, period: Long, diff: Long, rns: Array[RandomNumber]) = (timeList: Array[Long]) => timeList.filter(time => {
    val rnd = rns(((time / period) % rns.size).toInt)
    if (rnd.isPhase) true
    else {
      val encoding = rnd.encode == 0
      val timeRem = time % period
      val position = timeRem - delay - diff / 2 < 0 //0 for left
//      println(s"time is ${time},       timeRem is $timeRem,         position is ${timeRem - delay - diff / 2}")
      position == encoding
    }
  })

  //  def poissonCDF(lambda: Double, relativeUpperBound: Double = 3) = {
  //    val upperBound = (relativeUpperBound * lambda).toInt
  //    val eml = math.exp(-lambda)
  //    val Ps = new Array[Double](upperBound + 1)
  //    Ps(0) = eml
  //    Range(1, Ps.size).foreach(i => Ps(i) = Ps(i = 1) * lambda / i)
  //        val CDF = new Array []
  //    () => {}
  //  }
}

object TimeEvent {
  def apply(time: Long, channel: Int): Long = (time << SimpleTDCDataGenerator.channelBit) + channel

  implicit class markeChannelImp(timeList: Array[Long]) {
    def markeChannel(channel: Int) = timeList.map(t => TimeEvent(t, channel))

    def modulate(function: Array[Long] => Array[Long]) = function(timeList)
  }

}