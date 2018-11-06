package com.hydra.soap.mdiqkd.tdcparser

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.text.DecimalFormat
import java.util.{Properties, Timer, TimerTask}
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong, AtomicReference}

import scalafx.application.Platform
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Dimension2D, Point2D, Pos}
import scalafx.scene.layout._
import scalafx.stage.Screen
import com.hydra.io.MessageClient

import scalafx.scene.control._
import com.hydra.`type`.NumberTypeConversions._
import com.hydra.core.MessageGenerator
import com.hydra.soap.mdiqkd.tdcparser.LogarithmicAxis
import org.python.core.PyException
import org.python.google.common.util.concurrent.AtomicDouble
import org.python.util.PythonInterpreter

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scalafx.application.JFXApp
import scalafx.beans.property.{BooleanProperty, DoubleProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.chart.{AreaChart, NumberAxis, ValueAxis, XYChart}
import collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scalafx.scene.chart.XYChart.Series
import scalafx.scene.paint.Color
import com.hydra.services.tdc.application.RandomNumber

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
    case None => "10.1.1.11"
  }, parameters.named.get("port") match {
    case Some(port) => port.toInt
    case None => 20102
  })
  val storageInvoker = client.blockingInvoker("StorageService")
  val tdcInvoker = client.blockingInvoker("GroundTDCService")
  val pyMathInvoker = client.blockingInvoker("PyMathService")
  val path = "/test/tdc/default.fs"
  val recentSize = new AtomicInteger(0)

  val visualBounds = Screen.primary.visualBounds
  val frameSize = new Dimension2D(visualBounds.width * 0.9, visualBounds.height * 0.6)
  val framePosition = new Point2D(
    visualBounds.getMinX + (visualBounds.getMaxX - visualBounds.getMinX - frameSize.width) / 2,
    visualBounds.getMinY + (visualBounds.getMaxY - visualBounds.getMinY - frameSize.height) / 2)

  //  val regionsRef = new AtomicReference[Map[String, List[Tuple2[Double, Double]]]]()
  //  val regionColorMap = new mutable.HashMap[String, Color]()
  //
  //  val fitResult = new Label("")
  //  fitResult.visible = false
  //
  //  val simpleCalcResult = new TextArea()
  //  simpleCalcResult.editable = false
  //  val simpleCalcResultTitle = new Label()
  //

  val regionDefination = Map("Pulse1" -> (2.0, 4.0), "Pulse2" -> (5.0, 7.0), "Vacuum" -> (8.0, 10.0))

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

  def updateReport(reports: Map[String, Map[String, Double]]) = {
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
    val time0Ratio = vTime0(0) / vTime0(1)
    val time1Ratio = vTime1(1) / vTime1(0)

    val report = f"Pulse Extinction Ratio: ${10 * math.log10(pulseExtinctionRatio)}%.3f dB" + System.lineSeparator() +
      f"Vacuum Intensity: ${10 * math.log10(vacuumsCount / (timeSignalsCount + phaseSignalsCount))}%.2f dB" + System.lineSeparator() +
      f"Decoy Intensity (Time): ${timeDecoysCount / timeSignalsCount}%.3f" + System.lineSeparator() +
      f"Decoy Intensity (Phase): ${phaseDecoysCount / phaseSignalsCount}%.3f" + System.lineSeparator() +
      f"Time / Phase (Signal): ${timeSignalsCount / phaseSignalsCount}%.3f" + System.lineSeparator() +
      f"Time / Phase (Decoy): ${timeDecoysCount / phaseDecoysCount}%.3f" + System.lineSeparator() +
      System.lineSeparator() +
      f"Time 0 Error Rate: ${1 / time0Ratio * 100}%.3f" + "%" + System.lineSeparator() +
      f"Time 1 Error Rate: ${1 / time1Ratio * 100}%.3f" + "%" + System.lineSeparator() +
      ""
    reportArea.text = report
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
        //        //LineChart
        //        AnchorPane.setTopAnchor(lineChart, 0.0)
        //        AnchorPane.setBottomAnchor(lineChart, 0.0)
        //        AnchorPane.setLeftAnchor(lineChart, 0.0)
        //        AnchorPane.setRightAnchor(lineChart, 180.0)
        //        AnchorPane.setTopAnchor(lineChartLog, 0.0)
        //        AnchorPane.setBottomAnchor(lineChartLog, 0.0)
        //        AnchorPane.setLeftAnchor(lineChartLog, 0.0)
        //        AnchorPane.setRightAnchor(lineChartLog, 180.0)
        //
        //        //Fit Result
        //        AnchorPane.setTopAnchor(fitResult, 10)
        //        AnchorPane.setRightAnchor(fitResult, 200)
        //        AnchorPane.setLeftAnchor(fitResult, 10)
        //        fitResult.alignment = Pos.CenterRight
        //
        //        //ChartPane
        //        chartPane.children = List(lineChart, lineChartLog, fitResult)
        //        AnchorPane.setTopAnchor(chartPane, 0.0)
        //        AnchorPane.setLeftAnchor(chartPane, 240.0)
        //        AnchorPane.setRightAnchor(chartPane, 0.0)
        //        AnchorPane.setBottomAnchor(chartPane, 0.0)
        //        AnchorPane.setTopAnchor(chartPane, 0.0)
        //        AnchorPane.setLeftAnchor(chartPane, 240.0)
        //        AnchorPane.setRightAnchor(chartPane, 0.0)
        //        AnchorPane.setBottomAnchor(chartPane, 0.0)
        //
        //        //ConfigurationPane
        //        configurationPane.children = configurationFields ::: List(simpleCalcResult, simpleCalcResultTitle)
        //        AnchorPane.setTopAnchor(configurationPane, 0.0)
        //        AnchorPane.setRightAnchor(configurationPane, 0.0)
        //        AnchorPane.setBottomAnchor(configurationPane, 0.0)
        //        var top = 0.0
        //        configurationFields.foreach(cf => {
        //          AnchorPane.setTopAnchor(cf, top)
        //          AnchorPane.setLeftAnchor(cf, 0)
        //          AnchorPane.setRightAnchor(cf, 0)
        //          top += cf.prefHeight.value + 5
        //        })
        //        configurationPane.prefWidth = 180
        //        AnchorPane.setTopAnchor(simpleCalcResult, top + 50)
        //        AnchorPane.setLeftAnchor(simpleCalcResult, 0)
        //        AnchorPane.setRightAnchor(simpleCalcResult, 0)
        //        AnchorPane.setBottomAnchor(simpleCalcResult, 0)
        //        AnchorPane.setTopAnchor(simpleCalcResultTitle, top + 30)
        //        AnchorPane.setLeftAnchor(simpleCalcResultTitle, 0)
        //
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

      val channel: Int = mdiqkdEncoding("Channel")
      val startTime: Long = mdiqkdEncoding("StartTime")
      val period: Double = mdiqkdEncoding("Period")

      val histograms = mdiqkdEncoding.keys.filter(key => key.startsWith("Histogram With RandomNumber"))
        .map(key => (key.replace("Histogram With RandomNumber[", "").replace("]", "").toInt, mdiqkdEncoding(key).asInstanceOf[List[Int]])).toMap
      val rndCounts = mdiqkdEncoding.keys.filter(key => key.startsWith("Count of RandomNumber"))
        .map(key => (key.replace("Count of RandomNumber[", "").replace("]", "").toInt, mdiqkdEncoding(key).asInstanceOf[Int])).toMap
      val reports = chartTextRegeons.map(_.updateHistogram(startTime, period, integrated.get, histograms, rndCounts)).toMap
      updateReport(reports)
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
        case e: RuntimeException => println(e.getMessage)
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

  val recentStartTime = new AtomicLong(0)
  val recentPeriod = new AtomicDouble(0)
  val recentXData = new AtomicReference[Array[Double]](new Array[Double](0))
  val recentHistogram = new AtomicReference[Array[Double]](new Array[Double](0))

  def updateHistogram(startTime: Long, period: Double, integrated: Boolean, histograms: Map[Int, List[Int]], randomNumberCounts: Map[Int, Int]) = {
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

  //  def result(regionValues: Map[String, Double], validRandomNumberCount: Int) = {
  //    val regionAverages = regionValues.map(e => (e._1, e._2 / (regions(e._1)._2 - regions(e._1)._1)))
  //    report(regionAverages, validRandomNumberCount)
  //  }
}

//object JythonBridge {
//  var title = ""
//  var counts = new Array[Int](0)
//  var histogramX = new Array[Double](0)
//  var histogramY = new Array[Double](0)
//  var histogramDivide = 0
//  var display = ""
//  var regions = new mutable.HashMap[String, List[Tuple2[Double, Double]]]()
//  val regionColorMap = new mutable.HashMap[String, Color]()
//
//  def title(t: String): Unit = {
//    title = t
//  }
//
//  def display(t: String): Unit = {
//    display = t
//  }
//
//  def region(name: String, start: Double, stop: Double) = {
//    val list = regions.getOrElseUpdate(name, List[Tuple2[Double, Double]]())
//    regions(name) = list ::: List((start, stop))
//  }
//
//  def region(name: String, color: Color) = {
//    regionColorMap.put(name, color)
//  }
//
//  def region(name: String) = regions.get(name) match {
//    case None => 0.0
//    case Some(reg) => histogramX.zip(histogramY).filter(z => !reg.map(range => z._1 < range._1 || z._1 > range._2).forall(b => b)).map(_._2).sum
//  }
//
//  Thread.setDefaultUncaughtExceptionHandler((t, e) => e.printStackTrace())
//}
