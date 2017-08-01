package com.hydra.storage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.LinkOption;
import org.scalatest._
import PermissionLevel._
import PermissionDecision._
import ElementType._
import scala.collection.mutable.ArrayBuffer

class StorageServiceTest extends FunSuite with BeforeAndAfter with BeforeAndAfterAll {
  val testSpace = Paths.get("target/testservicespace/")

  override def beforeAll() {
    if (Files.exists(testSpace, LinkOption.NOFOLLOW_LINKS)) clearPath(testSpace)
    Files.createDirectories(testSpace)
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
    val service = new StorageService(testSpace)
    val a = service.listElements("", "/")
    assert(a == List("_A1", "_A2", "a1", "a2", "a3", "a4", "a5"))
  }

  test("Test metaData") {
    val service = new StorageService(testSpace)
    assert(service.metaData("", "/a1") == Map("Name" -> "a1", "Path" -> "/a1", "Type" -> "Collection"))
    assert(service.metaData("", "/_A1") == Map("Name" -> "_A1", "Path" -> "/_A1", "Type" -> "Content", "Size" -> 36))
    assert(service.metaData("", "/_A2") == Map("Name" -> "_A2", "Path" -> "/_A2", "Type" -> "Content", "Size" -> 256))
    assert(service.metaData("", "/_") == Map("Name" -> "_", "Path" -> "/_", "Type" -> "NotExist"))
  }

  test("Test listMetaData") {
    val service = new StorageService(testSpace)
    val e = service.listElements("", "/", true)
  }

  test("Test read") {
    val service = new StorageService(testSpace)
    assert(new String(service.read("", "/_A1", 1, 10)) == "234567890a")
    assert(new String(service.read("", "/_A1", 30, 6)) == "uvwxyz")
    intercept[IOException] {
      service.read("", "/_A1", 30, 7)
    }
    assert(service.read("", "/_A2", 100, 6)(2) == 102)
  }

  test("Test append") {
    val service = new StorageService(testSpace)
    service.append("", "/_A1", new String("ABCDE").getBytes)
    assert(service.metaData("", "/_A1") == Map("Name" -> "_A1", "Path" -> "/_A1", "Type" -> "Content", "Size" -> 41))
    assert(new String(service.read("", "/_A1", 35, 6)) == "zABCDE")
  }

  test("Test write") {
    val service = new StorageService(testSpace)
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
    val service = new StorageService(testSpace)
    assert(service.listElements("", "/") == List("_A1", "_A2", "a1", "a2", "a3", "a4", "a5"))
    service.delete("", "a1")
    assert(service.listElements("", "/") == List("_A1", "_A2", "a2", "a3", "a4", "a5"))
    service.delete("", "_A2")
    assert(service.listElements("", "/") == List("_A1", "a2", "a3", "a4", "a5"))
  }

  test("Test createFile") {
    val service = new StorageService(testSpace)
    service.createFile("", "/NewFile")
    service.createFile("", "/a2/NewFile")
    assert(service.listElements("", "/") == List("_A1", "a2", "a3", "a4", "a5", "NewFile"))
    assert(service.listElements("", "/a2") == List("NewFile"))
  }

  test("Test createDirectory") {
    val service = new StorageService(testSpace)
    service.createDirectory("", "/a2/NewDir")
    assert(service.listElements("", "/a2") == List("NewDir", "NewFile"))
  }

  after {
  }

  private def clearPath(path: Path) {
    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
      val it = Files.list(path).iterator
      while (it.hasNext) {
        clearPath(it.next)
      }
    }
    Files.delete(path)
  }
}

