package com.hydra.soap.mdiqkd.tdcparser

import java.io.File
import java.util.{Timer, TimerTask}
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import scalafx.application.Platform
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Dimension2D, Point2D}
import scalafx.scene.layout._
import scalafx.stage.Screen
import com.hydra.io.MessageClient
import scalafx.scene.control._
import com.hydra.`type`.NumberTypeConversions._
import com.hydra.core.{MessageGenerator, MessagePack}
import org.python.google.common.util.concurrent.AtomicDouble
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.chart.{AreaChart, NumberAxis, ValueAxis, XYChart}
//import collection.JavaConverters._
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

  val client = MessageClient.newClient(parameters.named.get("host") match {
    case Some(host) => host
    case None => "192.168.25.27"
  }, parameters.named.get("port") match {
    case Some(port) => port.toInt
    case None => 20102
  })
  val storageInvoker = client.blockingInvoker("StorageService")
  val tdcInvoker = client.blockingInvoker("GroundTDCService")
  val pyMathInvoker = client.blockingInvoker("PyMathService")
  val path = "/test/tdc/default.fs"
  val recentSize = new AtomicInteger(0)

  val reportPath = "/test/tdc/mdireport.fs"
  storageInvoker.FSFileInitialize("", reportPath)

  val visualBounds = Screen.primary.visualBounds
  val frameSize = new Dimension2D(visualBounds.width * 0.9, visualBounds.height * 0.6)
  val framePosition = new Point2D(
    visualBounds.getMinX + (visualBounds.getMaxX - visualBounds.getMinX - frameSize.width) / 2,
    visualBounds.getMinY + (visualBounds.getMaxY - visualBounds.getMinY - frameSize.height) / 2)

  val regionDefination = Map("Pulse1" -> Tuple2(2.0, 4.0), "Pulse2" -> Tuple2(5.0, 7.0), "Vacuum" -> Tuple2(8.0, 10.0))

  val histogramStrategyAllPulses = new HistogramStrategy("All Pulses", RandomNumber.ALL_RANDOM_NUMBERS.map(_.RN), regionDefination)
  val histogramStrategyTimeSignals = new HistogramStrategy("Time Signals", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isSignal).filter(_.isTime).map(_.RN), regionDefination)
  val histogramStrategyPhaseSignals = new HistogramStrategy("Phase Signals", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isSignal).filter(_.isPhase).map(_.RN), regionDefination)
  val histogramStrategyTimeDecoy = new HistogramStrategy("Time Decoy", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isDecoy).filter(_.isTime).map(_.RN), regionDefination)
  val histogramStrategyPhaseDecoy = new HistogramStrategy("Phase Decoy", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isDecoy).filter(_.isPhase).map(_.RN), regionDefination)
  val histogramStrategyVacuum = new HistogramStrategy("Vacuum", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isVacuum).map(_.RN), regionDefination)
  val histogramStrategyTime0 = new HistogramStrategy("Time 0", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isTime).filter(_.encode == 0).map(_.RN), regionDefination)
  val histogramStrategyTime1 = new HistogramStrategy("Time 1", RandomNumber.ALL_RANDOM_NUMBERS.filter(_.isTime).filter(_.encode == 1).map(_.RN), regionDefination)

  val histogramStrategies = List(
    histogramStrategyAllPulses,
    histogramStrategyVacuum,
    histogramStrategyTime0,
    histogramStrategyTime1,
    histogramStrategyTimeSignals,
    histogramStrategyPhaseSignals,
    histogramStrategyTimeDecoy,
    histogramStrategyPhaseDecoy,
  )

  def updateReport(reports: Map[String, Map[String, Double]], qberReport: Map[String, Double]) = {
    def getV(title: String) = List("Pulse1", "Pulse2", "Vacuum", "RandomNumberCount").map(reports(title)(_))

    val vAllPulses = getV(histogramStrategyAllPulses.title)
    val vTimeSignals = getV(histogramStrategyTimeSignals.title)
    val vPhaseSignals = getV(histogramStrategyPhaseSignals.title)
    val vTimeDecoys = getV(histogramStrategyTimeDecoy.title)
    val vPhaseDecoys = getV(histogramStrategyPhaseDecoy.title)
    val vVacuums = getV(histogramStrategyVacuum.title)
    val vTime0 = getV(histogramStrategyTime0.title)
    val vTime1 = getV(histogramStrategyTime1.title)

    val pulseExtinctionRatio = (vAllPulses(0) + vAllPulses(1)) / vAllPulses(2) / 2
    val timeSignalsCount = (vTimeSignals(0) + vTimeSignals(1)) / 2 / vTimeSignals(3)
    val phaseSignalsCount = (vPhaseSignals(0) + vPhaseSignals(1)) / 2 / vPhaseSignals(3)
    val timeDecoysCount = (vTimeDecoys(0) + vTimeDecoys(1)) / 2 / vTimeDecoys(3)
    val phaseDecoysCount = (vPhaseDecoys(0) + vPhaseDecoys(1)) / 2 / vPhaseDecoys(3)
    val vacuumsCount = (vVacuums(0) + vVacuums(1)) / 2 / vVacuums(3)
    val timeRatio = vTime0(0) / vTime1(1)
    val time0Ratio = vTime0(0) / vTime0(1)
    val time1Ratio = vTime1(1) / vTime1(0)

    val report = f"Pulse Extinction Ratio: ${10 * math.log10(pulseExtinctionRatio)}%.3f dB" + System.lineSeparator() +
      f"Vacuum Intensity: ${10 * math.log10(vacuumsCount / (timeSignalsCount + phaseSignalsCount))}%.2f dB" + System.lineSeparator() +
      f"Decoy Intensity (Time): ${timeDecoysCount / timeSignalsCount}%.3f" + System.lineSeparator() +
      f"Decoy Intensity (Phase): ${phaseDecoysCount / phaseSignalsCount}%.3f" + System.lineSeparator() +
      f"Time / Phase (Signal): ${timeSignalsCount / phaseSignalsCount}%.3f" + System.lineSeparator() +
      f"Time / Phase (Decoy): ${timeDecoysCount / phaseDecoysCount}%.3f" + System.lineSeparator() +
      f"Time 0 / Time 1: ${timeRatio}%.3f" + System.lineSeparator() +
      System.lineSeparator() +
      f"Time 0 Error Rate: ${1 / time0Ratio * 100}%.3f" + "%" + System.lineSeparator() +
      f"Time 1 Error Rate: ${1 / time1Ratio * 100}%.3f" + "%" + System.lineSeparator() +
      System.lineSeparator() +
      System.lineSeparator() +
      f"-------QBER-------" + System.lineSeparator() +
      f"Channel 1 in Window: ${qberReport("Channel 1 in Window") * 100}%.3f" + "%" + System.lineSeparator() +
      f"Channel 2 in Window: ${qberReport("Channel 2 in Window") * 100}%.3f" + "%" + System.lineSeparator() +
      System.lineSeparator() +
      f"HOM Count: ${qberReport("HOM Count")}" + System.lineSeparator() +
      f"HOM Dip: ${qberReport("HOM Dip")}%.3f" + System.lineSeparator() +
      System.lineSeparator() +
      f"Coincidences in Time: ${qberReport("QBER Time Count")}" + System.lineSeparator() +
      f"QBER in Time: ${qberReport("QBER Time") * 100}%.3f" +"%" + System.lineSeparator() +
      f"Coincidences in Phase: ${qberReport("QBER Phase Count")}" + System.lineSeparator() +
      f"QBER in Phase: ${qberReport("QBER Phase") * 100}%.3f" +"%" + System.lineSeparator() +
      System.lineSeparator() +
      ""
    reportArea.text = report

    val reportMap = Map[String, Double](
      "Pulse Extinction Ratio" -> 10 * math.log10(pulseExtinctionRatio),
      "Vacuum Intensity" -> 10 * math.log10(vacuumsCount / (timeSignalsCount + phaseSignalsCount)),
      "Decoy Intensity (Time)" -> timeDecoysCount / timeSignalsCount,
      "Decoy Intensity (Phase)" -> phaseDecoysCount / phaseSignalsCount,
      "Time / Phase (Signal)" -> timeSignalsCount / phaseSignalsCount,
      "Time / Phase (Decoy)" -> timeDecoysCount / phaseDecoysCount,
      "Time 0 / Time 1" -> timeRatio,
      "Time 0 Error Rate" -> 1 / time0Ratio * 100,
      "Time 1 Error Rate" -> 1 / time1Ratio * 100,
      "SystemTime" ->System.currentTimeMillis())
    val bytes = MessagePack.pack(reportMap ++ qberReport)
    storageInvoker.FSFileAppendFrame("", reportPath, bytes)
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

      val histograms = mdiqkdEncoding.keys.filter(key => key.startsWith("Histogram With RandomNumber"))
        .map(key => (key.replace("Histogram With RandomNumber[", "").replace("]", "").toInt, mdiqkdEncoding(key).asInstanceOf[List[Int]])).toMap
      val rndCounts = mdiqkdEncoding.keys.filter(key => key.startsWith("Count of RandomNumber"))
        .map(key => (key.replace("Count of RandomNumber[", "").replace("]", "").toInt, mdiqkdEncoding(key).asInstanceOf[Int])).toMap
      val reports = chartTextRegeons.map(_.updateHistogram(delay, period, integrated.get, histograms, rndCounts)).toMap

      val qberReport = new mutable.HashMap[String, Double]()
      val qberCount1 : Int = mdiqkdQBER("Count 1")
      val qberValidCount1 : Int = mdiqkdQBER("Valid Count 1")
      val qberCount2 : Int = mdiqkdQBER("Count 2")
      val qberValidCount2 : Int = mdiqkdQBER("Valid Count 2")
      val channel1InWindow = qberValidCount1.toDouble / qberCount1
      val channel2InWindow = qberValidCount2.toDouble / qberCount2
      qberReport.put("Channel 1 in Window", channel1InWindow)
      qberReport.put("Channel 2 in Window", channel2InWindow)

      val homResults = mdiqkdQBER("Signal-Signal, Phase, 0&0 with delays").asInstanceOf[List[Int]]
      qberReport.put("HOM Count", homResults(0))
      qberReport.put("HOM Dip", if(homResults(1) == 0) Double.NaN else homResults(0).toDouble/homResults(1))

      val ssTimeCorrect : Int = mdiqkdQBER("Signal-Signal, Time, Correct")
      val ssPhaseCorrect : Int = mdiqkdQBER("Signal-Signal, Phase, Correct")
      val ssTimeWrong : Int = mdiqkdQBER("Signal-Signal, Time, Wrong")
      val ssPhaseWrong : Int = mdiqkdQBER("Signal-Signal, Phase, Wrong")
      qberReport.put("QBER Time Count", ssTimeCorrect + ssTimeWrong)
      qberReport.put("QBER Time", if(ssTimeCorrect + ssTimeWrong == 0) Double.NaN else ssTimeWrong.toDouble/(ssTimeCorrect + ssTimeWrong))
      qberReport.put("QBER Phase Count", ssPhaseCorrect + ssPhaseWrong)
      qberReport.put("QBER Phase", if(ssPhaseCorrect + ssPhaseWrong == 0) Double.NaN else ssPhaseWrong.toDouble/(ssPhaseCorrect + ssPhaseWrong))

      println(s"$ssTimeCorrect, $ssTimeWrong,      $ssPhaseCorrect, $ssPhaseWrong")

      updateReport(reports, qberReport.toMap)
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
        case e: RuntimeException if e.getMessage.contains("ChannelFuture failed")=> //println(e.getMessage)
        case e: Throwable => e.printStackTrace()
      }
    }(executionContext)
  }, 1000, 400)
}

class ChartTextRegeon(strategy: HistogramStrategy) extends VBox {
  val xAxis = NumberAxis("Time (ns)", 0, 10, 1)
  val xAxisLog = NumberAxis("Time (ns)", 0, 10, 1)
  val yAxis = NumberAxis("Count", 0, 100, 10)
  val yAxisLogUnderlying = new LogarithmicAxis(1, 100)
  val yAxisLog = new ValueAxis(yAxisLogUnderlying) {}
  yAxisLog.label = "Count (Log)"
  val regions = new mutable.HashMap[String, List[Tuple2[Double, Double]]]()

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

class HistogramStrategy(val title: String, acceptedRNDs: Array[Int], val regions: Map[String, Tuple2[Double, Double]]) {
  def isAcceptedRND(rnd: Int) = acceptedRNDs.contains(rnd)
}
