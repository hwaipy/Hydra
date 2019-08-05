package com.hydra.services.tdc

import java.io._
import java.text.DecimalFormat
import java.util.{Properties, Timer, TimerTask}
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong, AtomicReference}

import scalafx.application.Platform
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Dimension2D, Point2D, Pos}
import scalafx.scene.layout.{AnchorPane, Region}
import scalafx.stage.Screen
import com.hydra.io.MessageClient
import scalafx.scene.control._
import com.hydra.`type`.NumberTypeConversions._
import com.hydra.core.MessageGenerator
import com.hydra.services.tdcviewer.LogarithmicAxis
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

object TDCViweer extends JFXApp {
  val DEBUG = new File(".").getAbsolutePath.contains("GitHub")
  System.setProperty("log4j.configurationFile", "./config/tdcviewer.debug.log4j.xml")

  val executionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor(new ThreadFactory {
    val counter = new AtomicInteger(0)

    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r, s"TDCViewerExecutionThread-${counter.getAndIncrement}")
      t.setDaemon(true)
      t.setUncaughtExceptionHandler((t: Thread, e: Throwable) => e.printStackTrace())
      t
    }
  }))

  class Handler{
    def getPulsePosition() =
//      if (gaussianFitTime.get < System.currentTimeMillis - 3000) Double.NaN
//      else gaussianFitResult.get()("Peak")
          if (gaussianFitTime.get < System.currentTimeMillis - 3000) Double.NaN
          else maxPosition.get()
  }

  val client = MessageClient.newClient(parameters.named.get("host") match {
    case Some(host) => host
    case None => "192.168.25.27"
  }, parameters.named.get("port") match {
    case Some(port) => port.toInt
    case None => 20102
  }, "TDCViewer", new Handler)
  val storageInvoker = client.blockingInvoker("StorageService")
  val tdcInvoker = client.blockingInvoker("GroundTDCService")
  val pyMathInvoker = client.blockingInvoker("PyMathService")
  val path = "/test/tdc/default.fs"
  val recentSize = new AtomicLong(0)

  val visualBounds = Screen.primary.visualBounds
  val frameSize = new Dimension2D(visualBounds.width * 0.9, visualBounds.height * 0.6)
  val framePosition = new Point2D(
    visualBounds.getMinX + (visualBounds.getMaxX - visualBounds.getMinX - frameSize.width) / 2,
    visualBounds.getMinY + (visualBounds.getMaxY - visualBounds.getMinY - frameSize.height) / 2)

  val counterFields = Range(0, 16).toList.map(i => new TextField())
  val delayFields = Range(0, 16).toList.map(i => new TextField())
  val counterLabels = Range(0, 16).toList.map(i => new Label())
  val counterPane = new AnchorPane()

  val xAxis = NumberAxis("Time (ns)", -100, 100, 10)
  val yAxis = NumberAxis("Count", 0, 100, 10)
  val xAxisLog = NumberAxis("Time (ns)", -100, 100, 10)
  val yAxisLogUnderlying = new LogarithmicAxis(1, 100)
  val yAxisLog = new ValueAxis(yAxisLogUnderlying) {}
  yAxisLog.label = "Count (Log)"
  val chartPane = new AnchorPane()
  val autoRangingYAxis = BooleanProperty(true)
  val yValueMin = DoubleProperty(0.0)
  val yValueMax = DoubleProperty(100.0)
  val yAxisManualMin = DoubleProperty(0.0)
  val yAxisManualMax = DoubleProperty(100.0)
  val yAxisManualMinFieldRef = new AtomicReference[TextField]()
  val yAxisManualMaxFieldRef = new AtomicReference[TextField]()
  val autoGaussianFit = BooleanProperty(false)

  val syncChannelFieldRef = new AtomicReference[TextField]()
  val signalChannelFieldRef = new AtomicReference[TextField]()
  val viewFromFieldRef = new AtomicReference[TextField]()
  val viewToFieldRef = new AtomicReference[TextField]()
  val binCountFieldRef = new AtomicReference[TextField]()
  val divideFieldRef = new AtomicReference[TextField]()
  val configurationFields: List[Region] =
    createHistogramChannelSetter("Sync") :: createHistogramChannelSetter("Signal") ::
      createHistogramViewSetter("ViewFrom") :: createHistogramViewSetter("ViewTo") ::
      createHistogramBinCountSetter() :: createHistogramDivideSetter() ::
      createIntegrateCheckSetter() :: createHistogramLogYCheckSetter() ::
      createGaussianFitChecker() :: createYAxisAutoRangeCheckSetter() ::
      createYAxisManualSetter("Min") :: createYAxisManualSetter("Max") ::
      Nil
  val configurationPane = new AnchorPane()
  //TODO bad code

  // Helper function to convert a tuple to `XYChart.Data`
  val toChartData = (xy: (Double, Double)) => XYChart.Data[Number, Number](xy._1, xy._2)

  val series = new XYChart.Series[Number, Number] {
    name = "Histogram"
    data = Seq((0.0, 0.0)).map(toChartData)
  }
  val lineChart = new AreaChart[Number, Number](xAxis, yAxis, ObservableBuffer(series))
  lineChart.setAnimated(false)
  lineChart.setLegendVisible(false)
  lineChart.setCreateSymbols(false)

  val seriesLog = new XYChart.Series[Number, Number] {
    name = "HistogramLog"
    data = Seq((0.0, 0.0)).map(toChartData)
  }
  val lineChartLog = new AreaChart[Number, Number](xAxisLog, yAxisLog, ObservableBuffer(seriesLog))
  lineChartLog.setAnimated(false)
  lineChartLog.setLegendVisible(false)
  lineChartLog.setCreateSymbols(false)
  lineChartLog.visible = false

  val fitSeries = new XYChart.Series[Number, Number] {
    name = "GaussianFit"
    data = Seq((0.0, 0.0)).map(toChartData)
  }
  val fitSeriesLog = new XYChart.Series[Number, Number] {
    name = "GaussianFitLog"
    data = Seq((0.0, 0.0)).map(toChartData)
  }

  val regionsRef = new AtomicReference[Map[String, List[Tuple2[Double, Double]]]]()
  val regionColorMap = new mutable.HashMap[String, Color]()

  val fitResult = new Label("")
  fitResult.visible = true

  val simpleCalcResult = new TextArea()
  simpleCalcResult.editable = false
  val simpleCalcResultTitle = new Label()

  stage = new PrimaryStage {
    title = "TDC Viewer"
    resizable = true
    scene = new Scene {
      stylesheets.add(ClassLoader.getSystemClassLoader.getResource("com/hydra/services/tdcviewer/TDCViewer.css").toExternalForm)
      root = new AnchorPane {
        //CounterLabels
        counterLabels.zipWithIndex.foreach(z => {
          AnchorPane.setTopAnchor(z._1, 40.0 * z._2)
          AnchorPane.setLeftAnchor(z._1, 0.0)
          z._1.prefHeight = 35
          z._1.prefWidth = 50
          z._1.focusTraversable = false
          z._1.text = s"CH ${z._2 + 1}"
        })

        //CounterFields
        counterFields.zipWithIndex.foreach(z => {
          AnchorPane.setTopAnchor(z._1, 40.0 * z._2)
          AnchorPane.setLeftAnchor(z._1, 55.0)
          z._1.prefHeight = 35
          z._1.prefWidth = 100
          z._1.editable = false
          z._1.focusTraversable = false
          z._1.text = "----"
        })

        //DelayFields
        delayFields.zipWithIndex.foreach(z => {
          AnchorPane.setTopAnchor(z._1, 40.0 * z._2)
          AnchorPane.setLeftAnchor(z._1, 170.0)
          z._1.prefHeight = 35
          z._1.prefWidth = 70
          z._1.text = "0.0"
        })

        //CounterPane
        counterPane.children = counterFields ::: counterLabels ::: delayFields
        AnchorPane.setTopAnchor(counterPane, 0.0)
        AnchorPane.setLeftAnchor(counterPane, 0.0)

        //LineChart
        AnchorPane.setTopAnchor(lineChart, 0.0)
        AnchorPane.setBottomAnchor(lineChart, 0.0)
        AnchorPane.setLeftAnchor(lineChart, 0.0)
        AnchorPane.setRightAnchor(lineChart, 180.0)
        AnchorPane.setTopAnchor(lineChartLog, 0.0)
        AnchorPane.setBottomAnchor(lineChartLog, 0.0)
        AnchorPane.setLeftAnchor(lineChartLog, 0.0)
        AnchorPane.setRightAnchor(lineChartLog, 180.0)

        //Fit Result
        AnchorPane.setTopAnchor(fitResult, 10)
        AnchorPane.setRightAnchor(fitResult, 200)
        AnchorPane.setLeftAnchor(fitResult, 10)
        fitResult.alignment = Pos.CenterRight

        //ChartPane
        chartPane.children = List(lineChart, lineChartLog, fitResult)
        AnchorPane.setTopAnchor(chartPane, 0.0)
        AnchorPane.setLeftAnchor(chartPane, 240.0)
        AnchorPane.setRightAnchor(chartPane, 0.0)
        AnchorPane.setBottomAnchor(chartPane, 0.0)
        AnchorPane.setTopAnchor(chartPane, 0.0)
        AnchorPane.setLeftAnchor(chartPane, 240.0)
        AnchorPane.setRightAnchor(chartPane, 0.0)
        AnchorPane.setBottomAnchor(chartPane, 0.0)

        //ConfigurationPane
        configurationPane.children = configurationFields ::: List(simpleCalcResult, simpleCalcResultTitle)
        AnchorPane.setTopAnchor(configurationPane, 0.0)
        AnchorPane.setRightAnchor(configurationPane, 0.0)
        AnchorPane.setBottomAnchor(configurationPane, 0.0)
        var top = 0.0
        configurationFields.foreach(cf => {
          AnchorPane.setTopAnchor(cf, top)
          AnchorPane.setLeftAnchor(cf, 0)
          AnchorPane.setRightAnchor(cf, 0)
          top += cf.prefHeight.value + 5
        })
        configurationPane.prefWidth = 180
        AnchorPane.setTopAnchor(simpleCalcResult, top + 50)
        AnchorPane.setLeftAnchor(simpleCalcResult, 0)
        AnchorPane.setRightAnchor(simpleCalcResult, 0)
        AnchorPane.setBottomAnchor(simpleCalcResult, 0)
        AnchorPane.setTopAnchor(simpleCalcResultTitle, top + 30)
        AnchorPane.setLeftAnchor(simpleCalcResultTitle, 0)

        children = Seq(counterPane, chartPane, configurationPane)
        prefWidth = frameSize.width
        prefHeight = frameSize.height
      }
    }
    onCloseRequest = (we) => client.stop
  }

  delayFields.zipWithIndex.foreach(z => z._1.focused.onChange((a, b, c) => {
    if (!z._1.focused.value) {
      val v = z._1.text.value
      try {
        val dv = v.toDouble
        Future {
          tdcInvoker.setDelay(z._2, (dv * 1000).toLong)
          updateDelays()
        }(executionContext)
      } catch {
        case e: Throwable =>
      }
    }
  }))

  val recentViewStart = new AtomicLong(-1)
  val recentViewStop = new AtomicLong(-1)
  val recentDivide = new AtomicLong(-1)
  val recentXData = new AtomicReference[Array[Double]](new Array[Double](0))
  val recentHistogram = new AtomicReference[Array[Double]](new Array[Double](0))
  val integrated = new AtomicBoolean(false)

  def updateResults() = {
    assertThread("TDCViewer")
    val size: Long = storageInvoker.metaData("", path, false).asInstanceOf[Map[String, Any]]("Size")
    if (size != recentSize.get) {
      recentSize.set(size)
      val frameBytes = storageInvoker.FSFileReadTailFrames("", path, 0, 1).asInstanceOf[List[Array[Byte]]](0)
      val mg = new MessageGenerator()
      mg.feed(frameBytes)
      val item = mg.next().get.content

      //counts
      val counts = item("Counter").asInstanceOf[List[Int]]
      displayCounterResult(counts)

      //histogram
      val histogram = item("Histogram").asInstanceOf[Map[String, Any]]
      //      val syncChannel: Int = histogram("SyncChannel")
      //      val signalChannel: Int = histogram("SignalChannel")
      val viewStart: Long = histogram("ViewFrom")
      val viewStop: Long = histogram("ViewTo")
      val divide: Int = histogram("Divide")
      val histo = histogram("Histogram").asInstanceOf[List[Int]].toArray
      if (integrated.get && recentViewStart.get == viewStart && recentViewStop.get == viewStop && recentHistogram.get.size == histo.size) {
        recentHistogram set recentHistogram.get.zip(histo).map(z => z._1 + z._2)
      } else recentHistogram set histo.map(i => i.toDouble)
      val binWidth = (viewStop - viewStart) / 1000.0 / histo.size / divide
      recentXData set Range(0, recentHistogram.get.size).map(i => (xAxis.lowerBound.value + binWidth * (i + 0.5))).toArray
      recentViewStart set viewStart
      recentViewStop set viewStop
      recentDivide set divide
      val xTick = calcTick(viewStart / 1000.0, (viewStart + (viewStop - viewStart) / divide) / 1000.0)
      Platform.runLater(() => {
        xAxis.lowerBound = xTick._1
        xAxis.upperBound = xTick._2
        xAxis.tickUnit = xTick._3
        xAxisLog.lowerBound = xTick._1
        xAxisLog.upperBound = xTick._2
        xAxisLog.tickUnit = xTick._3
        series.data = recentHistogram.get.zipWithIndex.map(z => (xAxis.lowerBound.value + binWidth * (z._2 + 0.5), z._1.toDouble)).map(toChartData)
        seriesLog.data = recentHistogram.get.zipWithIndex.filter(z => z._1 > 0).map(z => (xAxis.lowerBound.value + binWidth * (z._2 + 0.5), z._1.toDouble)).map(toChartData)
        yValueMin <= recentHistogram.get.min
        yValueMax <= recentHistogram.get.max
        updateYAxisRange()
      })
      evalJython(counts.toArray, recentXData.get, recentHistogram.get, divide)
      updateRegions()
      try{
        updateGaussianFit()
      }catch{
        case e:Throwable => //e.printStackTrace()
      }
    }
  }

  def updateYAxisRange() = {
    assertThread("JavaFX")
    val yTick = if (autoRangingYAxis.value) calcTick(recentHistogram.get.min.toDouble, recentHistogram.get.max.toDouble) else calcTick(yAxisManualMin.value, yAxisManualMax.value)
    yAxis.lowerBound = yTick._1
    yAxis.upperBound = yTick._2
    yAxis.tickUnit = yTick._3
    yAxisLog.lowerBound = Math.max(yTick._1, 1)
    yAxisLog.upperBound = yTick._2
  }

  def updateDelays() = {
    assertThread("TDCViewer")
    val delays = tdcInvoker.getDelays().asInstanceOf[List[Any]].map(i => {
      val d: Long = i
      d
    })
    Platform.runLater(() => delayFields.zip(delays).foreach(z => if (!z._1.focused.value) z._1.text = s"${z._2 / 1000.0}"))
  }

  def updateHistogramConfiguration() = {
    assertThread("TDCViewer")
    try {
      val conf = tdcInvoker.getAnalyserConfiguration("Histogram").asInstanceOf[Map[String, Any]]
      val syncChannel: Int = conf("Sync")
      val signalChannel: Int = conf("Signal")
      val viewStart: Long = conf("ViewStart")
      val viewStop: Long = conf("ViewStop")
      val divide: Int = conf("Divide")
      Platform.runLater(() => {
        if (!syncChannelFieldRef.get.focused.value) syncChannelFieldRef.get.text = (syncChannel + 1).toString
        if (!signalChannelFieldRef.get.focused.value) signalChannelFieldRef.get.text = (signalChannel + 1).toString
        if (!viewFromFieldRef.get.focused.value) viewFromFieldRef.get.text = (viewStart / 1000.0).toString
        if (!viewToFieldRef.get.focused.value) viewToFieldRef.get.text = (viewStop / 1000.0).toString
        if (!divideFieldRef.get.focused.value) divideFieldRef.get.text = divide.toString
      })
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }

  def displayCounterResult(counterResult: List[Int]) = Platform.runLater(() => counterResult.zip(counterFields).foreach(z => z._2.text = s"${z._1}"))

  def createHistogramChannelSetter(title: String) = {
    assertThread("JavaFX")
    val r = createLabelFieldSetter(title, (s) => {
      val dv = s.toInt
      Future {
        tdcInvoker.configureAnalyser("Histogram", Map(title -> (dv - 1)))
        updateHistogramConfiguration()
      }(executionContext)
    }, if (title == "Sync") "1" else "2")
    val fieldRef = if (title == "Sync") syncChannelFieldRef else signalChannelFieldRef
    fieldRef.set(r._3)
    r._1
  }

  def createHistogramViewSetter(title: String) = {
    assertThread("JavaFX")
    val r = createLabelFieldSetter(title, (s) => {
      val dv = (s.toDouble * 1000).toLong
      Future {
        tdcInvoker.configureAnalyser("Histogram", Map((if (title == "ViewFrom") "ViewStart" else "ViewStop") -> (dv)))
        updateHistogramConfiguration()
      }(executionContext)
    }, if (title == "ViewFrom") "-10" else "10")
    val fieldRef = if (title == "ViewFrom") viewFromFieldRef else viewToFieldRef
    fieldRef.set(r._3)
    r._1
  }

  def createHistogramBinCountSetter() = {
    assertThread("JavaFX")
    val r = createLabelFieldSetter("BinCount", (s) => {
      val dv = s.toInt
      Future {
        tdcInvoker.configureAnalyser("Histogram", Map("BinCount" -> (dv)))
        updateHistogramConfiguration()
      }(executionContext)
    }, "1000")
    binCountFieldRef.set(r._3)
    r._1
  }

  def createHistogramDivideSetter() = {
    assertThread("JavaFX")
    val r = createLabelFieldSetter("Divide", (s) => {
      val dv = s.toInt
      Future {
        tdcInvoker.configureAnalyser("Histogram", Map("Divide" -> (dv)))
        updateHistogramConfiguration()
      }(executionContext)
    }, "1")
    divideFieldRef.set(r._3)
    r._1
  }

  def createLabelFieldSetter(title: String, onFocusLost: (String) => Unit, defaultValue: String) = {
    assertThread("JavaFX")
    val label = new Label(title)
    val field = new TextField()
    field.focused.onChange((a, b, c) => if (!field.focused.value) onFocusLost(field.text.value))
    field.text = defaultValue

    val pane = new AnchorPane()
    pane.children = Seq(label, field)
    AnchorPane.setLeftAnchor(label, 0.0)
    AnchorPane.setTopAnchor(label, 0.0)
    AnchorPane.setBottomAnchor(label, 0.0)
    AnchorPane.setRightAnchor(label, 70.0)
    AnchorPane.setLeftAnchor(field, 65.0)
    AnchorPane.setTopAnchor(field, 0.0)
    AnchorPane.setBottomAnchor(field, 0.0)
    AnchorPane.setRightAnchor(field, 0.0)
    pane.prefHeight = 30
    (pane, label, field)
  }

  def createIntegrateCheckSetter() = {
    assertThread("JavaFX")
    createCheckButtonSetter("Integrated", false, (s) => integrated.set(s), Some(("Clear", () => recentHistogram.set(new Array[Double](0)))))
  }

  def createHistogramLogYCheckSetter() = {
    assertThread("JavaFX")
    createCheckButtonSetter("LogY", false, (s) => {
      assertThread("JavaFX")
      lineChart.visible = !s
      lineChartLog.visible = s
    }, None)
  }

  def createYAxisAutoRangeCheckSetter() = {
    assertThread("JavaFX")
    createCheckButtonSetter("AutoRange", true, (s) => {
      assertThread("JavaFX")
      autoRangingYAxis.value = s
      yAxisManualMinFieldRef.get.disable = s
      yAxisManualMaxFieldRef.get.disable = s
      updateYAxisRange()
    }, None)
  }

  def createYAxisManualSetter(title: String) = {
    assertThread("JavaFX")
    val property = if (title == "Min") yAxisManualMin else yAxisManualMax
    val ref = if (title == "Min") yAxisManualMinFieldRef else yAxisManualMaxFieldRef
    val r = createLabelFieldSetter(title, (s) => {
      try {
        property.value = s.toDouble
      } catch {
        case e: Throwable =>
      } finally {
        updateYAxisManualSettersValue()
      }
    }, property.value.toString)
    ref.set(r._3)
    r._3.disable = true
    r._1
  }

  def updateYAxisManualSettersValue() {
    yAxisManualMinFieldRef.get.text = yAxisManualMin.value.toString
    yAxisManualMaxFieldRef.get.text = yAxisManualMax.value.toString
  }

  def createGaussianFitChecker() = {
    assertThread("JavaFX")
    createCheckButtonSetter("Gaussian Fit", false, (s) => {
      autoGaussianFit.value = s
      fitResult.visible = s
      if (s) {
        lineChart.data.getValue.add(fitSeries)
        lineChartLog.data.getValue.add(fitSeriesLog)
      } else {
        lineChart.data.getValue.remove(fitSeries)
        lineChartLog.data.getValue.remove(fitSeriesLog)
        fitResult.text = ""
      }
      if (autoGaussianFit.value) {
        Future {
          updateGaussianFit()
        }(executionContext)
      }
    }, None)
  }

  private val gaussianFitResult = new AtomicReference[Map[String, Double]](null)
  private val gaussianFitTime = new AtomicLong(0)
  private val maxPosition = new AtomicDouble(Double.NaN)

  def updateGaussianFit() = {
    assertThread("TDCViewer")
    val histogram = recentHistogram.get
    val iMax = histogram.indexOf(histogram.max)
    maxPosition set recentXData.get()(iMax)
    gaussianFitTime set System.currentTimeMillis

    val fit = pyMathInvoker.singlePeakGaussianFit(recentXData.get, recentHistogram.get).asInstanceOf[List[Any]]
    val a: Double = fit(0)
    val x0: Double = fit(1)
    val sigma: Double = fit(2)
    val fittedYData = recentXData.get.map(x => a * math.exp(-math.pow((x - x0), 2) / (2 * math.pow(sigma, 2))))
    gaussianFitResult set Map("Peak" -> x0, "FWHM" -> sigma * 2.35)
    Platform.runLater(() => {
      println("update")
      fitSeries.data = (recentXData.get zip fittedYData).map(toChartData)
      fitSeriesLog.data = (recentXData.get zip fittedYData).map(toChartData)
      fitResult.text = s"Peak: ${timeDomainNotation(x0)}, FWHM: ${timeDomainNotation(sigma * 2.35)}"
    })
    val pw = new PrintWriter(new FileOutputStream("Fit.csv", true))
    pw.println(s"${System.currentTimeMillis()}, ${x0}, ${sigma *2.35}")
    pw.close()
  }

  def createCheckButtonSetter(title: String, selected: Boolean, onChange: (Boolean) => Unit, buttonAction: Option[Tuple2[String, () => Unit]]) = {
    assertThread("JavaFX")
    val check = new CheckBox(title)
    check.selected = selected
    check.selected.onChange(onChange(check.selected.value))

    val buttonOption = buttonAction.map(ba => {
      val button = new Button(ba._1)
      button.onAction = (a) => ba._2()
      button
    })
    val pane = new AnchorPane()
    pane.children = List(check) ::: buttonOption.toList
    AnchorPane.setLeftAnchor(check, 0.0)
    AnchorPane.setTopAnchor(check, 0.0)
    AnchorPane.setBottomAnchor(check, 0.0)
    AnchorPane.setRightAnchor(check, 55.0)
    buttonOption.foreach(button => {
      AnchorPane.setLeftAnchor(button, 120.0)
      AnchorPane.setTopAnchor(button, 0.0)
      AnchorPane.setBottomAnchor(button, 0.0)
      AnchorPane.setRightAnchor(button, 0.0)
    })
    pane.prefHeight = 30
    pane
  }

  private def calcTick(min: Double, max: Double) = {
    val tickEstimated = (max - min) / 20
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

  def timeDomainNotation(timeInNs: Double) = {
    val formatter = new DecimalFormat("#,##0.000")
    val values = {
      if (timeInNs < 0.5) (timeInNs * 1e3, "ps")
      else if (timeInNs < 1e3) (timeInNs, "ns")
      else if (timeInNs < 1e6) (timeInNs / 1e3, "µs")
      else if (timeInNs < 1e9) (timeInNs / 1e6, "ms")
      else (timeInNs, "s")
    }
    s"${formatter.format(values._1)} ${values._2}"
  }

  def updateRegions() = {
    val newRegions = regionsRef.get
    val newColorMap = JythonBridge.regionColorMap.toMap
    Platform.runLater(() => {
      doUpdateRegions(lineChart, newRegions, newColorMap)
      doUpdateRegions(lineChartLog, newRegions, newColorMap)
    })
  }

  def doUpdateRegions(chart: AreaChart[Number, Number], newRegions: Map[String, List[Tuple2[Double, Double]]], newColorMap: Map[String, Color]) = {
    val series = chart.data.getValue
    val seriesExists = series.asScala.toList
    val seriesToBeAdded = new mutable.HashMap[String, List[Tuple2[Double, Double]]]()
    newRegions.foreach(e => seriesToBeAdded.put(e._1, e._2))
    seriesExists.filter(s => s.getName.startsWith("Region-")).foreach(s => if (seriesToBeAdded.contains(s.getName.substring(7))) {
      seriesToBeAdded.remove(s.getName.substring(7))
    } else series.remove(s))
    seriesToBeAdded.foreach(e => series.add(new XYChart.Series[Number, Number] {
      name = s"Region-${e._1}"
      data = Seq((0.0, 0.0)).map(toChartData)
    }.delegate))
    series.asScala.filter(s => s.getName.startsWith("Region-")).foreach(s => regionsRef.get.get(s.getName.substring(7)).foreach(newSeriesData => {
      val data = ListBuffer[Tuple2[Double, Double]]()
      newSeriesData.foreach(reg => {
        data += Tuple2(reg._1 - 0.001, -1)
        data += Tuple2(reg._1, Integer.MAX_VALUE.toDouble)
        data += Tuple2(reg._2, Integer.MAX_VALUE.toDouble)
        data += Tuple2(reg._2 + 0.001, -1)
      })
      new Series[Number, Number](s).data = data.map(toChartData)
    }))

    //Update color
    newColorMap.foreach(e => regionColorMap.put(s"Region-${e._1}", e._2))
    series.asScala.foreach(s => regionColorMap.get(s.getName).foreach(color => {
      val fill = s.getNode.lookup(".chart-series-area-fill")
      val line = s.getNode.lookup(".chart-series-area-line")
      val rgb = s"${(color.getRed() * 255).toInt}, ${(color.getGreen() * 255).toInt}, ${(color.getBlue() * 255).toInt}"
      fill.setStyle("-fx-fill: rgba(" + rgb + ", 0.15);")
      line.setStyle("-fx-stroke: rgba(" + rgb + ", 1.0);")
    }))
  }

  val jythonInitProperties = new Properties()
  jythonInitProperties.setProperty("python.import.site", "false")
  PythonInterpreter.initialize(System.getProperties, jythonInitProperties, new Array[String](0))
  val interpreter = new PythonInterpreter()

  def evalJython(counts: Array[Int], xData: Array[Double], yData: Array[Double], divide: Int) = {
    assertThread("TDCViewer")
    JythonBridge.counts = counts
    JythonBridge.histogramX = xData
    JythonBridge.histogramY = yData
    JythonBridge.histogramDivide = divide
    JythonBridge.title = "Untitled"
    JythonBridge.display = ""
    JythonBridge.regions.clear()
    try {
      val pre =
        """
          |from com.hydra.services.tdc.JythonBridge import *
          |from scalafx.scene.paint.Color import *
        """.stripMargin
      val logi = Source.fromFile("scripts/default.jy").getLines().toList.mkString(System.lineSeparator())
      val post =
        """
        """.stripMargin
      val code = List(pre, logi, post).mkString(System.lineSeparator())
      regionsRef.set(new mutable.HashMap[String, List[Tuple2[Double, Double]]]().toMap)
      interpreter.exec(code)
      val evalResults = (JythonBridge.title, JythonBridge.display)
      regionsRef set JythonBridge.regions.toMap
      Platform.runLater(() => {
        simpleCalcResultTitle.text = evalResults._1
        simpleCalcResult.text = evalResults._2
      })
    } catch {
      case e: PyException => {
        val out = new ByteArrayOutputStream()
        e.printStackTrace(new PrintStream(out))
        e.printStackTrace(new PrintStream(out))
        out.close()
        val errorMsg = new String(out.toByteArray)
        Platform.runLater(() => {
          simpleCalcResultTitle.text = JythonBridge.title + " [Error]"
          simpleCalcResult.text = errorMsg
        })
      }
      case e: Throwable => e.printStackTrace()
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
      updateResults()
      updateDelays()
      updateHistogramConfiguration()
    }(executionContext)
  }, 1000, 400)
}

object JythonBridge {
  var title = ""
  var counts = new Array[Int](0)
  var histogramX = new Array[Double](0)
  var histogramY = new Array[Double](0)
  var histogramDivide = 0
  var display = ""
  var regions = new mutable.HashMap[String, List[Tuple2[Double, Double]]]()
  val regionColorMap = new mutable.HashMap[String, Color]()

  def title(t: String): Unit = {
    title = t
  }

  def display(t: String): Unit = {
    display = t
  }

  def region(name: String, start: Double, stop: Double) = {
    val list = regions.getOrElseUpdate(name, List[Tuple2[Double, Double]]())
    regions(name) = list ::: List((start, stop))
  }

  def region(name: String, color: Color) = {
    regionColorMap.put(name, color)
  }

  def region(name: String) = regions.get(name) match {
    case None => 0.0
    case Some(reg) => histogramX.zip(histogramY).filter(z => !reg.map(range => z._1 < range._1 || z._1 > range._2).forall(b => b)).map(_._2).sum
  }

  Thread.setDefaultUncaughtExceptionHandler((t, e) => e.printStackTrace())

}
