package com.hydra.soap.mdiqkd.tdcparser

import java.io.File
import java.util.{Timer, TimerTask}
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong, AtomicReference}

import scalafx.application.Platform
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Dimension2D, Point2D}
import scalafx.scene.layout._
import scalafx.stage.Screen
import com.hydra.io.MessageClient
import scalafx.scene.control._
import com.hydra.`type`.NumberTypeConversions._
import com.hydra.core.{MessageGenerator, MessagePack}
import org.mongodb.scala.bson.{BsonArray, BsonDouble, BsonInt32, BsonInt64}
import org.mongodb.scala.{Completed, Document, MongoClient, Observer}
import org.python.google.common.util.concurrent.AtomicDouble

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.chart.{AreaChart, NumberAxis, ValueAxis, XYChart}

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import com.hydra.services.tdc.application.RandomNumber
import scalafx.collections.ObservableBuffer

object TDCParser extends JFXApp {
  val DEBUG = new File(".").getAbsolutePath.contains("GitHub")
  System.setProperty("log4j.configurationFile", "./config/tdcviewer.debug.log4j.xml")

  val executionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor(new ThreadFactory {
    val counter = new AtomicInteger(0)

    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r, s"TDCParserExecutionThread-${counter.getAndIncrement}")
      t.setDaemon(true)
      t.setUncaughtExceptionHandler((t: Thread, e: Throwable) => e.printStackTrace())
      t
    }
  }))

  val riseIntegrateTime = new AtomicInteger(1)

  class TDCParserInvokeHandler {
    def setDelayMeasurementIntegrateTime(t: Int) = {
      if (t <= 0) throw new IllegalArgumentException("t should be larger than 0.")
      riseIntegrateTime set t
    }
  }

  lazy val client = MessageClient.newClient(parameters.named.get("host") match {
    case Some(host) => host
    case None => "172.16.60.199"
  }, parameters.named.get("port") match {
    case Some(port) => port.toInt
    case None => 20102
  }, parameters.named.get("name") match {
    case Some(name) => name
    case None => ""
  }, new TDCParserInvokeHandler)
  val storageInvoker = client.blockingInvoker("StorageService")
  val tdcInvoker = client.blockingInvoker("GroundTDCService")
  val pyMathInvoker = client.blockingInvoker("PyMathService")
  val dumperInvoker = client.asynchronousInvoker("MDIQKD-Dumper")
  val path = "/test/tdc/default.fs"
  val recentSize = new AtomicLong(0)

  val reportPath = "/test/tdc/mdireport.fs"
  storageInvoker.FSFileInitialize("", reportPath)
  //val mongoClient: MongoClient = MongoClient("mongodb://MDIQKD:freespace@192.168.25.27:27019")
  //val database = mongoClient.getDatabase("Freespace_MDI_QKD")
  //val collection = database.getCollection("TDCReport-20190528")

  val visualBounds = Screen.primary.visualBounds
  val frameSize = new Dimension2D(visualBounds.width * 0.9, visualBounds.height * 0.6)
  val framePosition = new Point2D(
    visualBounds.getMinX + (visualBounds.getMaxX - visualBounds.getMinX - frameSize.width) / 2,
    visualBounds.getMinY + (visualBounds.getMaxY - visualBounds.getMinY - frameSize.height) / 2)

  val regionDefination = Map("Pulse1" -> Tuple2(2.0, 4.0), "Pulse2" -> Tuple2(5.1, 7.1), "Vacuum" -> Tuple2(8.0, 10.0))

  val histogramStrategyAllPulses = new HistogramStrategy("All Pulses", RandomNumber.ALL_RANDOM_NUMBERS.map(_.RN), regionDefination)
  val histogramStrategyVacuum = new HistogramStrategy("Vacuum", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isVacuum).map(_.RN), regionDefination)
  val histogramStrategyZ0 = new HistogramStrategy("Z 0", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isZ).filter(_.encode == 0).map(_.RN), regionDefination, true)
  val histogramStrategyZ1 = new HistogramStrategy("Z 1", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isZ).filter(_.encode == 1).map(_.RN), regionDefination)
  val histogramStrategyX = new HistogramStrategy("X", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isX).map(_.RN), regionDefination)
  val histogramStrategyY = new HistogramStrategy("Y", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isY).map(_.RN), regionDefination)
  val histogramStrategyAliceTime = new HistogramStrategy("Alice Delay", Array(-1), regionDefination, true)
  val histogramStrategyBobTime = new HistogramStrategy("Bob Delay", Array(-2), regionDefination, true)

  val histogramStrategies = List(
    histogramStrategyAllPulses,
    histogramStrategyVacuum,
    histogramStrategyZ0,
    histogramStrategyZ1,
    histogramStrategyX,
    histogramStrategyY,
    histogramStrategyAliceTime,
    histogramStrategyBobTime,
  )

  def updateReport(reports: Map[String, Map[String, Double]], qberReport: Map[String, Double], qberSection: List[List[Int]], homSection: List[List[Double]], countSection: List[List[Int]], channelMonitorSyncEvents: List[Long]) = {
    def getV(title: String) = List("Pulse1", "Pulse2", "Vacuum", "RandomNumberCount").map(reports(title)(_))

    val vAllPulses = getV(histogramStrategyAllPulses.title)
    val vVacuums = getV(histogramStrategyVacuum.title)
    val vZ0 = getV(histogramStrategyZ0.title)
    val vZ1 = getV(histogramStrategyZ1.title)
    val vX = getV(histogramStrategyX.title)
    val vY = getV(histogramStrategyY.title)

    val pulseExtinctionRatio = (vAllPulses(0) + vAllPulses(1)) / vAllPulses(2) / 2
    val vacuumsCountRate = (vVacuums(0) + vVacuums(1)) / vVacuums(3)
    val Z0CountRate = (vZ0(0) + vZ0(1)) / vZ0(3)
    val Z1CountRate = (vZ1(0) + vZ1(1)) / vZ1(3)
    val XCountRate = (vX(0) + vX(1)) / vX(3)
    val YCountRate = (vY(0) + vY(1)) / vY(3)
    val ZRatio = Z0CountRate / Z1CountRate
    val Z0ExtinctionRatio = vZ0(0) / vZ0(1)
    val Z1ExtinctionRatio = vZ1(1) / vZ1(0)

    val report = f"Pulse Extinction Ratio: ${10 * math.log10(pulseExtinctionRatio)}%.3f dB" + System.lineSeparator() +
      f"Vacuum Intensity: ${10 * math.log10(vacuumsCountRate / (Z0CountRate))}%.2f dB" + System.lineSeparator() +
      f"X Intensity: ${XCountRate / Z0CountRate}%.3f" + System.lineSeparator() +
      f"Y Intensity: ${YCountRate / Z0CountRate}%.3f" + System.lineSeparator() +
      f"Z 0 / TZ 1: ${ZRatio}%.3f" + System.lineSeparator() +
      System.lineSeparator() +
      f"Z 0 Error Rate: ${1 / Z0ExtinctionRatio * 100}%.3f" + "%" + System.lineSeparator() +
      f"Z 1 Error Rate: ${1 / Z1ExtinctionRatio * 100}%.3f" + "%" + System.lineSeparator() +
      System.lineSeparator() +
      f"Pulse 0 Position of Z 0: ${qberReport("pulse0Position")}%.3f" + System.lineSeparator() +
      f"Pulse 0 Width of Z 0: ${qberReport("pulse0Width")}%.3f" + System.lineSeparator() +
      f"Pulse 0 Rise of Z 0: ${qberReport("pulse0Rise")}%.3f" + System.lineSeparator() +
      f"Pulse 0 Rise of Alice: ${qberReport("aliceRise")}%.3f" + System.lineSeparator() +
      f"Pulse 0 Rise of Bob: ${qberReport("bobRise")}%.3f" + System.lineSeparator() +
      System.lineSeparator() +
      System.lineSeparator() +
      f"-------QBER-------" + System.lineSeparator() +
      f"Channel 1 in Window: ${qberReport("Channel 1 in Window") * 100}%.3f" + "%" + System.lineSeparator() +
      f"Channel 2 in Window: ${qberReport("Channel 2 in Window") * 100}%.3f" + "%" + System.lineSeparator() +
      System.lineSeparator() +
      f"HOM with First Pulses of X Encoding" + System.lineSeparator() +
      f"Count: ${qberReport("HOM Count")}" + System.lineSeparator() +
      f"Side Count: ${qberReport("HOM Side Count")}" + System.lineSeparator() +
      f"Dip: ${qberReport("HOM Dip")}%.3f" + System.lineSeparator() +
      System.lineSeparator() +
      f"HOM with All First Pulses" + System.lineSeparator() +
      f"Count: ${qberReport("HOM Count All")}" + System.lineSeparator() +
      f"Side Count: ${qberReport("HOM Side Count All")}" + System.lineSeparator() +
      f"Dip: ${qberReport("HOM Dip All")}%.3f" + System.lineSeparator() +
      System.lineSeparator() +
      f"Coincidences in Z: ${qberReport("QBER Z Count")}" + System.lineSeparator() +
      f"QBER in Z: ${qberReport("QBER Z") * 100}%.3f" + "%" + System.lineSeparator() +
      f"Coincidences in X: ${qberReport("QBER X Count")}" + System.lineSeparator() +
      f"QBER in X: ${qberReport("QBER X") * 100}%.3f" + "%" + System.lineSeparator() +
      System.lineSeparator()
    reportArea.text = report

    val reportMap = Map[String, Double](
      "Pulse Extinction Ratio" -> 10 * math.log10(pulseExtinctionRatio),
      "Vacuum Intensity" -> 10 * math.log10(vacuumsCountRate / Z0CountRate),
      "X Intensity" -> XCountRate / Z0CountRate,
      "Y Intensity" -> YCountRate / Z0CountRate,
      "Z 0 / Z 1" -> ZRatio,
      "Z 0 Error Rate" -> 1 / Z0ExtinctionRatio * 100,
      "Z 1 Error Rate" -> 1 / Z1ExtinctionRatio * 100,
      "SystemTime" -> System.currentTimeMillis())
    val bytes = MessagePack.pack(reportMap ++ qberReport)

    storageInvoker.FSFileAppendFrame("", reportPath, bytes)

    //    val seq1 = (reportMap ++ qberReport).map(z => (z._1, BsonDouble(z._2))).toSeq
    //    val seq2 = Seq(("QBERSections", BsonArray(qberSection.map(li => BsonArray(li.map(i => BsonInt32(i)))))))
    //    val seq3 = Seq(("HOMSections", BsonArray(homSection.map(ld => BsonArray(ld.map(d => BsonDouble(d)))))))
    //    val seq4 = Seq(("CountSections", BsonArray(countSection.map(ld => BsonArray(ld.map(d => BsonInt32(d)))))))
    //    val seq5 = Seq(("ChannelMonitorSync", BsonArray(channelMonitorSyncEvents.map(event => BsonInt64(event)))))
    //    val reportDoc = Document.fromSeq(seq1 ++ seq2 ++ seq3 ++ seq4 ++ seq5)
    //    collection.insertOne(reportDoc).subscribe(new Observer[Completed] {
    //
    //      override def onNext(result: Completed): Unit = println("Inserted")
    //
    //      override def onError(e: Throwable): Unit = e.printStackTrace()
    //
    //      override def onComplete(): Unit = println("Completed")
    //    })

    val realTimeReport = reportMap ++ qberReport ++ Map(
      "QBERSections" -> qberSection,
      "HOMSections" -> homSection,
      "CountSections" -> countSection,
      "ChannelMonitorSync" -> channelMonitorSyncEvents
    )
    dumperInvoker.dumpQBER(realTimeReport)

    println("done update report")
  }

  val grid = new GridPane()
  val chartTextRegeons = histogramStrategies.map(stra => new ChartTextRegeon(stra))
  val reportArea = new TextArea()

  stage = new PrimaryStage {
    title = "MDI-QKD Data Parser"
    resizable = true
    scene = new Scene {
      stylesheets.add(ClassLoader.getSystemClassLoader.getResource("com/hydra/soap/mdiqkd/tdcparser/TDCParser.css").toExternalForm)
      root = new AnchorPane {
        //ReportArea
        reportArea.prefWidth = 250
        AnchorPane.setTopAnchor(reportArea, 0)
        AnchorPane.setBottomAnchor(reportArea, 0)
        AnchorPane.setRightAnchor(reportArea, 0)

        //Grid
        AnchorPane.setTopAnchor(grid, 0)
        AnchorPane.setBottomAnchor(grid, 0)
        AnchorPane.setLeftAnchor(grid, 0)
        AnchorPane.setRightAnchor(grid, reportArea.prefWidth.value)

        //Charts
        chartTextRegeons.zipWithIndex.foreach { z => grid.add(z._1, z._2 % 4, z._2 / 4) }

        children = Seq(grid, reportArea)
        prefWidth = frameSize.width
        prefHeight = frameSize.height
      }
    }
    onCloseRequest = (we) => client.stop
  }

  val integrated = new AtomicBoolean(false)

  def updateResults() = {
    assertThread("TDCParser")
    val size: Long = storageInvoker.metaData("", path, false).asInstanceOf[Map[String, Any]]("Size")
    if (size != recentSize.get) {
      recentSize.set(size)
      val frameBytes = storageInvoker.FSFileReadTailFrames("", path, 0, 1).asInstanceOf[List[Array[Byte]]](0)
      val mg = new MessageGenerator()
      mg.feed(frameBytes)
      val item = mg.next().get.content
      val mdiqkdEncoding = item("MDIQKDEncoding").asInstanceOf[Map[String, Any]]
      val mdiqkdQBER = item("MDIQKDQBER").asInstanceOf[Map[String, Any]]

      //      val channel: Int = mdiqkdEncoding("Channel")
      val delay: Double = mdiqkdEncoding("Delay")
      val period: Double = mdiqkdEncoding("Period")

      val timeAliceHistogrm = mdiqkdEncoding("Histogram Alice Time").asInstanceOf[List[Int]]
      val timeBobHistogrm = mdiqkdEncoding("Histogram Bob Time").asInstanceOf[List[Int]]
      val histograms = mdiqkdEncoding.keys.filter(key => key.startsWith("Histogram With RandomNumber"))
        .map(key => (key.replace("Histogram With RandomNumber[", "").replace("]", "").toInt, mdiqkdEncoding(key).asInstanceOf[List[Int]])).toMap
        .+(-1 -> timeAliceHistogrm).+(-2 -> timeBobHistogrm)

      val rndCounts = mdiqkdEncoding.keys.filter(key => key.startsWith("Count of RandomNumber"))
        .map(key => (key.replace("Count of RandomNumber[", "").replace("]", "").toInt, mdiqkdEncoding(key).asInstanceOf[Int])).toMap

      val reports = chartTextRegeons.map(_.updateHistogram(delay, period, integrated.get, histograms, rndCounts)).toMapHOM Side Count
      val pulse0Position = chartTextRegeons(2).fitCenter.get
      val pulse0Width = chartTextRegeons(2).fitWidth.get
      val pulse0Rise = chartTextRegeons(2).fitRise.get
      val aliceRise = chartTextRegeons(6).fitRise.get
      val bobRise = chartTextRegeons(7).fitRise.get

      val qberReport = new mutable.HashMap[String, Double]()
      qberReport.put("pulse0Position", pulse0Position)
      qberReport.put("pulse0Width", pulse0Width)
      qberReport.put("pulse0Rise", pulse0Rise)
      qberReport.put("aliceRise", aliceRise)
      qberReport.put("bobRise", bobRise)

      val qberCount1: Int = mdiqkdQBER("Count 1")
      val qberValidCount1: Int = mdiqkdQBER("Valid Count 1")
      val qberCount2: Int = mdiqkdQBER("Count 2")
      val qberValidCount2: Int = mdiqkdQBER("Valid Count 2")
      val channel1InWindow = qberValidCount1.toDouble / qberCount1
      val channel2InWindow = qberValidCount2.toDouble / qberCount2
      qberReport.put("Channel 1 in Window", channel1InWindow)
      qberReport.put("Channel 2 in Window", channel2InWindow)

      val homResults = mdiqkdQBER("X-X, 0&0 with delays").asInstanceOf[List[Double]]
      qberReport.put("HOM Count", homResults(0))
      qberReport.put("HOM Side Count", homResults(1))
      qberReport.put("HOM Dip", if (homResults(1) == 0) Double.NaN else homResults(0).toDouble / homResults(1))

      val homResultsAll = mdiqkdQBER("All, 0&0 with delays").asInstanceOf[List[Double]]
      qberReport.put("HOM Count All", homResultsAll(0))
      qberReport.put("HOM Side Count All", homResultsAll(1))
      qberReport.put("HOM Dip All", if (homResultsAll(1) == 0) Double.NaN else homResultsAll(0).toDouble / homResultsAll(1))

      val ssTimeCorrect: Int = mdiqkdQBER("Z-Z, Correct")
      val ssPhaseCorrect: Int = mdiqkdQBER("X-X, Correct")
      val ssTimeWrong: Int = mdiqkdQBER("Z-Z, Wrong")
      val ssPhaseWrong: Int = mdiqkdQBER("X-X, Wrong")
      qberReport.put("QBER Z Count", ssTimeCorrect + ssTimeWrong)
      qberReport.put("QBER Z", if (ssTimeCorrect + ssTimeWrong == 0) Double.NaN else ssTimeWrong.toDouble / (ssTimeCorrect + ssTimeWrong))
      qberReport.put("QBER X Count", ssPhaseCorrect + ssPhaseWrong)
      qberReport.put("QBER X", if (ssPhaseCorrect + ssPhaseWrong == 0) Double.NaN else ssPhaseWrong.toDouble / (ssPhaseCorrect + ssPhaseWrong))

      //      val basisStrings = List("O", "X", "Y", "Z")
      //      val coincidenceMap = new mutable.HashMap[String, List[Long]]()
      //      Range(0, 4).foreach(basisAlice => Range(0, 4).foreach(basisBob => List("Correct", "Wrong").foreach(cw => {
      //        val msg = s"${basisStrings(basisAlice)}-${basisStrings(basisBob)}, ${cw}"
      //        qberReport.put(msg, mdiqkdQBER(msg))
      //        val coincidences = mdiqkdQBER(s"${basisStrings(basisAlice)}-${basisStrings(basisBob)}, Coincidences, ${cw}").asInstanceOf[List[Long]]
      //        coincidenceMap.put(s"${basisStrings(basisAlice)}-${basisStrings(basisBob)}, Coincidences, ${cw}", coincidences)
      //      })))

      val time: Long = mdiqkdQBER("Time")
      qberReport.put("Time", time)

      val qberSections = mdiqkdQBER("QBER Sections").asInstanceOf[List[List[Int]]]
      //QBER Sections Detail: 100*32 Array. 100 for 100 sections. 32 for (Alice[O,X,Y,Z] * 4 + Bob[O,X,Y,Z]) * 2 + (0 for Correct and 1 for Wrong)
      val homSections = mdiqkdQBER("HOM Sections").asInstanceOf[List[List[Double]]]
      //HOM Sections Detail: 4*100 Array. 100 for 100 sections. 4 for: X-X, 0&0 without and with delays; All, 0&0 without and with delays
      val countSections = mdiqkdQBER("Count Sections").asInstanceOf[List[List[Int]]]
      //Count Sections Detail: 100*2 Array. 100 for 100 sections. 2 for signalList1 and signalList2
      //      println(qberSections)
      //      println(homSections)

      updateReport(reports, qberReport.toMap, qberSections, homSections, countSections, mdiqkdQBER("ChannelMonitorSync").asInstanceOf[List[Long]])
      //      evalJython(counts.toArray, recentXData.get, recentHistogram.get, divide)
    }
  }

  def assertThread(start: String) = if (!Thread.currentThread.getName.toLowerCase.startsWith(start.toLowerCase)) {
    val msg = s"Thread Error: current Thread[${Thread.currentThread.getName}] is not start with [${start}]"
    System.err.println(msg)
    throw new RuntimeException(msg)
  }

  assertThread("JavaFX")
  new Timer(true).schedule(new TimerTask {
    override def run() = Future {
      try {
        updateResults()
      } catch {
        case e: RuntimeException if e.getMessage.contains("ChannelFuture failed") => //println(e.getMessage)
        case e: Throwable => e.printStackTrace()
      }
    }(executionContext)
  }, 1000, 200)

  class ChartTextRegeon(strategy: HistogramStrategy) extends VBox {
    val xAxis = NumberAxis("Time (ns)", 0, 10, 1)
    val xAxisLog = NumberAxis("Time (ns)", 0, 10, 1)
    val yAxis = NumberAxis("Count", 0, 100, 10)
    val yAxisLogUnderlying = new LogarithmicAxis(1, 100)
    val yAxisLog = new ValueAxis(yAxisLogUnderlying) {}
    yAxisLog.label = "Count (Log)"
    val regions = new mutable.HashMap[String, List[Tuple2[Double, Double]]]()
    val fitCenter = new AtomicDouble(Double.NaN)
    val fitWidth = new AtomicDouble(Double.NaN)
    val fitRise = new AtomicDouble(Double.NaN)

    val toChartData = (xy: (Double, Double)) => XYChart.Data[Number, Number](xy._1, xy._2)

    val series = new XYChart.Series[Number, Number] {
      name = "Histogram"
      data = Seq((0.0, 0.0)).map(toChartData)
    }
    val lineChart = new AreaChart[Number, Number](xAxis, yAxis, ObservableBuffer(series))
    lineChart.setAnimated(false)
    lineChart.setLegendVisible(false)
    lineChart.setCreateSymbols(false)
    lineChart.title = strategy.title

    val seriesLog = new XYChart.Series[Number, Number] {
      name = "HistogramLog"
      data = Seq((0.0, 0.0)).map(toChartData)
    }
    val lineChartLog = new AreaChart[Number, Number](xAxisLog, yAxisLog, ObservableBuffer(seriesLog))
    lineChartLog.setAnimated(false)
    lineChartLog.setLegendVisible(false)
    lineChartLog.setCreateSymbols(false)
    lineChartLog.visible = false
    lineChartLog.title = strategy.title

    val fitSeries = new XYChart.Series[Number, Number] {
      name = "GaussianFit"
      data = Seq((0.0, 0.0)).map(toChartData)
    }
    val fitSeriesLog = new XYChart.Series[Number, Number] {
      name = "GaussianFitLog"
      data = Seq((0.0, 0.0)).map(toChartData)
    }
    val textField = new TextArea()
    textField.editable = false
    textField.focusTraversable = false
    val logCheck = new CheckBox("log")
    lineChart.visible <== !logCheck.selected
    lineChartLog.visible <== logCheck.selected
    val regionsRef = new AtomicReference[Map[String, List[Tuple2[Double, Double]]]]()

    val stackPane = new StackPane() {
      children = Seq(lineChart, lineChartLog)
    }
    val hBox = new HBox() {
      children = Seq(logCheck, textField)
    }

    //  children = Seq(stackPane, hBox)
    children = Seq(stackPane)

    val recentStartTime = new AtomicDouble(0)
    val recentPeriod = new AtomicDouble(0)
    val recentXData = new AtomicReference[Array[Double]](new Array[Double](0))
    val recentHistogram = new AtomicReference[Array[Double]](new Array[Double](0))
    val historyHistograms = new ListBuffer[Array[Double]]()

    def updateHistogram(startTime: Double, period: Double, integrated: Boolean, histograms: Map[Int, List[Int]], randomNumberCounts: Map[Int, Int]) = {
      val histogramViewed = histograms.filter(entry => strategy.isAcceptedRND(entry._1)).map(his => his._2).reduce((a, b) => a.zip(b).map(z => z._1 + z._2))
      val randomNumberValid = randomNumberCounts.filter(entry => strategy.isAcceptedRND(entry._1)).map(_._2).sum
      if (integrated && recentStartTime.get == startTime && recentPeriod.get == period) {
        recentHistogram set recentHistogram.get.zip(histogramViewed).map(z => z._1 + z._2)
      } else {
        recentHistogram set histogramViewed.toArray.map(i => i.toDouble)
      }

      val binWidth = period / 1000.0 / histogramViewed.size
      recentXData set Range(0, recentHistogram.get.size).map(i => (xAxis.lowerBound.value + binWidth * (i + 0.5))).toArray
      recentStartTime set startTime
      recentPeriod set period
      val xTick = calcTick(startTime / 1000.0, (startTime + period) / 1000.0)
      val regionValues = calculateRegionValues
      //    val report = strategy.result(regionValues, randomNumberValid)
      Platform.runLater(() => {
        xBoundAndTick(xTick._1, xTick._2, xTick._3)
        series.data = recentHistogram.get.zipWithIndex.map(z => (xAxis.lowerBound.value + binWidth * (z._2 + 0.5), z._1.toDouble)).map(toChartData)
        seriesLog.data = recentHistogram.get.zipWithIndex.filter(z => z._1 > 0).map(z => (xAxis.lowerBound.value + binWidth * (z._2 + 0.5), z._1.toDouble)).map(toChartData)
        updateYAxisRange()
      })

      val fit = if (strategy.autoFit) {
        //Fit
        try {
          val it = series.data.get().iterator()
          val xs = ArrayBuffer[Double]()
          val ys = ArrayBuffer[Double]()
          while (it.hasNext) {
            val next = it.next()
            xs += next.getXValue.doubleValue()
            ys += next.getYValue.doubleValue()
          }
          val mathService = TDCParser.client.blockingInvoker("PyMathService")
          val fitResult = mathService.singlePeakGaussianFit(xs.toList, ys.toList).asInstanceOf[List[Double]]
          if (fitResult.forall(r => r != Double.NaN)) {
            Some(fitResult)
          } else {
            None
          }
        } catch {
          case _: Throwable => {
            //          e.printStackTrace()
            None
          }
        }
      } else None
      if (fit.isDefined) {
        fitCenter set (fit.get) (1)
        fitWidth set math.abs((fit.get) (2) * 2.35)
      } else {
        fitCenter set Double.NaN
        fitWidth set Double.NaN
      }

      val riseTime = if (strategy.autoFit) {
        //get rise time
        try {
          val it = series.data.get().iterator()
          val xs = ArrayBuffer[Double]()
          val ys = ArrayBuffer[Double]()
          while (it.hasNext) {
            val next = it.next()
            xs += next.getXValue.doubleValue()
            ys += next.getYValue.doubleValue()
          }

          if (historyHistograms.size > 0 && historyHistograms(0).size != ys.size) historyHistograms.clear()
          historyHistograms += ys.toArray
          if (historyHistograms.size > riseIntegrateTime.get) historyHistograms.remove(0)
          val ysToFit = ys.toArray.map(_ => 0.0)
          historyHistograms.foreach(hiso => Range(0, ysToFit.size).foreach(i => ysToFit(i) += hiso(i)))

          val mathService = TDCParser.client.blockingInvoker("PyMathService")
          val fitResult: Double = mathService.riseTimeFit(xs.toList, ysToFit.toList)
          if (fitResult > 1e5) None else Some(fitResult)
        } catch {
          case e: Throwable => {
            e.printStackTrace()
            None
          }
        }
      } else None
      if (riseTime.isDefined) {
        fitRise set (riseTime.get)
      }

      (strategy.title, regionValues ++ Map("RandomNumberCount" -> randomNumberValid.toDouble))
    }

    def updateReport(report: String) = textField.text = report

    doUpdateRegions(lineChart)
    doUpdateRegions(lineChartLog)

    private def doUpdateRegions(chart: AreaChart[Number, Number]) = {
      val series = chart.data.getValue
      strategy.regions.foreach(e => series.add(new XYChart.Series[Number, Number] {
        name = s"Region-${e._1}"
        val seriesData = ListBuffer[Tuple2[Double, Double]]()
        seriesData += Tuple2(e._2._1 - 0.001, 0.001)
        seriesData += Tuple2(e._2._1, Integer.MAX_VALUE.toDouble)
        seriesData += Tuple2(e._2._2, Integer.MAX_VALUE.toDouble)
        seriesData += Tuple2(e._2._2 + 0.001, 0.001)
        data = seriesData.map(toChartData)
      }.delegate))
    }

    private def calculateRegionValues = {
      val histogramData = recentXData.get.zip(recentHistogram.get)
      val regionValues = strategy.regions.map(e => {
        val range = e._2
        val sum = histogramData.filter(z => z._1 >= range._1 && z._1 <= range._2).map(_._2).sum
        (e._1, sum)
      })
      regionValues
    }

    private def updateYAxisRange() = {
      val yTick = calcTick(recentHistogram.get.min, recentHistogram.get.max, estimate = 8)
      yAxis.lowerBound = yTick._1
      yAxis.upperBound = yTick._2
      yAxis.tickUnit = yTick._3
      yAxisLog.lowerBound = Math.max(yTick._1, 1)
      yAxisLog.upperBound = yTick._2
    }

    private def calcTick(min: Double, max: Double, estimate: Int = 20) = {
      val tickEstimated = (max - min) / estimate
      val tens = math.log10(tickEstimated).toInt
      val tickEstRem = tickEstimated / math.pow(10, tens)
      val tickBase = tickEstRem match {
        case ter if ter < 1.6 => 1
        case ter if ter < 3.6 => 2
        case ter if ter < 8.6 => 5
        case _ => 10
      }
      val tickUnit = tickBase * math.pow(10, tens)
      val viewMin = math.floor(min / tickUnit) * tickUnit
      val viewMax = math.ceil(max / tickUnit) * tickUnit
      val r = (viewMin, viewMax, tickUnit)
      r
    }

    private def xBoundAndTick(lowerBound: Double, upperBound: Double, tickUnit: Double) = {
      xAxis.lowerBound = lowerBound
      xAxisLog.lowerBound = lowerBound
      xAxis.upperBound = upperBound
      xAxisLog.upperBound = upperBound
      xAxis.tickUnit = tickUnit
      xAxisLog.tickUnit = tickUnit
    }
  }

  class HistogramStrategy(val title: String, acceptedRNDs: Array[Int], val regions: Map[String, Tuple2[Double, Double]], val autoFit: Boolean = false) {
    def isAcceptedRND(rnd: Int) = acceptedRNDs.contains(rnd)
  }

  val autoStopDelay = parameters.raw.headOption match {
    case Some(head) => head.toLong * 1000
    case None => 1000l * 3600 * 24 * 365
  }

   new Timer(true).schedule(new TimerTask {
     override def run(): Unit = {
       println("done!")
       System.exit(0)
     }
   }, autoStopDelay)
}