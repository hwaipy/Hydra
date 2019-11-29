package com.hydra.services.storage

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

  lazy val clientName = Configuration.getProperty("clientName", "StorageService")
  lazy val storageService = new StorageService(Paths.get(storageSpace), clientName, "Storage.md")
  val client = MessageClient.newClient(serverAddress, serverPort, clientName, storageService)

  println("Storage Service online.")
  Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
  println("Stoping Storage Service...")
  client.stop
}

class StorageService(basePath: Path, clientName: String, descriptionFile: String) extends SydraAppHandler(clientName, descriptionFile) {
  override def getSummary() = {
    (<html>
      <h1>
        {clientName}
      </h1>
      <p></p>
      <p>running...</p>
    </html>).toString
  }

  private val storage = new Storage(basePath)

  def listElements(user: String, path: String, withMetaData: Boolean = false) = {
    storage.updatePermission(new Permission(user))
    val elements = storage.getStorageElement(path).listElements.sortBy(se => se.name)
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

  def readAll(user: String, path: String) = {
    storage.updatePermission(new Permission(user))
    val data = storage.getStorageElement(path).readAll
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

  def readNote(user: String, path: String) = {
    storage.updatePermission(new Permission(user))
    val data = storage.getStorageElement(path).readNote
    storage.clearPermission
    Map("Note" -> data)
  }

  def writeNote(user: String, path: String, data: String) = {
    storage.updatePermission(new Permission(user))
    storage.getStorageElement(path).writeNote(data)
    storage.clearPermission
    true
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

  def getHipInformation(user: String, path: String) = {
    storage.updatePermission(new Permission(user))
    val info = storage.getHipInformation(path)
    storage.clearPermission
    info
  }

  def HBTFileInitialize(user: String, path: String, heads: List[List[String]]) = {
    storage.updatePermission(new Permission(user))
    storage.HBTFileInitialize(path, heads)
    storage.clearPermission
  }

  def HBTFileMetaData(user: String, path: String) = {
    storage.updatePermission(new Permission(user))
    val metaData = storage.HBTFileMetaData(path)
    storage.clearPermission
    metaData
  }

  def HBTFileAppendRows(user: String, path: String, rowsData: List[List[Any]]) = {
    storage.updatePermission(new Permission(user))
    storage.HBTFileAppendRows(path, rowsData)
    storage.clearPermission
  }

  def HBTFileAppendRow(user: String, path: String, rowData: List[Any]) = {
    HBTFileAppendRows(user, path, rowData :: Nil)
  }

  def HBTFileReadRows(user: String, path: String, from: Int, count: Int) = {
    storage.updatePermission(new Permission(user))
    val data = storage.HBTFileReadRows(path, from, count)
    storage.clearPermission
    data
  }

  def HBTFileReadAllRows(user: String, path: String) = {
    storage.updatePermission(new Permission(user))
    val data = storage.HBTFileReadAllRows(path)
    storage.clearPermission
    data
  }

  def FSFileInitialize(user: String, path: String) = {
    storage.updatePermission(new Permission(user))
    storage.FSFileInitialize(path)
    storage.clearPermission
  }

  def FSFileAppendFrames(user: String, path: String, frames: List[Array[Byte]]) = {
    storage.updatePermission(new Permission(user))
    storage.FSFileAppendFrames(path, frames)
    storage.clearPermission
  }

  def FSFileAppendFrame(user: String, path: String, frame: Array[Byte]) = {
    FSFileAppendFrames(user, path, frame :: Nil)
  }

  def FSFileReadHeadFrames(user: String, path: String, from: Int, count: Int) = {
    storage.updatePermission(new Permission(user))
    val data = storage.FSFileReadHeadFrames(path, from, count)
    storage.clearPermission
    data
  }

  def FSFileReadTailFrames(user: String, path: String, from: Int, count: Int) = {
    storage.updatePermission(new Permission(user))
    val data = storage.FSFileReadTailFrames(path, from, count)
    storage.clearPermission
    data
  }

  def FSFileReadTailFramesFrom(user: String, path: String, from: Int, count: Int, offset: Long) = {
    storage.updatePermission(new Permission(user))
    val data = storage.FSFileReadTailFrames(path, from, count, offset)
    storage.clearPermission
    data
  }

  def FSFileReadAllFrames(user: String, path: String) = {
    storage.updatePermission(new Permission(user))
    val data = storage.FSFileReadAllFrames(path)
    storage.clearPermission
    data
  }
}
