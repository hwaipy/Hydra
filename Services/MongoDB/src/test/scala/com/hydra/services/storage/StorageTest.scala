package com.hydra.services.storage

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.LinkOption
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import org.mongodb.scala.model.changestream.ChangeStreamDocument
import org.scalatest._

class StorageTest extends FunSuite with BeforeAndAfter with BeforeAndAfterAll {
  //  val testSpace = Paths.get("target/testspace/")

  override def beforeAll() {
    //    if (Files.exists(testSpace, LinkOption.NOFOLLOW_LINKS)) clearPath(testSpace.toFile)
    //    testSpace.toFile.mkdirs()
  }

  override def afterAll() {
  }

  before {
  }

  test("Test MongoDB Basic") {
    import org.mongodb.scala._
    val mongoClient: MongoClient = MongoClient("mongodb://hydra:hydra@192.168.25.27:27019")
    val database = mongoClient.getDatabase("HydraTest")
    val collection = database.getCollection("TestCollection")

    collection.find().subscribe(new Observer[Document] {
          var i = new AtomicInteger(0)
          override def onNext(result: Document): Unit = {
            println(s"Result: $result-       ${i.getAndIncrement()}")
            println(result("_id").asObjectId().getValue.getTimestamp)
          }
          override def onError(e: Throwable): Unit = e.printStackTrace()
          override def onComplete(): Unit = println("Completed")
        })
//    val doc: Document = Document("name" -> "MongoDB", "type" -> "database", "count" -> 1, "info" -> Document("x" -> 203, "y" -> 102))
//    val operate = collection.insertOne(doc)
//    operate.subscribe(new Observer[Completed] {
//      override def onNext(result: Completed): Unit = println(s"Inserted: $result")
//      override def onError(e: Throwable): Unit = println("Failed")
//      override def onComplete(): Unit = println("Completed")
//    })
    Thread.sleep(2000)
    mongoClient.close()
  }
  //  test("Test PermissionLevel") {
  //    assert(getLevel(-1) == NoAccess)
  //    assert(getLevel(0) == NoAccess)
  //    assert(getLevel(1) == Read)
  //    assert(getLevel(2) == Append)
  //    assert(getLevel(3) == Modify)
  //    assert(getLevel(4) == PermissionRead)
  //    assert(getLevel(5) == PermisionAppend)
  //    assert(getLevel(6) == PermissionModify)
  //    assert(getLevel(7) == PermissionModify)
  //    assert(getLevel(8) == PermissionModify)
  //    assert(permission(NoAccess, Read) == Deny)
  //    assert(permission(Read, Read) == Accept)
  //    assert(permission(null, Read) == Undefined)
  //  }
  //
  //  test("Test Basic Operations") {
  //    val attrRoot = testSpace.resolve("..root")
  //    Files.write(attrRoot, new String("PERMISSION.DEFAULT: 100").getBytes)
  //    val storage = new Storage(testSpace)
  //    assert(storage.getStorageElement("/a").toString == "StorageElement[/a]")
  //    assert(storage.getStorageElement("/a").exists == false)
  //    storage.getStorageElement("/a").createDirectories
  //    assert(storage.getStorageElement("/a").exists == true)
  //    assert(storage.getStorageElement("/a").getType == Collection)
  //    val c = storage.getStorageElement("/a/b/c")
  //    assert(!c.exists)
  //    c.createDirectories
  //    assert(c.exists)
  //    assert(c.getType == Collection)
  //    val e = storage.getStorageElement("/a/b/c/d/e")
  //    assert(!e.exists)
  //    e.createFile
  //    assert(e.exists)
  //    assert(e.getType == Content)
  //    intercept[IOException] {
  //      c.createDirectories
  //    }
  //    intercept[IOException] {
  //      c.createFile
  //    }
  //    e.delete
  //    assert(!e.exists)
  //    val a = storage.getStorageElement("/a")
  //    assert(a.exists)
  //    a.delete
  //    assert(!a.exists)
  //  }
  //
  //  after {
  //  }
  //
  //  private def clearPath(file: File) {
  //    if (file.isDirectory) {
  //      val it = file.listFiles.iterator
  //      while (it.hasNext) {
  //        clearPath(it.next)
  //      }
  //    }
  //    file.delete
  //  }
}

