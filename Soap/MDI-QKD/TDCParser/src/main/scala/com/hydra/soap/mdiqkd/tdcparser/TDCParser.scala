package com.hwaipy.wow

import java.awt.event.{InputEvent, KeyEvent}
import java.awt.{Color, Rectangle, Robot}
import java.io.{File, FileReader}
import java.util.Properties
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.prefs.Preferences

import javax.imageio.ImageIO
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Dimension2D, Insets, Point2D}
import scalafx.scene.layout._
import scalafx.stage.{Screen, StageStyle}
import scalafx.Includes._

import scala.concurrent.{ExecutionContext, Future}
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, ToggleButton}

import scala.collection.mutable.ListBuffer
import scala.language.reflectiveCalls
import scala.util.Random

object PD extends JFXApp {
  val properties = new Properties()
  val pIn = new FileReader("config.properties")
  properties.load(pIn)
  pIn.close()
  val executionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor(new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r, s"Fisher Thread")
      t.setDaemon(true)
      t.setUncaughtExceptionHandler((t: Thread, e: Throwable) => e.printStackTrace())
      t
    }
  }))
  val robot = new Robot()
  val random = new Random()
  val visualBounds = Screen.primary.visualBounds
  val actionDimension = (visualBounds.width * 0.4 + properties.getProperty("UI.Action.WidthExt", "0").toDouble,
    visualBounds.height * 0.4 + properties.getProperty("UI.Action.HeightExt", "0").toDouble)
  val actionBounds = (
    (visualBounds.width - actionDimension._1) / 2 + properties.getProperty("UI.Action.XExt", "0").toDouble,
    (visualBounds.height - actionDimension._2) / 2 + properties.getProperty("UI.Action.YExt", "0").toDouble,
    actionDimension._1, actionDimension._2)
  val captureBounds = (
    (actionBounds._1 + properties.getProperty("UI.Capture.XExt", "0").toDouble).toInt,
    (actionBounds._2 + properties.getProperty("UI.Capture.YExt", "0").toDouble).toInt,
    (actionBounds._3 + properties.getProperty("UI.Capture.WidthExt", "0").toDouble).toInt,
    (actionBounds._4 + properties.getProperty("UI.Capture.HeightExt", "0").toDouble).toInt
  )
  //val frameSize = new Dimension2D(visualBounds.width * 0.4, visualBounds.height * 0.48)
  //val framePosition = new Point2D(
  //  visualBounds.getMinX + (visualBounds.getMaxX - visualBounds.getMinX - frameSize.width) / 2,
  //  visualBounds.getMinY + (visualBounds.getMaxY - visualBounds.getMinY - frameSize.height) / 2*1.5)
  val fishing = new AtomicBoolean(false)
  val exited = new AtomicBoolean(false)
  var positionShower: (Double, Double) => Unit = null
  //Future[Unit] {
  //  var status = "Ready"
  //  var waitingStart = 0l
  //  val ysP = new ListBuffer[Double]()
  //  val ysW = new ListBuffer[Double]()
  //  while (!exited.get) {
  //    if (!fishing.get) {
  //      Thread.sleep(1000)
  //      status = "Ready"
  //    }
  //    else {
  //      if (status == "Ready") {
  //        ysP.clear()
  //        ysW.clear()
  //        robot.mouseMove((visualBounds.width /2).toInt, (visualBounds.height / 2).toInt)
  //        Thread.sleep(50 + random.nextInt(30))
  //        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
  //        Thread.sleep(50 + random.nextInt(30))
  //        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
  //
  //        Thread.sleep(50 + random.nextInt(30))
  //        robot.keyPress(KeyEvent.VK_ENTER)
  //        Thread.sleep(50 + random.nextInt(30))
  //        robot.keyRelease(KeyEvent.VK_ENTER)
  //        Thread.sleep(50 + random.nextInt(30))
  //        robot.keyPress(KeyEvent.VK_2)
  //        Thread.sleep(50 + random.nextInt(30))
  //        robot.keyRelease(KeyEvent.VK_2)
  //        Thread.sleep(50)
  //        Thread.sleep(3000)
  //        status = "Prepare"
  //        waitingStart = System.nanoTime()
  //      }
  //      if (status == "Prepare") {
  //        val waited = (System.nanoTime() - waitingStart) / 1e9
  //        if (waited > 3) status = "Waiting"
  //        else {
  //          ysP += capture()._2
  //        }
  //      }
  //      if (status == "Waiting") {
  //        val waited = (System.nanoTime() - waitingStart) / 1e9
  //        if (waited > 25) status = "Ready"
  //        else {
  //          ysW += capture()._2
  //          if (ysW.max - ysW.min > 2 * (ysP.max - ysP.min)) {
  //            Thread.sleep(800 + random.nextInt(800))
  //            val position = capture()
  //            robot.mouseMove((framePosition.x + position._1).toInt, (framePosition.y + position._2).toInt)
  //            Thread.sleep(50 + random.nextInt(30))
  //            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK)
  //            Thread.sleep(50 + random.nextInt(30))
  //            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK)
  //            Thread.sleep(50 + random.nextInt(30))
  //            Thread.sleep(1000 + random.nextInt(1000))
  //            if (random.nextDouble() < 0.05) {
  //              robot.keyPress(KeyEvent.VK_1)
  //              Thread.sleep(50 + random.nextInt(30))
  //              robot.keyRelease(KeyEvent.VK_1)
  //              Thread.sleep(1000 + random.nextInt(1000))
  //            }
  //            if (random.nextDouble() < 0.5) {
  //              robot.keyPress(KeyEvent.VK_SPACE)
  //              Thread.sleep(50 + random.nextInt(30))
  //              robot.keyRelease(KeyEvent.VK_SPACE)
  //              Thread.sleep(3000 + random.nextInt(3000))
  //            }
  //            status = "Ready"
  //          }
  //        }
  //      }
  //    }
  //  }
  //}(executionContext)

  stage = new PrimaryStage {
    title = "Fisher"
    x = 0
    y = 0
    width = visualBounds.width
    height = visualBounds.height
    scene = new Scene {
      stylesheets.add(ClassLoader.getSystemClassLoader.getResource("com/hwaipy/wow/PD.css").toExternalForm)
      root = new AnchorPane {
        styleClass += "rootPane"
        val configPane = new HBox {
          spacing = 10
          padding = Insets(10, 10, 10, 10)
          styleClass += "configPane"
          prefHeight = 50 + properties.getProperty("UI.Config.HeightExt", "0").toDouble
          prefWidth = 400 + properties.getProperty("UI.Config.WidthExt", "0").toDouble

          val quitButton = new Button("Quit") {
            onAction = () => {
              exited set true
              stage.close()
            }
          }
          val snapshotButton = new Button("Snapshot") {
            onAction = () => {
              val center = capture(true)
              actionMouseClick(center._1 + captureBounds._1, center._2 + captureBounds._2)
            }
          }
          val fishingButton = new ToggleButton("Go Fish") {
            onAction = () => {
              fishing set this.selected.value
            }
          }
          children = Seq(
            snapshotButton,
            fishingButton,
            new AnchorPane {prefWidth = 20},
            new AnchorPane {prefWidth = 20},
            quitButton)
        }
        val actionPane = new AnchorPane {
          styleClass += "actionPane"
          prefHeight = actionBounds._4
          prefWidth = actionBounds._3

          val targetPane = new AnchorPane() {
            styleClass += "targetPane"
            prefWidth = 120 + properties.getProperty("UI.Target.RExt", "0").toDouble
            prefHeight = prefWidth.value
          }
          targetPane.visible = false
          children = Seq(targetPane)

          def showTargetPane(x: Double, y: Double) = {
            AnchorPane.setLeftAnchor(targetPane, x - targetPane.prefWidth.value / 2 + properties.getProperty("UI.Target.XExt", "0").toDouble)
            AnchorPane.setTopAnchor(targetPane, y - targetPane.prefHeight.value / 2 + properties.getProperty("UI.Target.YExt", "0").toDouble)
            targetPane.visible = true
          }

          positionShower = showTargetPane
        }
        val packagePane = new AnchorPane {
          styleClass += "packagePane"

          val cellCountX = properties.getProperty("Package.Count.X", "5").toInt
          val cellCountY = properties.getProperty("Package.Count.Y", "5").toInt
          val cellSize = properties.getProperty("Package.Cell.Size", "25").toDouble
          prefHeight = cellSize * cellCountY
          prefWidth = cellSize * cellCountX

          children = Range(0, cellCountX).map(x => Range(0, cellCountY).map(y => {
            val packageCellPane = new AnchorPane {
              styleClass += "packageCellPane"
              prefHeight = cellSize
              prefWidth = cellSize
            }
            AnchorPane.setLeftAnchor(packageCellPane, cellSize * x)
            AnchorPane.setTopAnchor(packageCellPane, cellSize * y)
            packageCellPane
          })).flatten
        }

        AnchorPane.setTopAnchor(configPane, properties.getProperty("UI.Config.YExt", "0").toDouble)
        AnchorPane.setLeftAnchor(configPane, (visualBounds.width - configPane.prefWidth.value) / 2 + properties.getProperty("UI.Config.XExt", "0").toDouble)
        AnchorPane.setTopAnchor(actionPane, actionBounds._2)
        AnchorPane.setLeftAnchor(actionPane, actionBounds._1)
        AnchorPane.setTopAnchor(packagePane, visualBounds.height * 0.4 + properties.getProperty("UI.Package.YExt", "0").toDouble)
       //AnchorPane.setRightAnchor(packagePane, visualBounds.width * 0.05 + properties.getProperty("UI.Package.XExt", "0").toDouble)

        children = Seq(configPane, actionPane, packagePane)
      }
    }
    scene.value.setFill(null)
  }
  stage.initStyle(StageStyle.Transparent)
  stage.alwaysOnTop = true

  def capture(snapshotFile: Boolean = false) = {
    val screenCapture = robot.createScreenCapture(new Rectangle(captureBounds._1, captureBounds._2, captureBounds._3, captureBounds._4))
    val originalData = new Array[Int](screenCapture.getWidth * screenCapture.getHeight * 3)
    screenCapture.getData.getPixels(0, 0, screenCapture.getWidth, screenCapture.getHeight, originalData)
    val filteredData = new Array[Int](screenCapture.getWidth * screenCapture.getHeight)
    for (i <- 0 until filteredData.size) {
      //filteredData(i) = if (originalData(i * 3) > originalData(i * 3 + 1) * 1.5 && originalData(i * 3 + 1) > originalData(i * 3 + 2) * 1.5) 1 else 0
      filteredData(i) = if (originalData(i * 3) > originalData(i * 3 + 1) && originalData(i * 3) > originalData(i * 3 + 2)) 1 else 0
    }

    if (snapshotFile) {
      ImageIO.write(screenCapture, "png", new File("SS.png"))
      for (x <- 0 until screenCapture.getWidth) {
        for (y <- 0 until screenCapture.getHeight) {
          if (filteredData(y * screenCapture.getWidth + x) > 0) screenCapture.setRGB(x, y, Color.BLACK.getRGB)
          else screenCapture.setRGB(x, y, Color.WHITE.getRGB)
        }
      }
      ImageIO.write(screenCapture, "png", new File("SSF.png"))
    }

    var centerXSum = 0
    var centerYSum = 0
    var weight = 0
    for (x <- 0 until screenCapture.getWidth) {
      for (y <- 0 until screenCapture.getHeight) {
        if (filteredData(y * screenCapture.getWidth + x) > 0) {
          centerXSum += x
          centerYSum += y
          weight += 1
        }
      }
    }
    val center = (centerXSum.toDouble / weight, centerYSum.toDouble / weight)
    positionShower(center._1, center._2)
    center
  }

  def actionMouseClick(x: Double, y: Double, isLeft: Boolean = true) = {
    val button = if (isLeft) InputEvent.BUTTON1_DOWN_MASK else InputEvent.BUTTON3_DOWN_MASK
    robot.mouseMove(x.toInt, y.toInt)
    delay(50, 100)
    robot.mousePress(button)
    delay(50, 100)
    robot.mouseRelease(button)
    delay(50, 100)
  }

  def delay(from: Int, to: Int) = Thread.sleep(random.nextInt(to - from) + from)
}