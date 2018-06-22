package com.hydra.web

import java.io.{File, FileInputStream, FileNotFoundException, IOException}
import java.util.Properties
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.hydra.hap.SydraAppHandler
import com.hydra.io.MessageClient
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.handler.{HandlerList, ResourceHandler}
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}

import scala.concurrent.Future
import scala.io.Source

import scala.concurrent.ExecutionContext.Implicits.global

object WydraApp extends App {
  val Configuration = {
    val properties = new Properties
    val propertiesFile = new File("Wydra.properties")
    try {
      val propertiesIn = new FileInputStream(propertiesFile)
      properties.load(propertiesIn);
      propertiesIn.close
    } catch {
      case ex: FileNotFoundException => {
        println("Properties file not found...")
        System.exit(11)
      }
      case ex: IOException => {
        println("Exception in reading properties file.")
        System.exit(12)
      }
    }
    System.setProperty("log4j.configurationFile", properties.getProperty("log4j.configurationFile", "./config/log4j.xml"))
    properties
  }
  val debug = Configuration.getProperty("debug", "true").toBoolean
  val serverAddress = Configuration.getProperty("messageserver.address", "localhost")
  val serverPort = try {
    Configuration.getProperty("messageserver.port").toInt
  } catch {
    case e: Throwable => 20102
  }
  val clientName = Configuration.getProperty("clientName", "Wydra")
  val client = MessageClient.newClient(serverAddress, serverPort, clientName, new SydraAppHandler(clientName, "Wydra.md") {
    override def getSummary() = {
      (<html>
        <h1>
          {clientName}
        </h1>
        <p></p>
        <p>running...</p>
      </html>).toString
    }
  })

  val webPort = try {
    Configuration.getProperty("webserver.port").toInt
  } catch {
    case e: Throwable => 20080
  }
  val webServer = new Server(webPort)

  val servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS)
  servletContext.addServlet(new ServletHolder(new ClientDocumentServlet()), ClientDocumentServlet.path)
  servletContext.addServlet(new ServletHolder(new MsgPackRequestServlet()), MsgPackRequestServlet.path)
  //  servletContext.addServlet(new ServletHolder(new MD2HTMLServlet()), "/")
  servletContext.setContextPath("/wydra")

  val fileContext = new ResourceHandler()
  fileContext.setResourceBase("res")
  fileContext.setDirectoriesListed(debug)
  fileContext.setStylesheet("")
  println(fileContext.setRedirectWelcome(true))
  fileContext.setWelcomeFiles(Array[String]("index.html"))

  val handlers = new HandlerList()
  handlers.setHandlers(Array[Handler](servletContext, fileContext))
  webServer.setHandler(handlers)
  webServer.start
  println(s"Wydra WebServer started on port $webPort.")

  val latch = new CountDownLatch(1)
  Future {
    Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
    latch.countDown
  }
  latch.await(12, TimeUnit.HOURS)
  println("Stoping Wydra WebServer...")
  webServer.stop
  client.stop
}
