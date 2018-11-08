package com.hydra.services.polarizationcontrol

import com.hydra.io.MessageClient
import java.util.Properties
import java.io.IOException
import java.io.FileNotFoundException
import java.io.File
import java.io.FileInputStream

import com.hwaipy.science.polarizationcontrol.approach.annealing.AnnealingTest
import com.hydra.hap.SydraAppHandler

import scala.io.Source

object PolarizationControl extends App {
  lazy val Configuration = {
    val properties = new Properties
    val propertiesFile = new File("PolarizationControl.properties")
    try {
      val propertiesIn = new FileInputStream(propertiesFile)
      properties.load(propertiesIn)
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
  lazy val serverAddress = Configuration.getProperty("messageserver.address", "localhost")
  lazy val serverPort = try {
    Configuration.getProperty("messageserver.port").toInt
  } catch {
    case e: Throwable => 20102
  }

  lazy val clientName = Configuration.getProperty("clientName", "PolarizationControlService")
  lazy val polarizationControlService = new PolarizationControl(clientName, "PolarizationControl.md")
  val client = MessageClient.newClient(serverAddress, serverPort, clientName, polarizationControlService)

  println("Polarization Control Service online.")
  Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
  println("Polarization Control Storage Service...")
  client.stop
}

class PolarizationControl(clientName: String, descriptionFile: String) extends SydraAppHandler(clientName, descriptionFile) {
  override def getSummary() = {
    (<html>
      <h1>
        {clientName}
      </h1>
      <p></p>
      <p>running...</p>
    </html>).toString
  }

  def M1Calculate(azimuth: Double, altitude: Double, phase1: Double, phase2: Double, phase3: Double) = {
    val s = new M1Simulation()
    val result = s.calculate(azimuth, altitude, 0.0, 0.0, false, false, 0.0 / 180 * Math.PI, phase1, phase2, phase3)
    result.angles
  }

  def annealingSerach(startPoint: List[Double], reference: List[Double], r: Double = 0.9999, jC: Double = 4048) = {
    val p = new AnnealingTest(r, jC, startPoint.toArray, reference.toArray)
    val result = p.process
    val newAngles = result.angles
    if (!result.success) throw new RuntimeException("Failed!")
    newAngles
  }
}
