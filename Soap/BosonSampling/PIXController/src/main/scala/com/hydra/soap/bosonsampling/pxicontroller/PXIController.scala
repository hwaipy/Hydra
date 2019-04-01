package com.hydra.soap.bosonsampling.pxicontroller

import java.io._
import java.util.Properties

import com.hydra.io.MessageClient
import gnu.io.CommPortIdentifier

import language.postfixOps
import scala.io.Source

object PXIController extends App {
  val properties = new Properties
  val propertiesIn = new FileInputStream(new File("PIX.properties"))
  properties.load(propertiesIn)
  propertiesIn.close()

  val pix = new PIX(properties.getProperty("pix.port.name"), properties.getProperty("pxi.port.baudrate").toInt)
  val client = MessageClient.newClient("192.168.25.27", 20102, "PIX", pix)

  println("PIX Service online.")
  Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
  println("Stoping PIX Service...")
  client.stop
}

class PIX(port: String, baudrate: Int) {
  val ss = System.getProperty("os.name") match {
    case "Mac OS X" => None
    case _ => {
      val portIdentifier = CommPortIdentifier.getPortIdentifier(port)
      val sp = portIdentifier.open("JAVA-RTXT-PIX", 3000)
      val outputWriter = new PrintWriter(sp.getOutputStream())
      outputWriter.print("ke\r")
      outputWriter.flush()
      Some(outputWriter)
    }
  }

  def setPosition(x: Double) {
    ss match {
      case None =>
      case Some(outputWriter) => {
        val position = (x * 0.81877).toLong
        outputWriter.print("set, 0, " + position + "\r")
        println("set, 0, " + position + "\r")
        outputWriter.flush()
      }
    }
  }
}