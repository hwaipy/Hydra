package com.hydra.soap.bosonsampling.phasescanner

import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import java.awt.{BorderLayout, Color, Dimension, Toolkit}
import java.io._
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import javax.swing._
import java.util.concurrent.Executors
import java.util.Properties

import com.hydra.io.{AsynchronousRemoteObject, BlockingRemoteObject, MessageClient}
import com.xeiam.xchart.{ChartBuilder, SeriesMarker, StyleManager, XChartPanel}
import javax.imageio.ImageIO
import javax.swing.border.TitledBorder

import collection.mutable.{ArrayBuffer, ListBuffer}
import concurrent.{Await, ExecutionContext, Future}
import util.Random
//import language.postfixOps
import collection.JavaConverters._
import concurrent.duration.Duration

object PhaseScan extends App {
  val properties = new Properties
  val propertiesIn = new FileInputStream(new File("PhaseScanner.properties"))
  properties.load(propertiesIn)
  propertiesIn.close()


  val adcServices = "BSPS1" ::"BSPS2" ::"BSPS3" ::"BSPS4" ::"BSPS5" ::"BSPS6" ::"BSPS7" ::"BSPS8" :: Nil

  val client = MessageClient.newClient("192.168.25.27", 20102)
  val pix = client.blockingInvoker("PIX")
  //  val adcs = Range(0, 8).map(p => new SimulatedADC(8)).toList
  val adcs = adcServices.map(service => client.asynchronousInvoker(service))
  val scanner = new PhaseScanner(adcs, pix)

  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
  val frame = new PhaseScanFrame(scanner)
  frame.pack
  frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  frame.setVisible(true)

  //  object SimulatedADC {
  //    private val random = new Random()
  //  }
  //
  //  class SimulatedADC(val channelCount: Int) {
  //    private val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  //    private val phaseStart = Range(0, channelCount).toList.map(_ => SimulatedADC.random.nextDouble() * 2 * math.Pi)
  //    private val currentPosition = new AtomicReference[Double](0.0)
  //
  //    def measure() = {
  //      Future {
  //        val phase = currentPosition.get / 0.895 * 2 * 2 * math.Pi
  //        val values = phaseStart.map(phaseStart => math.sin(phaseStart + phase))
  //        values
  //      }(executionContext)
  //    }
  //
  //    def updateCurrentPosition(position: Double) = currentPosition.set(position)
  //  }

  class PhaseScanner(adcs: List[AsynchronousRemoteObject], pix: BlockingRemoteObject) {
    //TODO here the channelCount method need to be implemented in Arduino Board.
    //TODO make sue the future works currect.
    //    val adcChannelCounts = adcs.map(_.channelCount)
    val adcChannelCounts = adcs.map(_ => 8)
    val channelCount = adcChannelCounts.sum


    def gainAutoSense(dataIn: (Tuple2[Double, List[Double]]) => Unit, range: Range = Range(0, 500, 20)) {
      val gains = adcs.map(_ => Range(0, 8).map(_ => Unit)).flatten.map(_ => 6).toArray

      def setGains() = {
        adcs.zipWithIndex.map(z => {
          val adc = z._1
          val index = z._2
          adc.setGains(gains(index * 8), gains(index * 8 + 1), gains(index * 8 + 2), gains(index * 8 + 3), gains(index * 8 + 4), gains(index * 8 + 5), gains(index * 8 + 6), gains(index * 8 + 7))
        })
      }

      setGains()
      pix.setPosition(0)
      Thread.sleep(1000)
      val Rs = List(10e3, 20e3, 49.9e3, 100e3, 200e3, 510e3, 1e6);
      val gainLimits = Rs.map(R => 0.6 * 5 / R)
//       List(3.0E-4, 1.5E-4, 6.0120240480961926E-5, 3.0E-5, 1.5E-5, 5.882352941176471E-6, 3.0E-6)
      println(gainLimits)
      range.foreach(x => {
        pix.setPosition(x * 1.0)
        Thread.sleep(500)
        val measureFutures = adcs.map(adc => {
          adc.measure("30")
        })
        val rs = measureFutures.map(f => Await.result(f, Duration.Inf).toString)
        val ys = rs.map(r => r.split(" *, *").map(item => item.toDouble)).flatten
        dataIn((x / 1000.0, ys))
        ys.zipWithIndex.map(z => {
          val value = z._1
          val index = z._2
          val currentGain = gains(index)
          val currentGainLimit = gainLimits(currentGain)
          if (value > 0.7 * currentGainLimit) {
            gains(index) = List(gains(index) - 1, 0).max
            println(s"set gain ${index} to ${gains(index)}")
          }
        })
        setGains()
      })

      println(gains.zipWithIndex.foreach(println))
    }

    def scan(dataIn: (Tuple2[Double, List[Double]]) => Unit, range: Range = Range(0, 4000, 20)) {
      pix.setPosition(0)
      Thread.sleep(1000)
      range.foreach(x => {
        pix.setPosition(x * 1.0)
        Thread.sleep(500)
        val measureFutures = adcs.map(adc => {
          adc.measure("30")
        })
        val rs = measureFutures.map(f => Await.result(f, Duration.Inf).toString)
        val ys = rs.map(r => r.split(" *, *").map(item => item.toDouble)).flatten
        dataIn((x / 1000.0, ys))
      })
    }
  }

  class PhaseScanFrame(scanner: PhaseScanner) extends JFrame {
    private val contentPane = new JPanel
    setContentPane(contentPane)
    private val springLayout = new SpringLayout
    contentPane.setLayout(springLayout)
    contentPane.setPreferredSize(new Dimension(1000, 800))

    val toolbar = new JToolBar("")
    toolbar.setFloatable(false)
    //    private val gainAutoSenseButton = new JButton("GainSense")
    //    toolbar.add(gainAutoSenseButton)
    private val scanButton = new JButton("Scan")
    toolbar.add(scanButton)
    contentPane.add(toolbar, BorderLayout.NORTH)
    springLayout.putConstraint(SpringLayout.NORTH, toolbar, 0, SpringLayout.NORTH, contentPane)

    private val viewPane = new JPanel
    viewPane.setBackground(Color.BLUE)
    private val jScroll = new JScrollPane(viewPane)
    contentPane.add(jScroll)
    springLayout.putConstraint(SpringLayout.NORTH, jScroll, toolbar.getPreferredSize.height, SpringLayout.NORTH, contentPane)
    springLayout.putConstraint(SpringLayout.SOUTH, jScroll, 0, SpringLayout.SOUTH, contentPane)
    springLayout.putConstraint(SpringLayout.EAST, jScroll, 0, SpringLayout.EAST, contentPane)
    springLayout.putConstraint(SpringLayout.WEST, jScroll, 0, SpringLayout.WEST, contentPane)

    //    private val psps = (0 until scanner.channelCount).map(i => new PhaseScanPane(i)).toList
    private val psps = scanner.adcChannelCounts.zipWithIndex.map(countZ => (0 until countZ._1).map(i => new PhaseScanPane(countZ._2, i))).flatten
    private val springLayout2 = new SpringLayout
    viewPane.setLayout(springLayout2)
    psps.foreach(p => {
      viewPane.add(p)
      springLayout2.putConstraint(SpringLayout.EAST, p, 0, SpringLayout.EAST, viewPane)
      springLayout2.putConstraint(SpringLayout.WEST, p, 0, SpringLayout.WEST, viewPane)
    })
    springLayout2.putConstraint(SpringLayout.NORTH, psps.head, 0, SpringLayout.NORTH, viewPane)
    springLayout2.putConstraint(SpringLayout.SOUTH, psps.last, 0, SpringLayout.SOUTH, viewPane)
    psps.drop(1).zip(psps.dropRight(1)).foreach(z => springLayout2.putConstraint(SpringLayout.NORTH, z._1, 0, SpringLayout.SOUTH, z._2))
    viewPane.setPreferredSize(new Dimension(PhaseScanPane.PreferredSize.width, PhaseScanPane.PreferredSize.height * psps.size))

    scanButton.addActionListener((e) => {
      scanButton.setEnabled(false)
      psps.foreach(p => p.clear)
      new Thread(() => {
        //        println("Start")
        //        println("Started")
        scanner.gainAutoSense((data) => {
          psps.zip(data._2).foreach(z => z._1.newData(data._1, z._2))
        })
        //        scanner.stopGainAutoSense()
        SwingUtilities.invokeLater(() => {
          psps.foreach(p => p.clear)
          new Thread(() => {
            scanner.scan((data) => {
              psps.zip(data._2).foreach(z => z._1.newData(data._1, z._2))
            }, Range(0, 4000, 20))
            SwingUtilities.invokeLater(() => {
              scanButton.setEnabled(true)
            })
          }).start
        })
      }).start
    })
    toolbar.add(new AbstractAction("Fit All") {
      override def actionPerformed(e: ActionEvent) = {
        psps.foreach(p => p.doFit)
      }
    })
    toolbar.add(new AbstractAction("Copy Phase") {
      override def actionPerformed(e: ActionEvent) = {
        val s = psps.map(p => p.fitter.get() match {
          case null => 0
          case f => {(f.P + (if (f.A > 0) 0 else math.Pi))match{
            case p if p>math.Pi => p-2*math.Pi
            case p => p
          }}
        }).mkString("\t")
        Toolkit.getDefaultToolkit.getSystemClipboard.setContents(new StringSelection(s), null)
      }
    })
    toolbar.add(new AbstractAction("Save") {
      override def actionPerformed(e: ActionEvent) = {
        val timeStamp = System.currentTimeMillis
        val xData = psps.head.xDatas
        val yDatas = psps.map(p => p.yDatas).toList
        val iRange = math.min(xData.size, yDatas.map(yData => yData.size).min)
        val fileName = new File(s"Trace-${timeStamp}.csv")
        val pw = new PrintWriter(fileName)
        (0 until iRange).foreach(i => {
          pw.println(s"${xData(i)},${yDatas.map(y => y(i)).mkString(",")}")
        })
        pw.close
        val imageWidth = psps.map(_.getWidth).max
        val imageHeight = psps.map(_.getHeight).sum
        println(imageWidth)
        println(imageHeight)
        val bufferedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
        val g2 = bufferedImage.createGraphics()
        val y = new AtomicInteger(0)
        psps.foreach(psp => {
          psp.paint(g2)
          g2.translate(0, psp.getHeight)
        })
        g2.dispose()
        ImageIO.write(bufferedImage, "png", new File(s"Trace-${timeStamp}.png"))
      }
    })
  }

  object PhaseScanPane {
    val PreferredSize = new Dimension(800, 150)
    val executionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)
  }

  class PhaseScanPane(val deviceID: Int, val channel: Int) extends JPanel {
    val xDatas = new ArrayBuffer[Double]
    val yDatas = new ArrayBuffer[Double]
    val fitter = new AtomicReference[Fitter](new Fitter)
    setBorder(new TitledBorder(s"Phase Scanner ${deviceID + 1}-${channel + 1}"))
    val springLayout = new SpringLayout
    setLayout(springLayout)
    setBackground(Color.WHITE)

    val fitButton = new JButton("Fit")
    add(fitButton)
    springLayout.putConstraint(SpringLayout.NORTH, fitButton, 10, SpringLayout.NORTH, this)
    springLayout.putConstraint(SpringLayout.SOUTH, fitButton, 20, SpringLayout.NORTH, fitButton)
    springLayout.putConstraint(SpringLayout.WEST, fitButton, 15, SpringLayout.WEST, this)
    springLayout.putConstraint(SpringLayout.EAST, fitButton, 60, SpringLayout.WEST, fitButton)

    private val fittingValues = (0 until 4).map(i => new JTextField("")).toList
    fittingValues.foreach(fv => {
      fv.setBorder(null)
      fv.setEditable(false)
      fv.setFont(new java.awt.Font("Monospaced", 0, 11))
      add(fv)
      springLayout.putConstraint(SpringLayout.WEST, fv, 4, SpringLayout.WEST, this)
      springLayout.putConstraint(SpringLayout.EAST, fv, 80, SpringLayout.WEST, fv)
      springLayout.putConstraint(SpringLayout.SOUTH, fv, 20, SpringLayout.NORTH, fv)
    })
    springLayout.putConstraint(SpringLayout.NORTH, fittingValues.head, 10, SpringLayout.SOUTH, fitButton)
    fittingValues.drop(1).zip(fittingValues.dropRight(1)).foreach(z => springLayout.putConstraint(SpringLayout.NORTH, z._1, 5, SpringLayout.SOUTH, z._2))

    private val chart = new ChartBuilder().chartType(StyleManager.ChartType.Line).theme(StyleManager.ChartTheme.Matlab).width(600).height(400).build
    val chartPanel = new XChartPanel(chart)
    add(chartPanel)
    val scanSeries = chart.addSeries("ScanValue", Array[Double](0), Array[Double](0))
    val fittingSeries = chart.addSeries("FittingValue", Array[Double](0), Array[Double](0))
    fittingSeries.setMarker(SeriesMarker.NONE)
    chart.getStyleManager.setLegendVisible(false)
    springLayout.putConstraint(SpringLayout.NORTH, chartPanel, 0, SpringLayout.NORTH, this)
    springLayout.putConstraint(SpringLayout.SOUTH, chartPanel, 0, SpringLayout.SOUTH, this)
    springLayout.putConstraint(SpringLayout.WEST, chartPanel, 0, SpringLayout.EAST, fittingValues.head)
    springLayout.putConstraint(SpringLayout.EAST, chartPanel, 0, SpringLayout.EAST, this)

    fitButton.addActionListener((e) => {
      doFit
    })

    def doFit() {
      fitButton.setEnabled(false)
      val xData = xDatas.toList
      val yData = yDatas.toList
      Future {
        fitter.set(new Fitter(xData, yData))
        SwingUtilities.invokeLater(() => {
          updateFittingValueArea(fitter.get.A, fitter.get.W, fitter.get.P, fitter.get.B)
          updateChart
          fitButton.setEnabled(true)
        })
      }(PhaseScanPane.executionContext)
    }

    private def updateFittingValueArea(A: Double = 0, W: Double = 0, P: Double = 0, B: Double = 0, success: Boolean = true) {
      val s = success match {
        case true =>
          f"""
             |Pha: ${(P + (if (A > 0) 0 else math.Pi))match{
            case p if p>math.Pi => p-2*math.Pi
            case p => p
          }}%2.3f
             |  λ: ${2 * math.Pi / W}%2.1f
             |Amp: ${A}%2.3f
             |  B: ${B}%2.3f
                """.stripMargin
        case false =>
          """
            |Pha:
            |  λ:
            |Amp:
            |  B:
          """.stripMargin
      }
      s.split("\n").filter(s => s.size > 0).zip(fittingValues).foreach(z => z._2.setText(z._1))
    }

    override def getPreferredSize: Dimension = PhaseScanPane.PreferredSize

    def clear() {
      xDatas.clear
      yDatas.clear
      chartPanel.updateSeries("ScanValue", (0.0 :: Nil).asJava.asInstanceOf[java.util.List[Number]], (0.0 :: Nil).asJava.asInstanceOf[java.util.List[Number]], null)
      chartPanel.updateSeries("FittingValue", (0.0 :: Nil).asJava.asInstanceOf[java.util.List[Number]], (0.0 :: Nil).asJava.asInstanceOf[java.util.List[Number]], null)
      fitter.set(new Fitter)
      updateFittingValueArea(success = false)
    }

    def newData(x: Double, y: Double) {
      SwingUtilities.invokeLater(new Runnable {
        def run {
          xDatas += x
          yDatas += y
          updateChart
        }
      })
    }

    private def updateChart() {
      if (!SwingUtilities.isEventDispatchThread) {
        SwingUtilities.invokeLater(new Runnable {
          def run {
            updateChart
          }
        })
      }
      if (xDatas.size == yDatas.size && xDatas.size > 0) {
        chartPanel.updateSeries("ScanValue", xDatas.asJava.asInstanceOf[java.util.List[Number]], yDatas.asJava.asInstanceOf[java.util.List[Number]], null)
      }
      val fitData = fitter.get.fitData(xDatas.toList)
      if (xDatas.size == fitData.size && xDatas.size > 0) {
        chartPanel.updateSeries("FittingValue", xDatas.asJava.asInstanceOf[java.util.List[Number]], fitData.asJava.asInstanceOf[java.util.List[Number]], null)
      }
    }

    clear
  }

  class Fitter(xData: List[Double] = Nil, yData: List[Double] = Nil) {

    private val results = xData.size match {
      case s if s < 4 => 0.0 :: 0.0 :: 0.0 :: 0.0 :: Nil
      case _ => {
        try {
          val fitResults = client.blockingInvoker("PyMathService").sinFit(xData, yData, 2 * math.Pi / 0.895 * 2).asInstanceOf[List[Double]]
          fitResults
        } catch {
          case e: Throwable => {
            e.printStackTrace()
            ((yData.max - yData.min) / 1.414) :: 2 * math.Pi / (0.895 / 2) :: 0.0 :: 0.5 :: Nil
          }
        }
      }
    }
    val A = results(0)
    val W = results(1)
    val P = results(2)
    val B = results(3)

    def fitData(xs: List[Double]) = xs.map(x => A * math.sin(W * x + P) + B)
  }

}