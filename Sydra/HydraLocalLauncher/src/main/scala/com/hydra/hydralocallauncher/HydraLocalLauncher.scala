package com.hydra.hydralocallauncher

import java.util.concurrent.{Executors, ThreadFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.File
import java.math.BigInteger
import java.net.{HttpURLConnection, URL}
import java.nio.file._
import java.security.MessageDigest

import scala.collection.JavaConverters._
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Dimension2D, Point2D}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout.AnchorPane
import java.util.concurrent.atomic.AtomicInteger
import java.util.prefs.Preferences
import javafx.stage.Screen

import com.hydra.io.MessageClient

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success}
import scalafx.geometry.Pos
import scalafx.scene.text.Font

object HydraLocalLauncher extends JFXApp {
  val DEBUG = new File(".").getAbsolutePath.contains("GitHub")

  val root = Paths.get(System.getProperty("user.home"), "HydraLocal/")
  if (Files.notExists(root, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(root)

  val preferences = Preferences.userRoot.node("/Hydra/HydraLocalLauncher")
  val frameSize = new Dimension2D(500, 300)
  val visualBounds = Screen.getPrimary.getVisualBounds
  val framePosition = new Point2D(
    visualBounds.getMinX + (visualBounds.getMaxX - visualBounds.getMinX - frameSize.width) / 2,
    visualBounds.getMinY + (visualBounds.getMaxY - visualBounds.getMinY - frameSize.height) / 2)

  val textFieldHost = new TextField() {
    font = Font.font("Ariel", 30)
    alignment = Pos.Center
    promptText = "Server Address"
    style = s"-fx-base: #FFFFFF;"
    text = preferences.get("Host", "")
  }
  val buttonLaunch = new Button("Launch") {
    font = Font.font("Ariel", 20)
    disable = true
    onAction = (action) => {
      disable = true
      launch(textFieldHost.text.value)
    }
  }
  val processBar = new ProgressBar() {
    visible = false
  }

  stage = new PrimaryStage {
    title = "Hydra Local Launcher"
    x = framePosition.x
    y = framePosition.y
    resizable = false
    scene = new Scene {
      root = new AnchorPane {
        prefWidth = frameSize.width
        prefHeight = frameSize.height
        AnchorPane.setTopAnchor(textFieldHost, 40.0)
        AnchorPane.setLeftAnchor(textFieldHost, 40.0)
        AnchorPane.setBottomAnchor(textFieldHost, 200.0)
        AnchorPane.setRightAnchor(textFieldHost, 40.0)
        AnchorPane.setTopAnchor(buttonLaunch, 145.0)
        AnchorPane.setLeftAnchor(buttonLaunch, 175.0)
        AnchorPane.setBottomAnchor(buttonLaunch, 105.0)
        AnchorPane.setRightAnchor(buttonLaunch, 175.0)
        AnchorPane.setTopAnchor(processBar, 235.0)
        AnchorPane.setLeftAnchor(processBar, 40.0)
        AnchorPane.setBottomAnchor(processBar, 50.0)
        AnchorPane.setRightAnchor(processBar, 40.0)
        children = Seq(processBar, buttonLaunch, textFieldHost)

        textFieldHost.text.onChange { (a, b, newText) => validateHost(newText)
        }
      }
    }
  }

  private def validateHost(newText: String, moveFocus: Boolean = false) = {
    def revealValidationResult(right: Boolean) {
      Platform.runLater {
        if (newText == textFieldHost.text.value) {
          val colorCode = right match {
            case true => "#90EE90"
            case false => "#FFB6C1"
          }
          textFieldHost.style = s"-fx-base: ${colorCode};"
          buttonLaunch.disable = !right
          if (moveFocus) buttonLaunch.requestFocus
        }
      }
    }

    buttonLaunch.disable = true
    textFieldHost.style = s"-fx-base: #FFFFFF;"
    checkHostValidation(newText).onComplete {
      case Success(suc) => revealValidationResult(suc)
      case Failure(fai) => revealValidationResult(false)
    }
  }

  private val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(new ThreadFactory {
    val counter = new AtomicInteger(0)

    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r, s"CachedThreadPool-[HydraLocalLauncher]-${counter.getAndIncrement}")
      t.setDaemon(true)
      t
    }
  }))

  private def checkHostValidation(text: String) = {
    Future {
      //      val url = new URL(s"http://${text}/wydra/hydralocal?validate")
      //      val connection = url.openConnection.asInstanceOf[HttpURLConnection]
      //      connection.getResponseCode match {
      //        case 200 => Source.fromInputStream(connection.getInputStream).getLines.toList.head == "This is HydraLocal."
      //        case _ => false
      //      }
      val host = parseHost(text)
      val client = MessageClient.newClient(host._1, host._2, "")
    }(executionContext)
  }

  private def launch(host: String) {
    preferences.put("Host", host)
    textFieldHost.editable = false
    buttonLaunch.disable = true
    processBar.visible = true
    processBar.progress = -1
    Future {
      val url = new URL(s"http://${host}/wydra/hydralocal?list")
      val connection = url.openConnection.asInstanceOf[HttpURLConnection]
      connection.getResponseCode match {
        case 200 => {
          val lines = Source.fromInputStream(connection.getInputStream, "UTF-8").getLines.toList
          val task = new UpdateFileTask(lines)

          true
        }
        case _ => false
      }
    }(executionContext).onComplete {
      case Success(suc) => afterLaunch(suc)
      case Failure(fai) => afterLaunch(false); fai.printStackTrace();
    }

    def afterLaunch(success: Boolean) = {
      success match {
        case true => Platform.exit()
        case false => {


          //          moveAnimation(true)
        }
      }
    }
  }

  private def moveAnimation(moveOut: Boolean) = {
    //    textFieldHost.editable = !moveOut
    //    buttonLaunch.disable = moveOut
    //    val currentX = buttonLaunch.layoutX.value
    //    val range = moveOut match {
    //      case true => (0, -100)
    //      case false => (-100, 0)
    //    }
    //    val duration = 500.0
    //    val period = 10
    //    val startTime = System.nanoTime / 1000000
    //
    //    def calculatePosition(time: Long) = {
    //      val deltaTime = time - startTime
    //      val phi = (deltaTime / duration) * math.Pi
    //      val p = (1 - math.cos(phi)) / 2
    //      range._1 + (range._2 - range._1) * p
    //    }
    //
    //    val timer = new Timer("Move Animation Timer", true)
    //    timer.schedule(new TimerTask {
    //      override def run(): Unit = {
    //        val currentTime = System.nanoTime / 1000000
    //        if (currentTime > startTime + duration) cancel()
    //        val x = calculatePosition(currentTime)
    //        println(x)
    //        Platform.runLater {
    //          AnchorPane.setLeftAnchor(buttonLaunch, 175.0 + x)
    //          AnchorPane.setRightAnchor(buttonLaunch, 175.0 - x)
    //        }
    //      }
    //    }, 0, period)

  }

  validateHost(textFieldHost.text.value, true)

  class UpdateFileTask(lines: List[String]) {
    val totalWork = new AtomicInteger(0)
    val doneWork = new AtomicInteger(0)

    def process = {
      val remoteMap = new mutable.HashMap[String, FileEntry]()
      Range(0, lines.size / 4).foreach(i => remoteMap.put(lines(i * 4 + 0),
        new FileEntry(Paths.get(lines(i * 4 + 0)), lines(i * 4 + 1).toLong, lines(i * 4 + 2).toLong, lines(i * 4 + 3))))
      val remotePaths = remoteMap.keys.toList

      val localPaths = Files.walk(root, FileVisitOption.FOLLOW_LINKS).iterator.asScala.toList
        .filter(p => Files.isRegularFile(p)).map(p => root.relativize(p))
      localPaths.filterNot(remotePaths.contains).foreach(p => Files.delete(root.resolve(p)))

      val todoList = ArrayBuffer[FileEntry]()
      remoteMap.values.toList.foreach(entry => {
        if (!localPaths.contains(entry.path.toString) || calculateMD5(entry.path) != entry.hash) {
          todoList += entry
        }
      })

      todoList.foreach(todo => println(todo.path))
    }

    def progress = totalWork.get match {
      case 0 => -1
      case _ => doneWork.get / totalWork.get
    }


    //      val newKeys = paths.map(p => p.toString)
    //      resources.keys.toList.foreach(key => if (!newKeys.contains(key)) {
    //        resources.remove(key)
    //      })
    //      paths.foreach(path => {
    //        val attributes = Files.readAttributes[BasicFileAttributes](path, classOf[BasicFileAttributes], LinkOption.NOFOLLOW_LINKS)
    //        val size = attributes.size
    //        val lastModified = attributes.lastModifiedTime.toMillis
    //        val oldEntryOption = resources.get(path.toString)
    //        if (oldEntryOption == None || oldEntryOption.get.lastModified < lastModified) {
    //          resources(path.toString) = new FileEntry(path, lastModified, size, calculateMD5(path))
    //        }
    //      })

  }

  case class FileEntry(val path: Path, val lastModified: Long, val size: Long, val hash: String)

  private def calculateMD5(path: Path) = {
    val bytes = Files.readAllBytes(path)
    val md = MessageDigest.getInstance("MD5")
    md.update(bytes)
    new BigInteger(1, md.digest).toString(16)
  }

  private def parseHost(text: String) = {
    val split = text.split(":")
    val server = split(0)
    val port = split.size match {
      case 1 => 20102
      case _ => split(1).toInt
    }
    (server, port)
  }
}