package com.hydra.core

import java.io.IOException
import java.math.BigInteger
import java.nio.{ByteBuffer, CharBuffer}
import java.nio.charset.Charset

import org.scalatest._

import scala.collection.mutable.ListBuffer
import scala.util.Random

class MessageJsonPackTest extends FunSuite with BeforeAndAfter {
  val mapIn = scala.collection.mutable.LinkedHashMap(
    "keyString" -> "val\nue1",
    "keyChineseString" -> "加油",
    "keyInt" -> 123,
    "keyLong" -> (Int.MaxValue.toLong + 100),
    "keyBigInteger" -> new BigInteger(s"${Long.MaxValue}").add(new BigInteger(s"${Long.MaxValue}")),
    "keyBooleanFalse" -> false,
    "KeyBooleanTrue" -> true,
    //    "keyByteArray" -> Array[Byte](1, 2, 2, 2, 2, 1, 1, 2, 2, 1, 1, 4, 5, 4, 4, -1),
    "keyIntArray" -> Array[Int](3, 526255, 1321, 4, -1),
    "keyDoubleArray" -> Array[Double](3.1, 526255.2, 1321.1, 4.2, -1),
    "keyNull" -> null,
    "keyLongMax" -> Long.MaxValue,
    "keyLongMin" -> Long.MinValue,
    "keyFloat" -> 1.242,
    "keyDouble" -> 1.242134234234234,
    "keyBigDouble" -> BigDecimal("1.242343492384798234789824234349238479823478982423434923847982347898242343492384798234789824234349238479823478982479324728374239842134234234234"),
    "keyIntLikeDouble" -> 1.0,
    "keyDouble2" -> -12.2323e-100,
    "keyDouble4" -> Double.MinPositiveValue,
    "keyBigInteger2" -> BigInteger.valueOf(Long.MaxValue).add(BigInteger.TEN),
    "keyUnit" -> Unit,
  )
  val map = Map("keyMap" -> mapIn)

  before {
  }

  test("Test Map pack and unpack") {
    val s = MessageJsonPack.pack(map)
    val generator = new MessageJsonGenerator()
    generator.feed(s)
    val c = generator.next.get.content
    assert(eq(c, map))
  }

  test("Test feed overflow") {
    val generator = new MessageJsonGenerator()
    val bs = generator.bufferSize
    val str = MessageJsonPack.packString(map)
    val bytes = MessageJsonPack.pack(map)
    val hitTime = bs / str.length
    for (i <- Range(0, hitTime)) {
      assert(generator.feed(bytes) == 0)
    }
    val remaining = bs % str.length
    assert(generator.feed(bytes) == str.length - remaining)
    assert(generator.feed(bytes) == str.length)
  }

  test("Test over deepth") {
    var map: Map[_, _] = Map()
    for (d <- Range(0, 200)) {
      map = Map("item" -> map)
    }
    intercept[IllegalArgumentException] {
      MessagePack.pack(map)
    }
  }

  test("Test multi unpack") {
    val messageSize = new MessageJsonPacker().feed(map).pack.size
    val generator = new MessageJsonGenerator(bufferSize = messageSize * 120)
    val multi = 50
    (0 until 2).foreach(i => {
      for (i <- Range(0, multi)) {
        generator.feed(new MessageJsonPacker().feed(map).pack)
        generator.feed("\n\n\n\n    ")
        generator.feed(new MessageJsonPacker().feed(map).pack)
      }
      for (i <- Range(0, multi * 2)) assert(eq(generator.next.get.content, map))
      assert(generator.next == None)
    })
  }

  test("Test partial unpack char by char") {
    val multi = 10
    val str = MessageJsonPack.packString(map)
    val generator = new MessageJsonGenerator()
    for (m <- Range(0, multi)) {
      for (i <- Range(0, str.length - 1)) {
        generator.feed(str.substring(i, i + 1))
        assert(generator.next == None)
      }
      generator.feed(str.substring(str.size - 1, str.size))
      assert(eq(generator.next.get.content, map))
    }
  }

  test("Test partial unpack block by block") {
    val unitSize = MessageJsonPack.packString(map).length
    val limit = unitSize * 3
    val random = new Random()
    val multi = 10
    val blockSizesA = Array.fill[Int](multi - 1)(0)
    for (i <- Range(0, blockSizesA.length)) {
      blockSizesA(i) = random.nextInt(limit)
    }
    val blockSizesB = blockSizesA.toList
    val sumB = blockSizesB.sum
    val lastSize = unitSize - (sumB % unitSize)
    val blockSizes = blockSizesB :+ lastSize
    val totalSize = sumB + lastSize
    val packer = new MessageJsonPacker()
    for (i <- Range(0, totalSize / unitSize)) {
      packer.feed(map)
    }
    val str = packer.packString
    val generator = new MessageJsonGenerator()
    var generated = 0
    (0 :: blockSizes).reduce((sum, size) => {
      generator.feed(str.substring(sum, sum + size))
      val newSum = sum + size
      val newGenerated = newSum / unitSize
      while (newGenerated > generated) {
        assert(eq(generator.next.get.content, map))
        generated += 1
      }
      assert(generator.next == None)
      assert(generator.remaining == newSum % unitSize)
      newSum
    })
  }

  test("Test partial unpack byte by byte") {
    val multi = 10
    val bytes = MessageJsonPack.pack(map)
    val generator = new MessageJsonGenerator()
    for (m <- Range(0, multi)) {
      for (i <- Range(0, bytes.length - 1)) {
        generator.feed(Array[Byte](bytes(i)))
        assert(generator.next == None)
      }
      generator.feed(Array[Byte](bytes(bytes.size - 1)))
      assert(eq(generator.next.get.content, map))
    }
  }

  test("Test partial unpack bytes block by block") {
    val unitSize = MessageJsonPack.pack(map).length
    val limit = unitSize * 3
    val random = new Random()
    val multi = 10
    val blockSizesA = Array.fill[Int](multi - 1)(0)
    for (i <- Range(0, blockSizesA.length)) {
      blockSizesA(i) = random.nextInt(limit)
    }
    val blockSizesB = blockSizesA.toList
    val sumB = blockSizesB.sum
    val lastSize = unitSize - (sumB % unitSize)
    val blockSizes = blockSizesB :+ lastSize
    val totalSize = sumB + lastSize
    val packer = new MessageJsonPacker()
    for (i <- Range(0, totalSize / unitSize)) {
      packer.feed(map)
    }
    val bytes = packer.pack
    val generator = new MessageJsonGenerator()
    var generated = 0
    (0 :: blockSizes).reduce((sum, size) => {
      generator.feed(bytes.slice(sum, sum + size))
      val newSum = sum + size
      val newGenerated = newSum / unitSize
      while (newGenerated > generated) {
        assert(eq(generator.next.get.content, map))
        generated += 1
      }
      assert(generator.next == None)
      newSum
    })
    assert(generator.remaining == 0)
  }

  test("Test over size") {
    val bufferSize = 100000
    val generator = new MessageJsonGenerator(bufferSize = bufferSize)
    val str = MessageJsonPack.packString(map)
    val share = bufferSize / str.length
    for (i <- Range(0, share)) generator.feed(str)
    val lastBuffer = CharBuffer.wrap(str)
    generator.feed(lastBuffer)
    assert((share + 1) * str.length - bufferSize == lastBuffer.remaining)
    for (i <- Range(0, share)) assert(eq(generator.next.get.content, map))
    assert(generator.next == None)
    generator.feed(lastBuffer)
    assert(eq(generator.next.get.content, map))
  }

  test("Test exception") {
    val generator = new MessageJsonGenerator
    val str = MessageJsonPack.packString(map)
    generator.feed(str)
    generator.feed(str.substring(1, str.length - 1))
    generator.feed(str)
    assert(eq(generator.next.get.content, map))
    intercept[IOException] {
      generator.next()
    }
  }

  private def eq(a: Any, b: Any): Boolean = {
    def tryWrapInList(v: Any): Option[List[Any]] = {
      val list: ListBuffer[Any] = ListBuffer()
      v match {
        case array: Array[_] => {
          for (i <- Range(0, array.length)) {
            list += array(i)
          }
        }
        case seq: Seq[_] => list.appendAll(seq)
        case set: Set[_] => list.appendAll(set)
        case set: java.util.Set[_] => {
          val it = set.iterator
          while (it.hasNext) {
            list += it.next
          }
        }
        case l: java.util.List[_] => {
          val it = l.iterator
          while (it.hasNext) {
            list += it.next
          }
        }
        case _ => return None
      }
      Some(list.toList)
    }

    def tryWrapInMap(v: Any): Option[Map[String, Any]] = {
      val hm: collection.mutable.HashMap[String, Any] = collection.mutable.HashMap()
      v match {
        case map: scala.collection.Map[_, Any] => map.foreach(entry => {
          hm += (entry._1.toString -> entry._2)
        })
        case map: java.util.Map[_, _] => {
          val it = map.entrySet.iterator
          while (it.hasNext) {
            val entry = it.next
            hm += (entry.getKey.toString -> entry.getValue)
          }
        }
        case _ => return None
      }
      Some(hm.toMap)
    }

    def mapEq(mapA: Map[String, Any], mapB: Map[String, Any]): Boolean = {
      if (mapA.size != mapB.size) return false
      for ((k, v) <- mapA) {
        if (!mapB.contains(k)) return false
        if (!eq(v, mapB.get(k).get)) return false
      }
      return true
    }

    val listSomeA = tryWrapInList(a)
    val listSomeB = tryWrapInList(b)
    val mapSomeA = tryWrapInMap(a)
    val mapSomeB = tryWrapInMap(b)
    if (listSomeA != None && listSomeB != None) {
      return listSomeA.get == listSomeB.get
    }
    if (mapSomeA != None && mapSomeB != None) {
      return mapEq(mapSomeA.get, mapSomeB.get)
    }
    if ((a == None || a == null || a == Unit) && (b == None || b == null || b == Unit)) return true
    if ((a.isInstanceOf[BigInteger] || a.isInstanceOf[BigInt]) && (b.isInstanceOf[BigInteger] || b.isInstanceOf[BigInt])) {
      return (a match {
        case bi: BigInteger => BigInt.javaBigInteger2bigInt(bi)
        case bi: BigInt => bi
      }) == (b match {
        case bi: BigInteger => BigInt.javaBigInteger2bigInt(bi)
        case bi: BigInt => bi
      })
    }
    return a == b
  }
}
