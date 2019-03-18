//package com.hydra.io
//
//import java.io.IOException
//import java.math.BigInteger
//import java.nio.charset.{Charset, MalformedInputException}
//import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong, AtomicReference}
//import scala.collection.mutable.ListBuffer
//import play.api.libs.json._
//
//import collection.JavaConverters._
//import java.nio.{ByteBuffer, CharBuffer}
//import java.util.regex.Pattern
//
//object MessageJsonPack {
//  protected[core] val maxMessageDeepth = 100
//
//  def pack(value: Map[String, Any]) = new MessageJsonPacker().feed(value).pack()
//
//  def packString(value: Map[String, Any]) = new MessageJsonPacker().feed(value).packString()
//}
//
//class MessageJsonPacker(prefix:Boolean = false) extends MessageEncoder {
//  private val jsValues = new ListBuffer[JsValue]()
//
//  def feed(msg: Message) = feed(msg.content)
//
//  def feed(value: Any): MessageJsonPacker = {
//    jsValues += toJSValue(value, 0)
//    this
//  }
//
//  def toJSValue(value: Any, deepth: Int): JsValue = {
//    if (deepth > MessageJsonPack.maxMessageDeepth) {
//      throw new IllegalArgumentException("Message over deepth.")
//    }
//    value match {
//      case n if n == null || n == None => JsNull
//      case i: Int => JsNumber(i)
//      case i: AtomicInteger => JsNumber(i.get)
//      case s: String => JsString(s)
//      case b: Boolean => JsBoolean(b)
//      case b: AtomicBoolean => JsBoolean(b.get)
//      case l: Long => JsNumber(l)
//      case l: AtomicLong => JsNumber(l.get)
//      case s: Short => JsNumber(s.toInt)
//      case c: Char => JsNumber(c.toInt)
//      case b: Byte => JsNumber(b.toInt)
//      case f: Float => JsNumber(f.toDouble)
//      case d: Double => JsNumber(d)
//      case bi: BigInteger => JsNumber(BigDecimal(BigInt(bi)))
//      case bi: BigInt => JsNumber(BigDecimal(bi))
//      case bd: java.math.BigDecimal => JsNumber(bd)
//      case bd: BigDecimal => JsNumber(bd)
//      //      case bytes: Array[Byte] => {
//      //        packer.packBinaryHeader(bytes.length)
//      //        packer.writePayload(bytes)
//      //      }
//      case array: Array[_] => JsArray(array.toSeq.map(item => toJSValue(item, deepth + 1)))
//      case seq: Seq[_] => JsArray(seq.map(item => toJSValue(item, deepth + 1)))
//      case set: Set[_] => JsArray(set.toSeq.map(item => toJSValue(item, deepth + 1)))
//      case set: java.util.Set[_] => JsArray(set.asScala.toSeq.map(item => toJSValue(item, deepth + 1)))
//      case list: java.util.List[_] => JsArray(list.asScala.toSeq.map(item => toJSValue(item, deepth + 1)))
//      case map: scala.collection.Map[_, Any] => JsObject(map.map(z => (z._1.toString, toJSValue(z._2, deepth + 1))))
//      case map: java.util.Map[_, _] => JsObject(map.asScala.map(z => (z._1.toString, toJSValue(z._2, deepth + 1))))
//      case unit if (unit == Unit || unit == scala.runtime.BoxedUnit.UNIT) => JsNull
//      case o => throw new MessageException(s"[${o.getClass.toString}]${o} can not be convert to JSON.")
//    }
//  }
//
//  def packString() = {
//    val strArray = jsValues.map(Json.stringify)
//    jsValues.clear()
//    val strArray2 = if(prefix){
//      strArray.map(str => s"${str.size}:${str}")
//    }else strArray
//    strArray2.mkString("")
//  }
//
//  def pack() = packString().getBytes("UTF-8")
//}
//
//class MessageJsonGenerator(prefix:Boolean = false, val bufferSize: Int = 10 * 1024 * 1024) extends MessageDecoder {
//
//  private val buffer = CharBuffer.allocate(bufferSize)
//  buffer.limit(0)
//  private val byteBuffer = ByteBuffer.allocate(100)
//  byteBuffer.limit(0)
//  private val decoder = Charset.forName("UTF-8").newDecoder()
//  private val messageGenerated = new AtomicInteger(0)
//  private val bytesConverted = new AtomicLong(0)
//
//  def feed(buffer: ByteBuffer): Int = {
//    try {
//      val bytes = new Array[Byte](buffer.limit() - buffer.position())
//      buffer.get(bytes)
//      feed(bytes)
//    } catch {
//      case e: Throwable => {
//        e.printStackTrace()
//        0
//      }
//    }
//  }
//
//  def feed(feed: Array[Byte]): Int = {
//    val tempBuffer = ByteBuffer.allocate(byteBuffer.limit() + feed.size)
//    tempBuffer.put(byteBuffer)
//    byteBuffer.clear()
//    tempBuffer.put(feed)
//    val decodeDone = new AtomicBoolean(false)
//    val decodeRemaining = new AtomicInteger(0)
//    val decoded = new AtomicReference[String](null)
//    while (!decodeDone.get && decodeRemaining.get < 10) {
//      tempBuffer.position(0)
//      tempBuffer.limit(tempBuffer.capacity() - decodeRemaining.get)
//      try {
//        decoded set decoder.decode(tempBuffer).toString
//        decodeDone set true
//      } catch {
//        case e: MalformedInputException => decodeRemaining.incrementAndGet()
//      }
//    }
//    tempBuffer.position(tempBuffer.limit())
//    tempBuffer.limit(tempBuffer.capacity())
//    byteBuffer.put(tempBuffer)
//    byteBuffer.limit(byteBuffer.position())
//    byteBuffer.position(0)
//    bytesConverted.addAndGet(feed.size)
//    this.feed(decoded.get)
//  }
//
//  def feed(feed: String): Int = {
//    val buffer = CharBuffer.wrap(feed)
//    this.feed(buffer)
//    buffer.remaining
//  }
//
//  def feed(feed: CharBuffer): Int = {
//    var currentPosition: Int = buffer.position();
//    if (feed.remaining() > (buffer.capacity() - buffer.limit())) {
//      buffer.compact()
//      currentPosition = 0
//    } else {
//      buffer.position(buffer.limit())
//      buffer.limit(buffer.capacity())
//    }
//    val feedLimit: Int = feed.limit()
//    if (feed.remaining() > buffer.remaining()) {
//      feed.limit(feed.position() + buffer.remaining())
//    }
//    buffer.put(feed)
//    feed.limit(feedLimit)
//    buffer.limit(buffer.position())
//    buffer.position(currentPosition)
//    buffer.remaining
//  }
//
//  def toObject(value: JsValue): Any = {
//    value match {
//      case JsNull => None
//      case number: JsNumber => {
//        val bigDecValue = number.value
//        if (bigDecValue.isValidInt) bigDecValue.intValue
//        else if (bigDecValue.isValidLong) bigDecValue.longValue
//        else {
//          bigDecValue.toBigIntExact() match {
//            case Some(v) => v
//            case None => {
//              if (bigDecValue.isDecimalDouble) bigDecValue.doubleValue
//              else bigDecValue
//            }
//          }
//        }
//      }
//      case boolean: JsBoolean => boolean.value
//      case obj: JsObject => obj.fields.map(z => z._1 -> toObject(z._2)).toMap
//      case arr: JsArray => arr.value.toList.map(toObject)
//      case str: JsString => str.value
//      case _ => throw new MessageException(s"Unknown JSON object: [${value}]")
//    }
//  }
//
//  private val circePattern = Pattern.compile("expected whitespace or eof got.*line ([0-9]+), column ([0-9]+)")
//
//  def next(): Option[Message] = {
//    val nextJsonString = if(prefix){
//      val str = String.valueOf(buffer.array().slice(buffer.position(), buffer.limit()))
//      val prefixLength = str.indexOf(":")
//      if (prefixLength < 0) None else{
//        val prefixNumber = str.slice(0, prefixLength).toInt
//        val end = prefixLength + 1 + prefixNumber
//        if (end > str.size) None else {
//          buffer.position(buffer.position() + end)
//          Some(str.slice(prefixLength + 1, end))
//        }
//      }
//    }else{
//      val str = String.valueOf(buffer.array().slice(buffer.position(), buffer.limit()))
//      io.circe.parser.parse(str) match {
//        case Right(right) => {
//          buffer.position(buffer.limit)
//          Some(str)
//        }
//        case Left(left) => {
//          val matcher = circePattern.matcher(left.message)
//          if (matcher.find()) {
//            val positionColumn = matcher.group(2).toInt - 1
//            buffer.position(buffer.position() + positionColumn)
//            Some(str.substring(0, positionColumn))
//          } else {
//            None
//          }
//        }
//      }
//    }
//    nextJsonString match {
//      case Some(njs) => {
//        val jsv = Json.parse(njs)
//        messageGenerated.incrementAndGet
//        toObject(jsv) match {
//          case o: Map[_, Any] => Some(Message.wrap(o.asInstanceOf[Map[String, Any]]))
//          case o => throw new IOException(s"Message is not a MAP: $nextJsonString (converted as [${o.getClass.toString}])")
//        }
//      }
//      case None => None
//    }
//  }
//
//  def remaining: Int = buffer.limit() - buffer.position()
//
//  def getStatistics = (messageGenerated.get, bytesConverted.get)
//}
