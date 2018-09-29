package com.hydra.services.tdc

import java.util.concurrent.atomic.AtomicReference

import com.hydra.core.MessagePack
import com.hydra.io.{BlockingRemoteObject, MessageClient}
import com.hydra.services.tdc.device.adapters.GroundTDCDataAdapter

import scala.collection.mutable
import scala.io.Source

class GroundTDCProcessService(port: Int) {
  private val channelCount = 16
  private val groundTDA = new GroundTDCDataAdapter(channelCount)
  private val dataTDA = new LongBufferToDataBlockListTDCDataAdapter(channelCount)
  private val server = new TDCProcessServer(channelCount, port, dataIncome, List(groundTDA, dataTDA))
  private val analysers = mutable.HashMap[String, DataAnalyser]()
  private val pathRef = new AtomicReference[String]("/test/tdc/default.fs")
  private val storageRef = new AtomicReference[BlockingRemoteObject](null)
  analysers("Counter") = new CounterAnalyser(channelCount)
  analysers("Histogram") = new HistogramAnalyser(channelCount)

  def stop() = server.stop

  private def dataIncome(data: Any) = {
    if (!data.isInstanceOf[List[_]]) throw new IllegalArgumentException(s"Wrong type: ${data.getClass}")
    data.asInstanceOf[List[DataBlock]].foreach(dataBlockIncome)
  }

  private def dataBlockIncome(dataBlock: DataBlock) = {
    val result = new mutable.HashMap[String, Any]()
    analysers.map(e => (e._1, e._2.dataIncome(dataBlock))).filter(e => e._2.isDefined).foreach(e => result(e._1) = e._2.get)
    result("Time") = System.currentTimeMillis()
    val bytes = MessagePack.pack(result)
    storageRef.get.FSFileAppendFrame("", pathRef.get, bytes)
  }

  def postInit(client: MessageClient) = {
    storageRef set client.blockingInvoker("StorageService")
    storageRef.get.FSFileInitialize("", pathRef.get)
  }

  def turnOnAnalyser(name: String, paras: Map[String, String] = Map()) = analysers.get(name) match {
    case Some(analyser) => analyser.turnOn(paras)
    case None => throw new IllegalArgumentException(s"Analyser ${name} not exists.")
  }

  def configureAnalyser(name: String, paras: Map[String, String]) = analysers.get(name) match {
    case Some(analyser) => analyser.configure(paras)
    case None => throw new IllegalArgumentException(s"Analyser ${name} not exists.")
  }

  def getAnalyserConfiguration(name: String) = analysers.get(name) match {
    case Some(analyser) => analyser.getConfiguration()
    case None => throw new IllegalArgumentException(s"Analyser ${name} not exists.")
  }

  def turnOffAnalyser(name: String) = analysers.get(name) match {
    case Some(analyser) => analyser.turnOff()
    case None => throw new IllegalArgumentException(s"Analyser ${name} not exists.")
  }

  def turnOffAllAnalysers() = analysers.values.foreach(analyser => analyser.turnOff())

  def setDelays(delays: List[Long]) = dataTDA.setDelays(delays)

  def setDelay(channel: Int, delay: Long) = dataTDA.setDelay(channel, delay)

  def getDelays() = dataTDA.getDelays()
}

object TDCProcess extends App {
  val port = 20156
  val process = new GroundTDCProcessService(port)
  val client = MessageClient.newClient("localhost", 20102, "GroundTDCServer", process)
  process.postInit(client)

  process.turnOnAnalyser("Counter")
  process.turnOnAnalyser("Histogram", Map("Sync" -> "0", "Signal" -> "1", "ViewStart" -> "-100000", "ViewStop" -> "100000"))

  println("Ground TDC Process started on port 20156.")
  Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
  println("Stoping Ground TDC Process...")
  client.stop
  process.stop
}


//  //  private val SAMPLING_MODE_SAMPLING = "Sampling"
//  //  private val SAMPLING_MODE_VISIBILITY = "Visibility"
//  //  private val HISTOGRAM_MODE_BIN = "Bin"
//  //  private val HISTOGRAM_MODE_PULSE = "Pulse"
//  //  private val preferences = Preferences.userNodeForPackage(classOf[AppFrame])
//  //  private var samplingChartPanel = null
//  //  private var counterFields = null
//  //  private var delayFields = null
//  //  private var inputBoxs = null
//  //  private var indexTrigger = 1
//  //  private var indexSignal = 2
//  //  private var viewFrom = -100
//  //  private var viewTo = 100
//  //  private var pulsePeriod = 13
//  //  private var gateWidth = 4
//  //  private var matrixFile = "."
//  //  private var matrix = null
//  //  private var doHistogram = true
//  //  private var doSampling = false
//  //  private var histogramMode = HISTOGRAM_MODE_BIN
//  //  private var samplingMode = SAMPLING_MODE_SAMPLING
//  //  private var samplingIntegrate = false
//  //  private var triggerMode = false
//  //  private val coincidences = new Array[Int](1 << 20)
//  //  private var photonCounts = 0
//  //  private var photonCountsTime = 0
//  //  private val visibilities = new Array[Array[Double]](256, 4)
//  //  private var permenents = null
//  //  private var modes = null
//  //  private var delayedPulse = 1
//  //  private val infiniteCounters = new Array[Long](16)
//  //  private var unitEndTime = 0
//  //  private val delays = new Array[Double](16)
//
//  private var timeEvents = new Array[util.ArrayList[_]](16)
//  var i = 0
//  while (i < timeEvents.length) {
//    timeEvents(i) = new util.ArrayList[Long] {
//      i += 1;
//      i - 1
//    }
//  }


//  private var histogramChart = new Chart(100, 400)
//  histogramChart.addSeries("Histogram", Array[Int](0), Array[Int](0))
//  histogramChart.getStyleManager.setLegendVisible(false)
//  histogramChart.getStyleManager.setMarkerSize(0)
//  private var histogramChartPanel = new XChartPanel(histogramChart)
//  private var samplingChart = new Chart(640, 360)
//  samplingChart.getStyleManager.setChartType(StyleManager.ChartType.Bar)
//  samplingChart.addSeries("SamplingExpect", Array[Int](0), Array[Int](0))
//  samplingChart.addSeries("SamplingExperiment", Array[Int](0), Array[Int](0))
//  samplingChart.getStyleManager.setLegendVisible(false)
//  samplingChart.getStyleManager.setMarkerSize(0)
//  private var samplingChartPanel = new XChartPanel(samplingChart)
//  counterFields = Array[JTextField](jTextFieldCount0, jTextFieldCount1, jTextFieldCount2, jTextFieldCount3, jTextFieldCount4, jTextFieldCount5, jTextFieldCount6, jTextFieldCount7, jTextFieldCount8, jTextFieldCount9, jTextFieldCount10, jTextFieldCount11, jTextFieldCount12, jTextFieldCount13, jTextFieldCount14, jTextFieldCount15)
//  delayFields = Array[JTextField](jTextFieldDelay1, jTextFieldDelay2, jTextFieldDelay3, jTextFieldDelay4, jTextFieldDelay5, jTextFieldDelay6, jTextFieldDelay7, jTextFieldDelay8, jTextFieldDelay9, jTextFieldDelay10, jTextFieldDelay11, jTextFieldDelay12, jTextFieldDelay13, jTextFieldDelay14, jTextFieldDelay15, jTextFieldDelay16)
//  inputBoxs = Array[JCheckBox](jCheckBoxInputs1, jCheckBoxInputs2, jCheckBoxInputs3, jCheckBoxInputs4, jCheckBoxInputs5, jCheckBoxInputs6, jCheckBoxInputs7, jCheckBoxInputs8, jCheckBoxInputs9, jCheckBoxInputs10, jCheckBoxInputs11, jCheckBoxInputs12, jCheckBoxInputs13, jCheckBoxInputs14, jCheckBoxInputs15)
//  for (inputBox <- inputBoxs) {
//    inputBox.setVisible(false)
//  }
//  loadPreferences()
//  connectPreferences()
//  postInit()
//