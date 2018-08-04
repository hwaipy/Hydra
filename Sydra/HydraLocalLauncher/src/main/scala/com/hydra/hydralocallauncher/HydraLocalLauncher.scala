package com.hydra.hydralocallauncher

import java.util.concurrent.{Executors, ThreadFactory}

import scala.concurrent.{ExecutionContext, Future}
import java.io.File
import java.nio.ByteBuffer
import java.nio.file._
import java.nio.file.attribute.FileTime
import java.util.{Timer, TimerTask}

import scala.collection.JavaConverters._
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Dimension2D, Point2D}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout.AnchorPane
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference}
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
  System.setProperty("log4j.configurationFile", "./config/hydralocallauncher.debug.log4j.xml")

  val localRoot = Paths.get(System.getProperty("user.home"), "HydraLocal/release/")
  if (Files.notExists(localRoot, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(localRoot)

  val preferences = Preferences.userRoot.node("/Hydra/HydraLocalLauncher")
  val frameSize = new Dimension2D(500, 300)
  val visualBounds = Screen.getPrimary.getVisualBounds
  val framePosition = new Point2D(
    visualBounds.getMinX + (visualBounds.getMaxX - visualBounds.getMinX - frameSize.width) / 2,
    visualBounds.getMinY + (visualBounds.getMaxY - visualBounds.getMinY - frameSize.height) / 2)

  val clientRef = new AtomicReference[MessageClient]

  val textFieldHost = new TextField() {
    font = Font.font("Ariel", 30)
    alignment = Pos.Center
    promptText = "Server Address"
    style = s"-fx-base: #FFFFFF;"
    text = preferences.get("Host", "")
  }
  val buttonLaunch = new Button("Launch") {
    font = Font.font("Ariel", 20)
    onAction = (action) => launch(textFieldHost.text.value)
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
      }
    }
    onCloseRequest = (window) => {
      clientRef.get match {
        case c if c != null => c.stop
        case _ =>
      }
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

  private def launch(host: String) {
    preferences.put("Host", host)
    textFieldHost.editable = false
    buttonLaunch.disable = true
    processBar.visible = true
    processBar.progress = -1
    Future {
      val parsedHost = parseHost(host)
      val client = MessageClient.newClient(parsedHost._1, parsedHost._2)
      clientRef.set(client)
      val task = new UpdateFileTask(client)
      val taskProcess = Future {
        try {
          task.process
        } catch {
          case e: Throwable => e.printStackTrace
        }
      }(executionContext)
      while (!taskProcess.isCompleted) {
        Platform.runLater {
          processBar.progress = task.progress
        }
        Thread.sleep(50)
      }
      Platform.runLater {
        processBar.progress = -1.0
      }
      client.stop
      true
    }(executionContext).onComplete {
      case Success(suc) => {
        println("Updated.")
        println(Files.exists(localRoot.resolve("sydra")))
        val hydraLocalJar = Files.list(localRoot.resolve("sydra")).iterator().asScala.filter(p =>
          p.getFileName.toString.toLowerCase.startsWith("hydralocal")
            && p.getFileName.toString.toLowerCase.endsWith(".jar") && Files.isRegularFile(p)).toList.headOption
        hydraLocalJar match {
          case Some(jar) => {
            println(jar)
            Runtime.getRuntime.exec(s"java -jar ${jar.getFileName.toString}", null, jar.getParent.toFile)
          }
          case None => println("No launchable JAR.")
        }
        Platform.runLater {
          Platform.exit()
        }
      }
      case Failure(fail) => shakeAnimation()
    }(executionContext)
  }

  private def shakeAnimation() = {
    processBar.visible = false
    buttonLaunch.disable = false
    textFieldHost.editable = true

    val currentX = buttonLaunch.layoutX.value
    val range = 16
    val duration = 240.0
    val period = 10
    val startTime = System.nanoTime / 1000000

    def calculatePosition(time: Long) = {
      val deltaTime = time - startTime
      val phi = (deltaTime / duration) * math.Pi * 4
      val p = math.sin(phi)
      range * p
    }

    val timer = new Timer("Move Animation Timer", true)
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        val currentTime = System.nanoTime / 1000000
        if (currentTime > startTime + duration) {
          cancel()
          AnchorPane.setLeftAnchor(buttonLaunch, 175.0)
          AnchorPane.setRightAnchor(buttonLaunch, 175.0)
        }
        val x = calculatePosition(currentTime)
        Platform.runLater {
          AnchorPane.setLeftAnchor(buttonLaunch, 175.0 + x)
          AnchorPane.setRightAnchor(buttonLaunch, 175.0 - x)
        }
      }
    }, 0, period)
  }

  class UpdateFileTask(client: MessageClient) {
    val totalWork = new AtomicLong(0)
    val doneWork = new AtomicLong(0)

    val invoker = client.blockingInvoker(target = "StorageService")
    val remoteRoot = Paths.get("/apps/hydralocal/release/")

    def process = {
      val remoteMap = new mutable.HashMap[String, FileEntry]()

      def fetchRemoteFileMetaData(path: Path): Unit = {
        val mds = invoker.listElements("", path.toString, true)
        mds.asInstanceOf[List[Map[String, Any]]].foreach(md => {
          md("Type") match {
            case "Content" => {
              val relativePath = remoteRoot.relativize(Paths.get(md("Path").toString))
              remoteMap(relativePath.toString) =
                new FileEntry(relativePath, md("LastModifiedTime").toString.toLong, md("Size").toString.toLong)
            }
            case "Collection" => fetchRemoteFileMetaData(Paths.get(md("Path").toString))
          }
        })
      }

      fetchRemoteFileMetaData(remoteRoot)

      val remotePaths = remoteMap.keys.toList
      val localPaths = Files.walk(localRoot, FileVisitOption.FOLLOW_LINKS).iterator.asScala.toList
        .filter(p => Files.isRegularFile(p)).map(p => localRoot.relativize(p).toString)
      localPaths.filterNot(remotePaths.contains).foreach(p => Files.delete(localRoot.resolve(p)))

      val todoList = ArrayBuffer[FileEntry]()
      remoteMap.values.toList.foreach(remoteEntry => {
        if (!localPaths.contains(remoteEntry.path.toString) ||
          Files.isDirectory(localRoot.resolve(remoteEntry.path)) ||
          remoteEntry.lastModified > Files.getLastModifiedTime(localRoot.resolve(remoteEntry.path), LinkOption.NOFOLLOW_LINKS).toMillis) {
          todoList += remoteEntry
        }
      })

      totalWork.set(todoList.map(todo => todo.size).sum)

      todoList.foreach(download)
    }

    def progress = totalWork.get match {
      case 0 => -1.0
      case _ => doneWork.get.toDouble / totalWork.get
    }

    private val blockSize = 1000000;

    private def download(entry: FileEntry) = {
      val localPath = localRoot.resolve(entry.path)
      val remotePath = remoteRoot.resolve(entry.path)
      if (Files.exists(localPath)) delete(localPath)
      Files.createDirectories(localPath.getParent)
      Files.createFile(localPath)

      val channel = Files.newByteChannel(localPath, StandardOpenOption.WRITE)
      val blocks = Math.ceil(entry.size.toDouble / blockSize).toInt
      Range(0, blocks).foreach(block => {
        val start = block * blockSize
        val length = math.min(blockSize, entry.size - start)
        val data = invoker.read("", remotePath.toString, start, length).asInstanceOf[Array[Byte]]
        channel.write(ByteBuffer.wrap(data))
        doneWork.addAndGet(length)
      })
      channel.close
      Files.setLastModifiedTime(localPath, FileTime.fromMillis(entry.lastModified))
    }

    private def delete(path: Path): Unit = {
      if (Files.isDirectory(path)) Files.list(path).iterator.asScala.foreach(delete)
      Files.delete(path)
    }
  }

  case class FileEntry(val path: Path, val lastModified: Long, val size: Long)

  private def parseHost(text: String) = {
    val split = text.trim.split(":")
    val server = split(0)
    val port = split.size match {
      case 1 => 20102
      case _ => split(1).toInt
    }
    (server, port)
  }

  val loadFuture = Future {
    MessageClient.newClient("localhost", 12345, "Class Loader")
  }(executionContext)
}