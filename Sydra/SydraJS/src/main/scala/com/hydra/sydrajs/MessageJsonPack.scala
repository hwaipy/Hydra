package com.hydra.sydrajs

import java.io.IOException
import java.math.BigInteger
import java.nio.charset.{Charset, MalformedInputException}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong, AtomicReference}

import scala.collection.mutable.ListBuffer
import collection.JavaConverters._
import java.nio.{ByteBuffer, CharBuffer}
import java.util.regex.Pattern

import play.api.libs.json._

object MessageJsonPack {
  protected[sydrajs] val maxMessageDeepth = 100

  def pack(message: Message) = new MessageJsonPacker().pack(message)

  def parse(jsonString: String) = new MessageJsonGenerator().parse(jsonString)
}

class MessageJsonPacker {
  def pack(message: Message) = {
    Json.stringify(toJSValue(message.content, 0))
  }

  def toJSValue(value: Any, deepth: Int): JsValue = {
    if (deepth > MessageJsonPack.maxMessageDeepth) {
      throw new IllegalArgumentException("Message over deepth.")
    }
    value match {
      case n if n == null || n == None => JsNull
      case i: Int => JsNumber(i)
      case i: AtomicInteger => JsNumber(i.get)
      case s: String => JsString(s)
      case b: Boolean => JsBoolean(b)
      case b: AtomicBoolean => JsBoolean(b.get)
      case l: Long => JsNumber(l)
      case l: AtomicLong => JsNumber(l.get)
      case s: Short => JsNumber(s.toInt)
      case c: Char => JsNumber(c.toInt)
      case b: Byte => JsNumber(b.toInt)
      case f: Float => JsNumber(f.toDouble)
      case d: Double => JsNumber(d)
      case bi: BigInteger => JsNumber(BigDecimal(BigInt(bi)))
      case bi: BigInt => JsNumber(BigDecimal(bi))
      case bd: java.math.BigDecimal => JsNumber(bd)
      case bd: BigDecimal => JsNumber(bd)
      //      case bytes: Array[Byte] => {
      //        packer.packBinaryHeader(bytes.length)
      //        packer.writePayload(bytes)
      //      }
      case array: Array[_] => JsArray(array.toSeq.map(item => toJSValue(item, deepth + 1)))
      case seq: Seq[_] => JsArray(seq.map(item => toJSValue(item, deepth + 1)))
      case set: Set[_] => JsArray(set.toSeq.map(item => toJSValue(item, deepth + 1)))
      case set: java.util.Set[_] => JsArray(set.asScala.toSeq.map(item => toJSValue(item, deepth + 1)))
      case list: java.util.List[_] => JsArray(list.asScala.toSeq.map(item => toJSValue(item, deepth + 1)))
      case map: scala.collection.Map[_, Any] => JsObject(map.map(z => (z._1.toString, toJSValue(z._2, deepth + 1))))
      case map: java.util.Map[_, _] => JsObject(map.asScala.map(z => (z._1.toString, toJSValue(z._2, deepth + 1))))
      case unit if (unit == Unit || unit == scala.runtime.BoxedUnit.UNIT) => JsNull
      case o => throw new MessageException(s"[${o.getClass.toString}]${o} can not be convert to JSON.")
    }
  }
}

class MessageJsonGenerator {
  def parse(jsonString: String) = {
    val jsValue = Json.parse(jsonString)
    toObject(jsValue) match {
      case o: Map[_, Any] => Message.wrap(o.asInstanceOf[Map[String, Any]])
      case o => throw new IOException(s"Message is not a MAP: $jsonString")
    }
  }

  def toObject(value: JsValue): Any = {
    value match {
      case JsNull => None
      case number: JsNumber => {
        val bigDecValue = number.value
        if (bigDecValue.isValidInt) bigDecValue.intValue
        else if (bigDecValue.isValidLong) bigDecValue.longValue
        else {
          bigDecValue.toBigIntExact() match {
            case Some(v) => v
            case None => {
              if (bigDecValue.isDecimalDouble) bigDecValue.doubleValue
              else bigDecValue
            }
          }
        }
      }
      case boolean: JsBoolean => boolean.value
      case obj: JsObject => obj.fields.map(z => z._1 -> toObject(z._2)).toMap
      case arr: JsArray => arr.value.toList.map(toObject)
      case str: JsString => str.value
      case _ => throw new MessageException(s"Unknown JSON object: [${value}]")
    }
  }
}
