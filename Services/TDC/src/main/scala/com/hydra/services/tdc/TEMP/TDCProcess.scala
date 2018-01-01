import java.io.InputStream
import java.net.{ServerSocket, Socket}
import java.util
import java.util.{ArrayList, Arrays}
import java.util.prefs.Preferences
import javax.swing.{JCheckBox, JTextField}

import com.hydra.services.tdc.TEMP.AppFrame
import com.hydra.services.tdc.TEMP.device.tdc.adapters.GroundTDCDataAdapter
import com.hydra.services.tdc.TEMP.device.tdc.{TDCDataProcessor, TDCParser}
import com.xeiam.xchart.{Chart, StyleManager, XChartPanel}
import org.jscience.mathematics.vector.ComplexMatrix

object TDCProcess extends App {
  //  private val SAMPLING_MODE_SAMPLING = "Sampling"
  //  private val SAMPLING_MODE_VISIBILITY = "Visibility"
  //  private val HISTOGRAM_MODE_BIN = "Bin"
  //  private val HISTOGRAM_MODE_PULSE = "Pulse"
  //  private val preferences = Preferences.userNodeForPackage(classOf[AppFrame])
  //  private var samplingChartPanel = null
  //  private var counterFields = null
  //  private var delayFields = null
  //  private var inputBoxs = null
  //  private var indexTrigger = 1
  //  private var indexSignal = 2
  //  private var viewFrom = -100
  //  private var viewTo = 100
  //  private var pulsePeriod = 13
  //  private var gateWidth = 4
  //  private var matrixFile = "."
  //  private var matrix = null
  //  private var doHistogram = true
  //  private var doSampling = false
  //  private var histogramMode = HISTOGRAM_MODE_BIN
  //  private var samplingMode = SAMPLING_MODE_SAMPLING
  //  private var samplingIntegrate = false
  //  private var triggerMode = false
  //  private val coincidences = new Array[Int](1 << 20)
  //  private var photonCounts = 0
  //  private var photonCountsTime = 0
  //  private val visibilities = new Array[Array[Double]](256, 4)
  //  private var permenents = null
  //  private var modes = null
  //  private var delayedPulse = 1
  //  private val infiniteCounters = new Array[Long](16)
  //  private var unitEndTime = 0
  //  private val delays = new Array[Double](16)

  private var timeEvents = new Array[util.ArrayList[_]](16)
  var i = 0
  while (i < timeEvents.length) {
    timeEvents(i) = new util.ArrayList[Long] {
      i += 1;
      i - 1
    }
  }
  initServer()
  private var histogramChart = new Chart(100, 400)
  histogramChart.addSeries("Histogram", Array[Int](0), Array[Int](0))
  histogramChart.getStyleManager.setLegendVisible(false)
  histogramChart.getStyleManager.setMarkerSize(0)
  private var histogramChartPanel = new XChartPanel(histogramChart)
  private var samplingChart = new Chart(640, 360)
  samplingChart.getStyleManager.setChartType(StyleManager.ChartType.Bar)
  samplingChart.addSeries("SamplingExpect", Array[Int](0), Array[Int](0))
  samplingChart.addSeries("SamplingExperiment", Array[Int](0), Array[Int](0))
  samplingChart.getStyleManager.setLegendVisible(false)
  samplingChart.getStyleManager.setMarkerSize(0)
  private var samplingChartPanel = new XChartPanel(samplingChart)
  counterFields = Array[JTextField](jTextFieldCount0, jTextFieldCount1, jTextFieldCount2, jTextFieldCount3, jTextFieldCount4, jTextFieldCount5, jTextFieldCount6, jTextFieldCount7, jTextFieldCount8, jTextFieldCount9, jTextFieldCount10, jTextFieldCount11, jTextFieldCount12, jTextFieldCount13, jTextFieldCount14, jTextFieldCount15)
  delayFields = Array[JTextField](jTextFieldDelay1, jTextFieldDelay2, jTextFieldDelay3, jTextFieldDelay4, jTextFieldDelay5, jTextFieldDelay6, jTextFieldDelay7, jTextFieldDelay8, jTextFieldDelay9, jTextFieldDelay10, jTextFieldDelay11, jTextFieldDelay12, jTextFieldDelay13, jTextFieldDelay14, jTextFieldDelay15, jTextFieldDelay16)
  inputBoxs = Array[JCheckBox](jCheckBoxInputs1, jCheckBoxInputs2, jCheckBoxInputs3, jCheckBoxInputs4, jCheckBoxInputs5, jCheckBoxInputs6, jCheckBoxInputs7, jCheckBoxInputs8, jCheckBoxInputs9, jCheckBoxInputs10, jCheckBoxInputs11, jCheckBoxInputs12, jCheckBoxInputs13, jCheckBoxInputs14, jCheckBoxInputs15)
  for (inputBox <- inputBoxs) {
    inputBox.setVisible(false)
  }
  loadPreferences()
  connectPreferences()
  postInit()

  private def initServer(): Unit = {
    val tdcParser = new TDCParser(new TDCDataProcessor() {
      override def process(data: Any): Unit = {
        if (data.isInstanceOf[util.ArrayList[_]]) {
          val list = data.asInstanceOf[util.ArrayList[_]]
          if (list.size > 0) dataIncome(list)
        }
        else throw new RuntimeException
      }
    }, new GroundTDCDataAdapter(Array[Int](0, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 21, 2, 3, 4, 5)))
    new Thread(new Runnable() {
      override def run(): Unit = {
        try {
          val server = new ServerSocket(20156)
          val buffer = new Array[Byte](1024 * 1024 * 16)
          while ( {
            !server.isClosed
          }) {
            val socket = server.accept
            System.out.println("Connected")
            try {
              val in = socket.getInputStream
              while ( {
                !socket.isClosed
              }) {
                val read = in.read(buffer)
                tdcParser.offer(util.Arrays.copyOfRange(buffer, 0, read))
              }
            } catch {
              case e: Exception =>

            }
            System.out.println("End")
          }
        } catch {
          case e: Exception =>
            e.printStackTrace(System.err)
        }
      }
    }).start()
    new Thread(new Runnable() {
      override def run(): Unit = {
        doFlushLoop()
      }
    }).start()
  }
}