package com.hydra.services.tdc

import java.io.File
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.{ExecutionContext, Future}
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.Dimension2D
import scalafx.scene.Scene
import scalafx.scene.layout.AnchorPane
import scalafx.stage.Screen
import com.hydra.io.MessageClient

import scalafx.scene.control.{Label, TextField}

object HydraLocalLauncher extends JFXApp {
  val DEBUG = new File(".").getAbsolutePath.contains("GitHub")
  System.setProperty("log4j.configurationFile", "./config/tdcviewer.debug.log4j.xml")

  val visualBounds = Screen.primary.visualBounds
  val frameSize = new Dimension2D(visualBounds.width * 0.6, visualBounds.height * 0.6)
  //  val framePosition = new Point2D(
  //    visualBounds.getMinX + (visualBounds.getMaxX - visualBounds.getMinX - frameSize.width) / 2,
  //    visualBounds.getMinY + (visualBounds.getMaxY - visualBounds.getMinY - frameSize.height) / 2)
  //
  //  val clientRef = new AtomicReference[MessageClient]

  val counterFields = Range(0, 16).toList.map(i => new TextField())
  val counterLabels = Range(0, 16).toList.map(i => new Label())
  val counterPane = new AnchorPane()

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
    //    x = framePosition.x
    //    y = framePosition.y
    resizable = true
    scene = new Scene {
      root = new AnchorPane {
        //CounterLabels
        counterLabels.zipWithIndex.foreach(z => {
          AnchorPane.setTopAnchor(z._1, 40.0 * z._2)
          AnchorPane.setLeftAnchor(z._1, 0.0)
          //          AnchorPane.setRightAnchor(z._1, 0.0)
          z._1.prefHeight = 35
          z._1.prefWidth = 50
          z._1.focusTraversable = false
          z._1.text = "CH X"
        })

        //CounterFields
        counterFields.zipWithIndex.foreach(z => {
          AnchorPane.setTopAnchor(z._1, 40.0 * z._2)
          AnchorPane.setLeftAnchor(z._1, 55.0)
          z._1.prefHeight = 35
          z._1.prefWidth = 60
          z._1.editable = false
          z._1.focusTraversable = false
          z._1.text = "----"
        })

        //CounterPane
        counterPane.children = counterFields ::: counterLabels
        AnchorPane.setTopAnchor(counterPane, 0.0)
        AnchorPane.setLeftAnchor(counterPane, 0.0)
        //        counterPane.prefWidth = 100

        children = Seq(counterPane)
        prefWidth = frameSize.width
        prefHeight = frameSize.height


        //        counterPane.style = "-fx-background-color: rgb(255, 0, 0)"


        //        AnchorPane.setTopAnchor(textFieldHost, 40.0)
        //        AnchorPane.setLeftAnchor(textFieldHost, 40.0)
        //        AnchorPane.setBottomAnchor(textFieldHost, 200.0)
        //        AnchorPane.setRightAnchor(textFieldHost, 40.0)
        //        AnchorPane.setTopAnchor(buttonLaunch, 145.0)
        //        AnchorPane.setLeftAnchor(buttonLaunch, 175.0)
        //        AnchorPane.setBottomAnchor(buttonLaunch, 105.0)
        //        AnchorPane.setRightAnchor(buttonLaunch, 175.0)
        //        AnchorPane.setTopAnchor(processBar, 235.0)
        //        AnchorPane.setLeftAnchor(processBar, 40.0)
        //        AnchorPane.setBottomAnchor(processBar, 50.0)
        //        AnchorPane.setRightAnchor(processBar, 40.0)
        //        children = Seq(processBar, buttonLaunch, textFieldHost)
      }
    }
    //    onCloseRequest = (window) => {
    //      clientRef.get match {
    //        case c if c != null => c.stop
    //        case _ =>
    //      }
    //    }
  }

  val executionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor(new ThreadFactory {
    val counter = new AtomicInteger(0)

    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r, s"CachedThreadPool-[HydraLocalLauncher]-${counter.getAndIncrement}")
      t.setDaemon(true)
      t
    }
  }))
  Future {
    val client = MessageClient.newClient(parameters.named.get("host") match {
      case Some(host) => host
      case None => "localhost"
    }, parameters.named.get("port") match {
      case Some(port) => port.toInt
      case None => 20102
    })
    Platform.runLater(() => stage.onCloseRequest = (we) => client.stop)
    val invoker = client.blockingInvoker("GroundTDCServer")
    //    invoker.turnOffAllAnalysers()
    //    invoker.turnOnAnalyser("Counter")
    while (true) {
      try {
        val counterResult = invoker.fetchResults("Counter")
        println(counterResult)
      } catch {
        case e: Throwable => println(e)
      }
      Thread.sleep(400)
    }
  }(executionContext)

  //  private def shakeAnimation() = {
  //    processBar.visible = false
  //    buttonLaunch.disable = false
  //    textFieldHost.editable = true
  //
  //    val currentX = buttonLaunch.layoutX.value
  //    val range = 16
  //    val duration = 240.0
  //    val period = 10
  //    val startTime = System.nanoTime / 1000000
  //
  //    def calculatePosition(time: Long) = {
  //      val deltaTime = time - startTime
  //      val phi = (deltaTime / duration) * math.Pi * 4
  //      val p = math.sin(phi)
  //      range * p
  //    }
  //
  //    val timer = new Timer("Move Animation Timer", true)
  //    timer.schedule(new TimerTask {
  //      override def run(): Unit = {
  //        val currentTime = System.nanoTime / 1000000
  //        if (currentTime > startTime + duration) {
  //          cancel()
  //          AnchorPane.setLeftAnchor(buttonLaunch, 175.0)
  //          AnchorPane.setRightAnchor(buttonLaunch, 175.0)
  //        }
  //        val x = calculatePosition(currentTime)
  //        Platform.runLater {
  //          AnchorPane.setLeftAnchor(buttonLaunch, 175.0 + x)
  //          AnchorPane.setRightAnchor(buttonLaunch, 175.0 - x)
  //        }
  //      }
  //    }, 0, period)
  //  }
  //
  //  class UpdateFileTask(client: MessageClient) {
  //    val totalWork = new AtomicLong(0)
  //    val doneWork = new AtomicLong(0)
  //
  //    val invoker = client.blockingInvoker(target = "StorageService")
  //    val remoteRoot = Paths.get("/apps/hydralocal/")
  //
  //    def process = {
  //      val remoteMap = new mutable.HashMap[String, FileEntry]()
  //
  //      def fetchRemoteFileMetaData(path: Path): Unit = {
  //        val mds = invoker.listElements("", path.toString, true)
  //        mds.asInstanceOf[List[Map[String, Any]]].foreach(md => {
  //          md("Type") match {
  //            case "Content" => {
  //              val relativePath = remoteRoot.relativize(Paths.get(md("Path").toString))
  //              remoteMap(relativePath.toString) =
  //                new FileEntry(relativePath, md("LastModifiedTime").toString.toLong, md("Size").toString.toLong)
  //            }
  //            case "Collection" => fetchRemoteFileMetaData(Paths.get(md("Path").toString))
  //          }
  //        })
  //      }
  //
  //      fetchRemoteFileMetaData(remoteRoot)
  //
  //      val remotePaths = remoteMap.keys.toList
  //      val localPaths = Files.walk(localRoot, FileVisitOption.FOLLOW_LINKS).iterator.asScala.toList
  //        .filter(p => Files.isRegularFile(p)).map(p => localRoot.relativize(p).toString)
  //      localPaths.filterNot(remotePaths.contains).filterNot(p => p.startsWith(".")).foreach(p => Files.delete(localRoot.resolve(p)))
  //
  //      val todoList = ArrayBuffer[FileEntry]()
  //      remoteMap.values.toList.foreach(remoteEntry => {
  //        if (!localPaths.contains(remoteEntry.path.toString) ||
  //          Files.isDirectory(localRoot.resolve(remoteEntry.path)) ||
  //          remoteEntry.lastModified > Files.getLastModifiedTime(localRoot.resolve(remoteEntry.path), LinkOption.NOFOLLOW_LINKS).toMillis) {
  //          todoList += remoteEntry
  //        }
  //      })
  //
  //      totalWork.set(todoList.map(todo => todo.size).sum)
  //
  //      todoList.foreach(download)
  //    }
  //
  //    def progress = totalWork.get match {
  //      case 0 => -1.0
  //      case _ => doneWork.get.toDouble / totalWork.get
  //    }
  //
  //    private val blockSize = 1000000;
  //
  //    private def download(entry: FileEntry) = {
  //      val localPath = localRoot.resolve(entry.path)
  //      val remotePath = remoteRoot.resolve(entry.path)
  //      if (Files.exists(localPath)) delete(localPath)
  //      Files.createDirectories(localPath.getParent)
  //      Files.createFile(localPath)
  //
  //      val channel = Files.newByteChannel(localPath, StandardOpenOption.WRITE)
  //      val blocks = Math.ceil(entry.size.toDouble / blockSize).toInt
  //      Range(0, blocks).foreach(block => {
  //        val start = block * blockSize
  //        val length = math.min(blockSize, entry.size - start)
  //        val data = invoker.read("", remotePath.toString, start, length).asInstanceOf[Array[Byte]]
  //        channel.write(ByteBuffer.wrap(data))
  //        doneWork.addAndGet(length)
  //      })
  //      channel.close
  //      Files.setLastModifiedTime(localPath, FileTime.fromMillis(entry.lastModified))
  //    }
  //
  //    private def delete(path: Path): Unit = {
  //      if (Files.isDirectory(path)) Files.list(path).iterator.asScala.foreach(delete)
  //      Files.delete(path)
  //    }
  //  }
  //
  //  case class FileEntry(val path: Path, val lastModified: Long, val size: Long)
  //
  //  private def parseHost(text: String) = {
  //    val split = text.trim.split(":")
  //    val server = split(0)
  //    val port = split.size match {
  //      case 1 => 20102
  //      case _ => split(1).toInt
  //    }
  //    (server, port)
  //  }
  //
  //  val loadFuture = Future {
  //    MessageClient.newClient("localhost", 12345, "Class Loader")
  //  }(executionContext)
}