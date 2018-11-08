package com.hydra.services.storage

import java.io.{File, IOException}
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.LinkOption
import org.scalatest._
import util.Random

class StorageServiceTest extends FunSuite with BeforeAndAfter with BeforeAndAfterAll {
  val testSpace = Paths.get("target/testservicespace/")

  override def beforeAll() {
    if (Files.exists(testSpace, LinkOption.NOFOLLOW_LINKS)) clearPath(testSpace.toFile)
    testSpace.toFile.mkdirs
    val storage = new Storage(testSpace)
    storage.getStorageElement("/a1").createDirectories
    storage.getStorageElement("/a2").createDirectories
    storage.getStorageElement("/a3").createDirectories
    storage.getStorageElement("/a4").createDirectories
    storage.getStorageElement("/a5").createDirectories
    storage.getStorageElement("/_A1").createFile
    storage.getStorageElement("/_A2").createFile
    Files.write(testSpace.resolve("_A1"), new String("1234567890abcdefghijklmnopqrstuvwxyz").getBytes)
    Files.write(testSpace.resolve("_A2"), new Array[Byte](256).zipWithIndex.map(i => i._2.toByte).toArray)
  }

  override def afterAll() {
  }

  before {
  }

  test("Test list") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    val a = service.listElements("", "/")
    assert(a == List("_A1", "_A2", "a1", "a2", "a3", "a4", "a5"))
  }

  test("Test metaData") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    assert(service.metaData("", "/a1") == Map("Name" -> "a1", "Path" -> "/a1", "Type" -> "Collection"))
    assert(service.metaData("", "/_A1") == Map("Name" -> "_A1", "Path" -> "/_A1", "Type" -> "Content", "Size" -> 36))
    assert(service.metaData("", "/_A2") == Map("Name" -> "_A2", "Path" -> "/_A2", "Type" -> "Content", "Size" -> 256))
    assert(service.metaData("", "/_") == Map("Name" -> "_", "Path" -> "/_", "Type" -> "NotExist"))
  }

  test("Test listMetaData") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    val listElements = service.listElements("", "/", true)
    val elements = listElements.asInstanceOf[List[Map[String, _]]]
    val expected = List(
      List("_A1", "/_A1", "Content"),
      List("_A2", "/_A2", "Content"),
      List("a1", "/a1", "Collection"),
      List("a2", "/a2", "Collection"),
      List("a3", "/a3", "Collection"),
      List("a4", "/a4", "Collection"),
      List("a5", "/a5", "Collection"))
    assert(elements.size == expected.size)
    expected.zip(elements).foreach(z => {
      val exp = z._1
      val res = z._2.asInstanceOf[Map[String, _]]
      assert(List("Name", "Path", "Type").map(key => res(key)) == exp)
    })
    assert(elements(0)("Size") == 36)
    assert(elements(1)("Size") == 256)
  }

  test("Test note") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    assert(service.readNote("", "/")("Note") == "")
    service.writeNote("", "/", "Test Note")
    assert(service.readNote("", "/")("Note") == "Test Note")
    service.writeNote("", "/", "Test N")
    assert(service.readNote("", "/")("Note") == "Test N")
  }

  test("Test read") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    assert(new String(service.read("", "/_A1", 1, 10)) == "234567890a")
    assert(new String(service.read("", "/_A1", 30, 6)) == "uvwxyz")
    intercept[IOException] {
      service.read("", "/_A1", 30, 7)
    }
    assert(service.read("", "/_A2", 100, 6)(2) == 102)
  }

  test("Test append") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    service.append("", "/_A1", new String("ABCDE").getBytes)
    assert(service.metaData("", "/_A1") == Map("Name" -> "_A1", "Path" -> "/_A1", "Type" -> "Content", "Size" -> 41))
    assert(new String(service.read("", "/_A1", 35, 6)) == "zABCDE")
  }

  test("Test write") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    service.write("", "/_A1", new String("ABCDE").getBytes, 10)
    assert(service.metaData("", "/_A1") == Map("Name" -> "_A1", "Path" -> "/_A1", "Type" -> "Content", "Size" -> 41))
    assert(new String(service.read("", "/_A1", 35, 6)) == "zABCDE")
    assert(new String(service.read("", "/_A1", 0, 16)) == "1234567890ABCDEf")
    service.write("", "/_A1", new String("defghi").getBytes, 39)
    assert(service.metaData("", "/_A1") == Map("Name" -> "_A1", "Path" -> "/_A1", "Type" -> "Content", "Size" -> 45))
    assert(new String(service.read("", "/_A1", 35, 10)) == "zABCdefghi")
    assert(new String(service.read("", "/_A1", 0, 16)) == "1234567890ABCDEf")
  }

  test("Test delete") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    assert(service.listElements("", "/") == List("_A1", "_A2", "a1", "a2", "a3", "a4", "a5"))
    service.delete("", "a1")
    assert(service.listElements("", "/") == List("_A1", "_A2", "a2", "a3", "a4", "a5"))
    service.delete("", "_A2")
    assert(service.listElements("", "/") == List("_A1", "a2", "a3", "a4", "a5"))
  }

  test("Test createFile") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    service.createFile("", "/NewFile")
    service.createFile("", "/a2/NewFile")
    assert(service.listElements("", "/") == List("NewFile", "_A1", "a2", "a3", "a4", "a5"))
    assert(service.listElements("", "/a2") == List("NewFile"))
  }

  test("Test createDirectory") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    service.createDirectory("", "/a2/NewDir")
    assert(service.listElements("", "/a2") == List("NewDir", "NewFile"))
  }

  test("Test HBTFile.") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    service.HBTFileInitialize("", "/HBTFileTest.hbt", List("Column 1", "Byte") :: List("Column 2", "Short") ::
      List("Column 3", "Int") :: List("Column 4", "Long") :: List("Column 5", "Float") :: List("Column 6", "Double") :: Nil)
    service.HBTFileAppendRow("", "/HBTFileTest.hbt", List(1, 2, 3, 4, 5, 6))
    service.HBTFileAppendRows("", "/HBTFileTest.hbt", List(1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte, 6.toByte) ::
      List(1.toShort, 2.toShort, 3.toShort, 4.toShort, 5.toShort, 6.toShort) :: List(1l, 2l, 3l, 4l, 5l, 6l) ::
      List(1.1f, 2.2f, 3.3f, 4.4f, 5.5f, 6.6f) :: List(1.1, 2.2, 3.3, 4.4, 5.5, 6.6) :: Nil)
    val readRows = service.HBTFileReadAllRows("", "/HBTFileTest.hbt")
    assert(readRows(0) == List(1, 2, 3, 4, 5, 6))
    assert(readRows(1) == List(1, 2, 3, 4, 5, 6))
    assert(readRows(2) == List(1, 2, 3, 4, 5, 6))
    assert(readRows(3) == List(1, 2, 3, 4, 5, 6))
    assert(readRows(4) == List(1, 2, 3, 4, 5.5, 6.6.toFloat))
    assert(readRows(5) == List(1, 2, 3, 4, 5.5, 6.6))
    val metaExpect = Map("ColumnCount" -> 6, "RowDataLength" -> 27, "RowCount" -> 6,
      "Heads" -> List(List("Column 1", "Byte"), List("Column 2", "Short"), List("Column 3", "Int"), List("Column 4", "Long"), List("Column 5", "Float"), List("Column 6", "Double")))
    assert(service.HBTFileMetaData("", "/HBTFileTest.hbt") == metaExpect)
  }


  test("Test FSFile.") {
    val service = new StorageService(testSpace, "Test-StorageService", "")
    service.FSFileInitialize("", "/FSFileTest.fs")
    val random = new Random()
    val frames = Range(0, 100).toList.map(i => {
      val size = random.nextInt(10) + 100
      val frame = new Array[Byte](size)
      random.nextBytes(frame)
      frame
    })
    service.FSFileAppendFrame("", "/FSFileTest.fs", frames(0))
    service.FSFileAppendFrames("", "/FSFileTest.fs", frames)
    val firstFrame = service.FSFileReadHeadFrames("", "/FSFileTest.fs", 0, 1)
    assert(firstFrame(0).toList == frames(0).toList)
    val headFrames = service.FSFileReadHeadFrames("", "/FSFileTest.fs", 10, 40)
    headFrames.zip((List(frames(0)) ::: frames).slice(10, 50)).foreach(z => assert(z._1.toList == z._2.toList))
    val tailFrames = service.FSFileReadTailFrames("", "/FSFileTest.fs", 4, 30)
    tailFrames.zip((List(frames(0)) ::: frames).reverse.slice(4, 34)).foreach(z => assert(z._1.toList == z._2.toList))
    val allFrames = service.FSFileReadAllFrames("", "/FSFileTest.fs")
    allFrames.zip((List(frames(0)) ::: frames)).foreach(z => assert(z._1.toList == z._2.toList))
  }

  after {
  }

  private def clearPath(file: File) {
    if (file.isDirectory) {
      val it = file.listFiles.iterator
      while (it.hasNext) {
        clearPath(it.next)
      }
    }
    file.delete
  }
}

