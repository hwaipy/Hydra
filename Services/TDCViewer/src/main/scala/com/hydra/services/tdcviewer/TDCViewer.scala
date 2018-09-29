package com.hydra.services.tdc

import java.io.File
import java.util.{Timer, TimerTask}
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong, AtomicReference}

import scalafx.application.Platform
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Dimension2D, Point2D}
import scalafx.scene.layout.{AnchorPane, Region}
import scalafx.stage.Screen
import com.hydra.io.MessageClient

import scalafx.scene.control.{Button, CheckBox, Label, TextField}
import com.hydra.`type`.NumberTypeConversions._
import com.hydra.core.MessageGenerator

import scalafx.application.JFXApp
import scalafx.beans.property.DoubleProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.chart.LineChart
import scalafx.scene.chart.NumberAxis
import scalafx.scene.chart.XYChart


object TDCViweer extends JFXApp {
  val DEBUG = new File(".").getAbsolutePath.contains("GitHub")
  System.setProperty("log4j.configurationFile", "./config/tdcviewer.debug.log4j.xml")

  val executionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor(new ThreadFactory {
    val counter = new AtomicInteger(0)

    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r, s"CachedThreadPool-[HydraLocalLauncher]-${counter.getAndIncrement}")
      t.setDaemon(true)
      t.setUncaughtExceptionHandler((t: Thread, e: Throwable) => println(e))
      t
    }
  }))

  val client = MessageClient.newClient(parameters.named.get("host") match {
    case Some(host) => host
    case None => "localhost"
  }, parameters.named.get("port") match {
    case Some(port) => port.toInt
    case None => 20102
  })
  val storageInvoker = client.blockingInvoker("StorageService")
  val tdcInvoker = client.blockingInvoker("GroundTDCServer")
  val path = "/test/tdc/default.fs"
  val recentSize = new AtomicInteger(0)

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
  val chartPane = new AnchorPane()

  val histogramSyncChannel = DoubleProperty(1)
  val histogramSignalChannel = DoubleProperty(2)

  val configurationFields: List[Region] =
    createHistogramChannelSetter("Sync") :: createHistogramChannelSetter("Signal") :: createIntegrateCheckSetter() :: Nil
  //  createAutoFitCheckSetter()::
  val configurationPane = new AnchorPane()

  // Helper function to convert a tuple to `XYChart.Data`
  val toChartData = (xy: (Double, Double)) => XYChart.Data[Number, Number](xy._1, xy._2)

  val series = new XYChart.Series[Number, Number] {
    name = "Histogram"
    data = Seq((0.0, 0.0)).map(toChartData)
  }
  val lineChart = new LineChart[Number, Number](xAxis, yAxis, ObservableBuffer(series))
  lineChart.setAnimated(false)
  lineChart.setLegendVisible(false)
  lineChart.setCreateSymbols(false)

  //  val textFieldHost = new TextField() {
  //    font = Font.font("Ariel", 30)
  //    alignment = Pos.Center
  //    promptText = "Server Address"
  //    style = s"-fx-base: #FFFFFF;"
  //    text = preferences.get("Host", "")
  //  }
  //  val buttonLaunch = new Button("Launch") {
  //    font = Font.font("Ariel", 20)
  //    onAction = (action) => launch(textFieldHost.text.value)
  //  }
  //  val processBar = new ProgressBar() {
  //    visible = false
  //  }
  //
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

        //ChartPane
        chartPane.children = List(lineChart)
        AnchorPane.setTopAnchor(chartPane, 0.0)
        AnchorPane.setLeftAnchor(chartPane, 240.0)
        AnchorPane.setRightAnchor(chartPane, 0.0)
        AnchorPane.setBottomAnchor(chartPane, 0.0)

        //ConfigurationPane
        configurationPane.children = configurationFields
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
  val recentHistogram = new AtomicReference[Array[Int]](new Array[Int](0))
  val integrated = new AtomicBoolean(false)

  def updateResults() = {
    val size: Long = storageInvoker.metaData("", path, false).asInstanceOf[Map[String, Any]]("Size")
    if (size != recentSize) {
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
      val syncChannel: Int = histogram("SyncChannel")
      val signalChannel: Int = histogram("SignalChannel")
      println(s"histogram between $syncChannel and $signalChannel")
      val viewStart: Long = histogram("ViewFrom")
      val viewStop: Long = histogram("ViewTo")
      val histo = histogram("Histogram").asInstanceOf[List[Int]].toArray
      if (integrated.get && recentViewStart.get == viewStart && recentViewStop.get == viewStop && recentHistogram.get.size == histo.size) {
        recentHistogram set recentHistogram.get.zip(histo).map(z => z._1 + z._2)
      } else recentHistogram set histo
      recentViewStart set viewStart
      recentViewStop set viewStop
      val xTick = calcTick(viewStart / 1000.0, viewStop / 1000.0)
      xAxis.lowerBound = xTick._1
      xAxis.upperBound = xTick._2
      xAxis.tickUnit = xTick._3
      val binWidth = (viewStop - viewStart) / 1000.0 / histo.size
      Platform.runLater(() => {
        series.data = recentHistogram.get.zipWithIndex.map(z => (xAxis.lowerBound.value + binWidth * (z._2 + 0.5), z._1.toDouble)).map(toChartData)
      })
      val yTick = calcTick(recentHistogram.get.min.toDouble, recentHistogram.get.max.toDouble)
      yAxis.lowerBound = yTick._1
      yAxis.upperBound = yTick._2
      yAxis.tickUnit = yTick._3
    }
  }

  def updateDelays() = {
    val delays = tdcInvoker.getDelays().asInstanceOf[List[Any]].map(i => {
      val d: Long = i
      d
    })
    Platform.runLater(() => delayFields.zip(delays).foreach(z => if (!z._1.focused.value) z._1.text = s"${z._2 / 1000.0}"))
  }

  def updateHistogramConfiguration() = {
    tdcInvoker.getAnalyserConfiguration("Histogram")
  }


  new Timer(true).schedule(new TimerTask {
    override def run() = Future {
      updateResults()
      updateDelays()
    }(executionContext)
  }, 400, 400)

  def displayCounterResult(counterResult: List[Int]) = Platform.runLater(() => counterResult.zip(counterFields).foreach(z => z._2.text = s"${z._1}"))

  def createHistogramChannelSetter(title: String) = {
    val label = new Label(title)
    val field = new TextField()
    field.focused.onChange((a, b, c) => if (field.focused.value) {
      val v = field.text.value
      try {
        val dv = v.toInt
        Future {
          tdcInvoker.configureAnalyser("Histogram", Map(field.text.value -> dv))
          updateHistogramConfiguration()
        }(executionContext)
      } catch {
        case e: Throwable =>
      }
    })
    field.text = if (title == "Sync") "1" else "2"


    val pane = new AnchorPane()
    pane.children = Seq(label, field)
    AnchorPane.setLeftAnchor(label, 0.0)
    AnchorPane.setTopAnchor(label, 0.0)
    AnchorPane.setBottomAnchor(label, 0.0)
    AnchorPane.setRightAnchor(label, 50.0)
    AnchorPane.setLeftAnchor(field, 55.0)
    AnchorPane.setTopAnchor(field, 0.0)
    AnchorPane.setBottomAnchor(field, 0.0)
    AnchorPane.setRightAnchor(field, 0.0)
    pane.prefHeight = 35
    pane
  }

  def createIntegrateCheckSetter() = {
    val check = createCheckSetter("Integrated", false, (s) => integrated.set(s))
    val button = new Button("Clear")
    button.onAction = (a) => recentHistogram set new Array[Int](0)
    val pane = new AnchorPane()
    pane.children = Seq(check, button)
    AnchorPane.setLeftAnchor(check, 0.0)
    AnchorPane.setTopAnchor(check, 0.0)
    AnchorPane.setBottomAnchor(check, 0.0)
    AnchorPane.setRightAnchor(check, 55.0)
    AnchorPane.setLeftAnchor(button, 120.0)
    AnchorPane.setTopAnchor(button, 0.0)
    AnchorPane.setBottomAnchor(button, 0.0)
    AnchorPane.setRightAnchor(button, 0.0)
    pane.prefHeight = 35
    pane
  }

  private def createCheckSetter(title: String, selected: Boolean = false, onChange: (Boolean) => Unit) = {
    val cb = new CheckBox(title)
    cb.selected = selected
    cb.selected.onChange((a, b, c) => onChange(cb.selected.value))
    cb.prefHeight = 35
    cb
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
}