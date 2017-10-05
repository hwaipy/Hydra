package com.hydra.storage.fits

import org.scalatest._
import nom.tam.fits._
import nom.tam.util.BufferedFile
import nom.tam.fits.header.InstrumentDescription.FILTER;

class FitsTest extends FunSuite with BeforeAndAfter with BeforeAndAfterAll {
  test("Fits IO Images") {
    //    val data = Array.ofDim[Double](512, 512)
    //    for (x <- Range(0, 512); y <- Range(0, 512)) {
    //      data(x)(y) = x + 0.001 * y
    //    }
    //    val f = new Fits()
    //    f.addHDU(FitsFactory.hduFactory(data))
    //    val bf = new BufferedFile("1.image-512X512-Double.fits", "rw")
    //    f.write(bf)
    //    bf.close
    //
    //    val fr = new Fits("1.image-512X512-Double.fits")
    //    val hdu = fr.getHDU(0).asInstanceOf[ImageHDU]
    //    val image = hdu.getKernel
    //    println(hdu.getAxes.toList)
    //    println(hdu.getHeader)
  }

  test("Fits IO Tables") {
    FitsFactory.setUseAsciiTables(false)
    val numRows = 10000000
    val x = new Array[Double](numRows)
    val y = new Array[Double](numRows)
    for (i <- Range(0, numRows)) {
      x(i) = i * 11
      y(i) = i * 7
    }
    val data = Array[Any](x, y)
    val f1 = new Fits()
    f1.addHDU(FitsFactory.hduFactory(data))
    val bf1 = new BufferedFile("2.table-row2matrix.fits", "rw")
    f1.write(bf1)
    bf1.close

    //    val bt = new BinaryTable()
    //    bt.addColumn(x)
    //    bt.addColumn(y)
    //    val bhdu = new BinaryTableHDU(null, bt)
    //    //    bhdu.addColumn(x)
    //    //    bhdu.addColumn(y)
    //    val f2 = new Fits()
    //    f2.addHDU(bhdu)


    //    val bf2 = new BufferedFile("2.table-row2matrixB.fits", "rw")
    //    f2.write(bf2)
    //    bf2.close
  }

  //  val testSpace = Paths.get("target/testservicespace/")
  //
  //  override def beforeAll() {
  //    if (Files.exists(testSpace, LinkOption.NOFOLLOW_LINKS)) clearPath(testSpace)
  //    Files.createDirectories(testSpace)
  //    val storage = new Storage(testSpace)
  //    storage.getStorageElement("/a1").createDirectories
  //    storage.getStorageElement("/a2").createDirectories
  //    storage.getStorageElement("/a3").createDirectories
  //    storage.getStorageElement("/a4").createDirectories
  //    storage.getStorageElement("/a5").createDirectories
  //    storage.getStorageElement("/_A1").createFile
  //    storage.getStorageElement("/_A2").createFile
  //    Files.write(testSpace.resolve("_A1"), new String("1234567890abcdefghijklmnopqrstuvwxyz").getBytes)
  //    Files.write(testSpace.resolve("_A2"), new Array[Byte](256).zipWithIndex.map(i => i._2.toByte).toArray)
  //  }
  //
  //  override def afterAll() {
  //  }
  //
  //  before {
  //  }
  //
  //  test("Test list") {
  //    val service = new StorageService(testSpace)
  //    val a = service.listElements("", "/")
  //    assert(a == List("_A1", "_A2", "a1", "a2", "a3", "a4", "a5"))
  //  }
  //
  //  test("Test metaData") {
  //    val service = new StorageService(testSpace)
  //    assert(service.metaData("", "/a1") == Map("Name" -> "a1", "Path" -> "/a1", "Type" -> "Collection"))
  //    assert(service.metaData("", "/_A1") == Map("Name" -> "_A1", "Path" -> "/_A1", "Type" -> "Content", "Size" -> 36))
  //    assert(service.metaData("", "/_A2") == Map("Name" -> "_A2", "Path" -> "/_A2", "Type" -> "Content", "Size" -> 256))
  //    assert(service.metaData("", "/_") == Map("Name" -> "_", "Path" -> "/_", "Type" -> "NotExist"))
  //  }
  //
  //  test("Test listMetaData") {
  //    val service = new StorageService(testSpace)
  //    val e = service.listElements("", "/", true)
  //    println(e)
  //  }
  //
  //  test("Test read") {
  //    val service = new StorageService(testSpace)
  //    assert(new String(service.read("", "/_A1", 1, 10)) == "234567890a")
  //    assert(new String(service.read("", "/_A1", 30, 6)) == "uvwxyz")
  //    intercept[IOException] {
  //      service.read("", "/_A1", 30, 7)
  //    }
  //    assert(service.read("", "/_A2", 100, 6)(2) == 102)
  //  }
  //
  //  test("Test append") {
  //    val service = new StorageService(testSpace)
  //    service.append("", "/_A1", new String("ABCDE").getBytes)
  //    assert(service.metaData("", "/_A1") == Map("Name" -> "_A1", "Path" -> "/_A1", "Type" -> "Content", "Size" -> 41))
  //    assert(new String(service.read("", "/_A1", 35, 6)) == "zABCDE")
  //  }
  //
  //  test("Test write") {
  //    val service = new StorageService(testSpace)
  //    service.write("", "/_A1", new String("ABCDE").getBytes, 10)
  //    assert(service.metaData("", "/_A1") == Map("Name" -> "_A1", "Path" -> "/_A1", "Type" -> "Content", "Size" -> 41))
  //    assert(new String(service.read("", "/_A1", 35, 6)) == "zABCDE")
  //    assert(new String(service.read("", "/_A1", 0, 16)) == "1234567890ABCDEf")
  //    service.write("", "/_A1", new String("defghi").getBytes, 39)
  //    assert(service.metaData("", "/_A1") == Map("Name" -> "_A1", "Path" -> "/_A1", "Type" -> "Content", "Size" -> 45))
  //    assert(new String(service.read("", "/_A1", 35, 10)) == "zABCdefghi")
  //    assert(new String(service.read("", "/_A1", 0, 16)) == "1234567890ABCDEf")
  //  }
  //
  //  test("Test delete") {
  //    val service = new StorageService(testSpace)
  //    assert(service.listElements("", "/") == List("_A1", "_A2", "a1", "a2", "a3", "a4", "a5"))
  //    service.delete("", "a1")
  //    assert(service.listElements("", "/") == List("_A1", "_A2", "a2", "a3", "a4", "a5"))
  //    service.delete("", "_A2")
  //    assert(service.listElements("", "/") == List("_A1", "a2", "a3", "a4", "a5"))
  //  }
  //
  //  test("Test createFile") {
  //    val service = new StorageService(testSpace)
  //    service.createFile("", "/NewFile")
  //    service.createFile("", "/a2/NewFile")
  //    assert(service.listElements("", "/") == List("_A1", "a2", "a3", "a4", "a5", "NewFile"))
  //    assert(service.listElements("", "/a2") == List("NewFile"))
  //  }
  //
  //  test("Test createDirectory") {
  //    val service = new StorageService(testSpace)
  //    service.createDirectory("", "/a2/NewDir")
  //    assert(service.listElements("", "/a2") == List("NewDir", "NewFile"))
  //  }
  //
  //  after {
  //  }
  //
  //  private def clearPath(path: Path) {
  //    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
  //      val it = Files.list(path).iterator
  //      while (it.hasNext) {
  //        clearPath(it.next)
  //      }
  //    }
  //    Files.delete(path)
  //  }
}

