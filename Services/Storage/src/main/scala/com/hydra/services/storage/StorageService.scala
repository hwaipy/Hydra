package com.hydra.storage

import java.nio.file.Path

import com.hydra.io.MessageClient
import java.nio.file.Paths
import java.util.Properties
import java.io.IOException
import java.io.FileNotFoundException
import java.io.File
import java.io.FileInputStream

import com.hydra.hap.SydraAppHandler

import scala.io.Source

object StorageService extends App {
  lazy val Configuration = {
    val properties = new Properties
    val propertiesFile = new File("Storage.properties")
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
  lazy val serverAddress = Configuration.getProperty("messageserver.address", "localhost")
  lazy val serverPort = try {
    Configuration.getProperty("messageserver.port").toInt
  } catch {
    case e: Throwable => 20102
  }
  lazy val storageSpace = Configuration.getProperty("storagespace", "target/storagespace")
  lazy val storageService = new StorageService(Paths.get(storageSpace))

  println(serverAddress)
  println(serverPort)

  lazy val clientName = Configuration.getProperty("clientName", "StorageService")
  val client = MessageClient.newClient(serverAddress, serverPort, clientName, storageService)

//  val client = MessageClient.newClient(serverAddress, serverPort, clientName, new SydraAppHandler(clientName, "doc.md") {
//    override def getSummary() = {
//      (<html>
//        <h1>
//          {clientName}
//        </h1>
//        <p></p>
//        <p>running...</p>
//      </html>).toString
//    }
//  })

  println("Storage Service online.")
  Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
  println("Stoping Storage Service...")
  client.stop
}

class StorageService(basePath: Path) {
  private val storage = new Storage(basePath)

  def listElements(user: String, path: String, withMetaData: Boolean = false) = {
    storage.updatePermission(new Permission(user))
    val elements = storage.getStorageElement(path).listElements
    storage.clearPermission
    if (withMetaData) {
      elements.map(e => e.metaDataMap(true))
    } else {
      elements.map(e => e.name)
    }
  }

  def metaData(user: String, path: String, withTime: Boolean = false) = {
    storage.updatePermission(new Permission(user))
    val element = storage.getStorageElement(path)
    storage.clearPermission
    element.metaDataMap(withTime)
  }

  def read(user: String, path: String, start: Long, length: Int) = {
    storage.updatePermission(new Permission(user))
    val data = storage.getStorageElement(path).read(start, length)
    storage.clearPermission
    data
  }

  def append(user: String, path: String, data: Array[Byte]) {
    storage.updatePermission(new Permission(user))
    storage.getStorageElement(path).append(data)
    storage.clearPermission
  }

  def write(user: String, path: String, data: Array[Byte], start: Long) {
    storage.updatePermission(new Permission(user))
    storage.getStorageElement(path).write(data, start)
    storage.clearPermission
  }

  def clear(user: String, path: String) {
    storage.updatePermission(new Permission(user))
    storage.getStorageElement(path).clear
    storage.clearPermission
  }

  def delete(user: String, path: String) {
    storage.updatePermission(new Permission(user))
    storage.getStorageElement(path).delete
    storage.clearPermission
  }

  def createFile(user: String, path: String) {
    storage.updatePermission(new Permission(user))
    storage.getStorageElement(path).createFile
    storage.clearPermission
  }

  def createDirectory(user: String, path: String) {
    storage.updatePermission(new Permission(user))
    storage.getStorageElement(path).createDirectories
    storage.clearPermission
  }

  def exists(user: String, path: String) = {
    storage.updatePermission(new Permission(user))
    val e = storage.getStorageElement(path).exists
    storage.clearPermission
    e
  }
}
