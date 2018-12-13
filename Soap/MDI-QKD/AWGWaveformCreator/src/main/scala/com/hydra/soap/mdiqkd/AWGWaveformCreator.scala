package com.hydra.soap.mdiqkd

import java.io._

class AWGWaveformCreator {

  abstract class Channel(val name: String) {
    var delay = 0.0
    var enabled = true

    def amplitude(randomNumber: RandomNumber, timeInPulse: Double, pulseIndex: Int): Byte
  }

  private val sampleRate = 25e9
  private var pulseWidth = 2e-9
  private var laserPulseWidth = 2.5e-9
  private var repetationRate = 100e6
  private var firstLaserPulseMode = false
  private var firstModulationPulseMode = false
  private var specifiedRandomNumberMode = false
  private var specifiedRandomNumber = new RandomNumber(0)
  private var period = 1 / repetationRate
  private var ampSignalTime = 255.toByte
  private var ampSignalPhase = 127.toByte
  private var ampDecoyTime = 100.toByte
  private var ampDecoyPhase = 40.toByte
  private var ampPM = 200.toByte
  private var interferometerDiff = 3e-9
  private var syncWidth = 100.0
  private var syncPeriod = 1000.0
  private val channels = List(
    new Channel("Laser") {
      override def amplitude(randomNumber: RandomNumber, timeInPulse: Double, pulseIndex: Int) =
        if (firstLaserPulseMode && pulseIndex > 0) 0
        else if (specifiedRandomNumberMode && randomNumber != specifiedRandomNumber) 0
        else if (((timeInPulse <= laserPulseWidth) && ((!firstLaserPulseMode) || (pulseIndex == 0)))) 1 else 0
    },
    new Channel("Sync") {
      override def amplitude(randomNumber: RandomNumber, timeInPulse: Double, pulseIndex: Int) ={
        val time = pulseIndex * period + timeInPulse
        val timeInSync = time % syncPeriod
        if (timeInSync < syncWidth) 1 else 0
      }
    },
    new Channel("AMDecoy") {
      override def amplitude(randomNumber: RandomNumber, timeInPulse: Double, pulseIndex: Int) =
        if (firstModulationPulseMode && pulseIndex > 0) 0 else {
          if (timeInPulse > pulseWidth) 0
          else if (randomNumber.isVacuum) 0
          else {
            if (randomNumber.isSignal) {
              if (randomNumber.isTime) ampSignalTime else ampSignalPhase
            } else {
              if (randomNumber.isTime) ampDecoyTime else ampDecoyPhase
            }
          }
        }
    },
    new Channel("AMTime1") {
      override def amplitude(randomNumber: RandomNumber, timeInPulse: Double, pulseIndex: Int) = {
        amplitudeAMTime(randomNumber, timeInPulse, pulseIndex)
      }
    },
    new Channel("AMTime2") {
      override def amplitude(randomNumber: RandomNumber, timeInPulse: Double, pulseIndex: Int) = {
        amplitudeAMTime(randomNumber, timeInPulse, pulseIndex)
      }
    },
    new Channel("PM") {
      override def amplitude(randomNumber: RandomNumber, timeInPulse: Double, pulseIndex: Int) =
        if (firstModulationPulseMode && pulseIndex > 0) 0 else {
          val inPulse1 = (timeInPulse >= 0) && (timeInPulse < period / 2)
//          if (inPulse1 && randomNumber.encode == 0 && !randomNumber.isVacuum && !randomNumber.isTime) ampPM else 0
          if (inPulse1 && randomNumber.encode == 0) ampPM else 0
        }
    }
  )

  private def amplitudeAMTime(randomNumber: RandomNumber, timeInPulse: Double, pulseIndex: Int): Byte =
    if (firstModulationPulseMode && pulseIndex > 0) 0 else {
      val inPulse1 = (timeInPulse >= 0) && (timeInPulse < pulseWidth)
      val inPulse2 = (timeInPulse >= interferometerDiff) && (timeInPulse < interferometerDiff + pulseWidth)
      if ((!inPulse1) && (!inPulse2)) 0
      else if (randomNumber.isVacuum) 0
      else if (!randomNumber.isTime) 1
      else if (randomNumber.encode == 0) {
        if (inPulse1) 1 else 0
      } else if (randomNumber.encode == 1) {
        if (inPulse2) 1 else 0
      } else 0
    }

  def setPulseWidth(pw: Double) = pulseWidth = pw

  def setLaserPulseWidth(pw: Double) = laserPulseWidth = pw

  def setRepetationRate(rr: Double) = {
    this.repetationRate = rr
    period = 1 / repetationRate
  }

  def setAmplituteSignalTime(ampST: Double) = ampSignalTime = ((ampST + 1) / 2 * 255.99).toByte

  def setAmplituteSignalPhase(ampSP: Double) = ampSignalPhase = ((ampSP + 1) / 2 * 255.99).toByte

  def setAmplituteDecoyTime(ampDT: Double) = ampDecoyTime = ((ampDT + 1) / 2 * 255.99).toByte

  def setAmplituteDecoyPhase(ampDP: Double) = ampDecoyPhase = ((ampDP + 1) / 2 * 255.99).toByte

  def setAmplitutePM(ampPM: Double) = this.ampPM = ((ampPM + 1) / 2 * 255.99).toByte

  def setFirstModulationPulseMode(fpm: Boolean) = firstModulationPulseMode = fpm

  def setFirstLaserPulseMode(flpm: Boolean) = firstLaserPulseMode = flpm

  def setSpecifiedRandomNumberMode(srnm: Boolean) = specifiedRandomNumberMode = srnm

  def setSpecifiedRandomNumber(srn: Int) = specifiedRandomNumber = new RandomNumber(srn)

  def setInterferometerDiff(diff: Double) = interferometerDiff = diff

  def setSyncWidth(width: Double) = syncWidth = width

  def setSyncPeriod(period: Double) = syncPeriod = period

  def setDelay(name: String, delay: Double) = channels.filter(c => c.name == name).headOption match {
    case Some(channel) => channel.delay = delay
    case None => throw new IllegalArgumentException(s"Channel ${name} not exists.")
  }

  def setEnabled(name: String, enabled: Boolean) = channels.filter(c => c.name == name).headOption match {
    case Some(channel) => channel.enabled = enabled
    case None => throw new IllegalArgumentException(s"Channel ${name} not exists.")
  }

  // Defination of Random Number:
  //# The 1st bit (0, 1) represent for (Vacuum, non Vacuum)
  //# The 2nd bit (0, 1) represent for intensity (Decoy, Signal)
  //# The 3nd bit (0, 1) represent for basis (Time, Phase)
  //# The third element (0, 1) represent for encoding (0, 1)
  class RandomNumber(private val RN: Int) {
    def isVacuum = (RN & 0x8) == 0

    def isSignal = (RN & 0x4) > 0

    def isTime = (RN & 0x2) > 0

    def encode = RN & 0x1

    override def equals(obj: scala.Any): Boolean = if (!obj.isInstanceOf[RandomNumber]) false else obj.asInstanceOf[RandomNumber].RN == RN
  }

  private var waveforms: Map[String, Array[Byte]] = Map()

  def createWaveforms(randomNumbers: Array[Int]) = {
    val rns = randomNumbers.map(i => new RandomNumber(i))
    waveforms = channels.map(c => (c.name -> createWaveform(rns, c.name))).toMap
  }

  def getWaveformSize(name: String) = waveforms.get(name) match {
    case Some(waveform) => waveform.size
    case None => throw new IllegalArgumentException(s"Channel ${name} not exists.")
  }

  def fetchWaveform(name: String, start: Int, stop: Int) = waveforms.get(name) match {
    case Some(waveform) => waveform.slice(start, stop)
    case None => throw new IllegalArgumentException(s"Channel ${name} not exists.")
  }

  def saveToFile(file: File) = {
    val out = new BufferedOutputStream(new FileOutputStream(file), 20000000)
    List("AMDecoy", "Laser", "Sync", "PM", "AMTime1", "AMTime2").foreach(w => waveforms(w).foreach(v => out.write(v)))
    out.close()
  }

  private def createWaveform(randomNumbers: Array[RandomNumber], name: String) = {
    val channel = channels.filter(c => c.name == name).headOption match {
      case Some(channel) => channel
      case None => throw new IllegalArgumentException(s"Channel ${name} not exists.")
    }
    val delaySample = -math.floor(channel.delay * sampleRate + 0.5)
    val totalSample = (randomNumbers.size * period * sampleRate).toInt
    val result = new Array[Byte](totalSample)
    for (i <- 0 until totalSample) {
      val iSample = (i + delaySample + totalSample) % totalSample
      val pulseIndex = (iSample / sampleRate / period).toInt
      val randomNumber = randomNumbers(pulseIndex)
      val timeInPulse = (iSample / sampleRate) - period * pulseIndex
      result(i) = channel.amplitude(randomNumber, timeInPulse, pulseIndex)
    }
    result
  }
}

object AWGWaveformCreator extends App {
  val paras = args.map(arg => arg.split("=")).filter(z => z.size > 1).map(z => z(0) -> z(1)).toMap

  val RNDFile = new File("RNDs")
  val raf = new RandomAccessFile(RNDFile, "r")
  val RNDLength = raf.length().toInt
  val RNDBuffer = new Array[Byte](RNDLength)
  raf.readFully(RNDBuffer)
  val randomNumbers = RNDBuffer.map(b => b.toInt)

  val awgwc = new AWGWaveformCreator

  // set delays
  awgwc.channels.map(_.name).foreach(name => paras.get(s"delay${name}").foreach(dS => awgwc.setDelay(name, dS.toDouble)))
  paras.get("pulseWidth").foreach(w => awgwc.setPulseWidth(w.toDouble))
  paras.get("laserPulseWidth").foreach(w => awgwc.setLaserPulseWidth(w.toDouble))
  paras.get("firstLaserPulseMode").foreach(w => awgwc.setFirstLaserPulseMode(w.toBoolean))
  paras.get("firstModulationPulseMode").foreach(w => awgwc.setFirstModulationPulseMode(w.toBoolean))
  paras.get("specifiedRandomNumberMode").foreach(w => awgwc.setSpecifiedRandomNumberMode(w.toBoolean))
  paras.get("specifiedRandomNumber").foreach(w => awgwc.setSpecifiedRandomNumber(w.toInt))
  paras.get("amplituteSignalTime").foreach(w => awgwc.setAmplituteSignalTime(w.toDouble))
  paras.get("amplituteSignalPhase").foreach(w => awgwc.setAmplituteSignalPhase(w.toDouble))
  paras.get("amplituteDecoyTime").foreach(w => awgwc.setAmplituteDecoyTime(w.toDouble))
  paras.get("amplituteDecoyPhase").foreach(w => awgwc.setAmplituteDecoyPhase(w.toDouble))
  paras.get("amplitutePM").foreach(w => awgwc.setAmplitutePM(w.toDouble))
  paras.get("interferometerDiff").foreach(w => awgwc.setInterferometerDiff(w.toDouble))
  paras.get("syncWidth").foreach(w => awgwc.setSyncWidth(w.toDouble))
  paras.get("syncPeriod").foreach(w => awgwc.setSyncPeriod(w.toDouble))

  val startTime = System.nanoTime()
  awgwc.createWaveforms(randomNumbers)
  awgwc.saveToFile(new File("test.wave"))
  val endTime = System.nanoTime()
  val delta = (endTime - startTime) / 1e9
//  println(delta + " s")

  //    val host = if (args.length > 3) args(0) else "localhost"
  //    val port = if (args.length > 3) args(1).toInt else 20102
  //    val clientName = if (args.length > 3) args(2) else "AWGWaveformCreator"
  //    val client = MessageClient.newClient(host, port, clientName, awgwc)
  //    println(s"AWGWaveformCreator[${clientName}] started.")
  //    Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
  //    println("Stoping...")
  //    client.stop
}
