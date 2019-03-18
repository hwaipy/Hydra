//package com.hydra.io
//
//import java.io.IOException
//import java.math.BigInteger
//import java.nio.ByteBuffer
//import java.util.concurrent.atomic.AtomicBoolean
//import java.util.concurrent.atomic.AtomicInteger
//import java.util.concurrent.atomic.AtomicLong
//import org.msgpack.core.MessageInsufficientBufferException
//import org.msgpack.value.Value
//import scala.collection.mutable.HashMap
//import scala.collection.mutable.HashSet
//import scala.collection.mutable.ListBuffer
//import com.hydra.core.MessageException
//import com.hydra.core.Message
//
//object MessagePack {
//  protected[core] val ExtensionType: Byte = 11
//  protected[core] val maxMessageDeepth = 100
//  private val ObjectCodeMap = new HashMap[Any, Long]
//  private val CodeObjectMap = new HashMap[Long, Any]
//  private val InvokerMap = new HashMap[Long, HashSet[String]]
//  private val ID = new AtomicLong
//
//  def pack(value: Any): Array[Byte] = {
//    new MessagePacker().feed(value).pack()
//  }
//
//  protected[core] def allocate(obj: Any, invoker: String) = {
//    MessagePack.synchronized {
//      val id = ObjectCodeMap.get(obj) match {
//        case Some(id) => id
//        case None => {
//          val id = ID.getAndIncrement
//          ObjectCodeMap.put(obj, id)
//          CodeObjectMap.put(id, obj)
//          id
//        }
//      }
//      val set = InvokerMap.getOrElseUpdate(id, new HashSet)
//      set.add(invoker)
//      id
//    }
//  }
//}
//
//class MessagePacker extends MessageEncoder {
//  private val packer = org.msgpack.core.MessagePack.newDefaultBufferPacker
//  private val bufferArray = new Array[Byte](8)
//  private val buffer = ByteBuffer.wrap(bufferArray)
//
//  def feed(msg: Message): MessagePacker = feed(msg.content, None)
//
//  def feed(value: Any, target: Option[String] = None): MessagePacker = {
//    doFeed(value, 0, target)
//    this
//  }
//
//  def doFeed(value: Any, deepth: Int, target: Option[String]) {
//    if (deepth > MessagePack.maxMessageDeepth) {
//      throw new IllegalArgumentException("Message over deepth.")
//    }
//    value match {
//      case n if n == null || n == None => packer.packNil
//      case i: Int => packer.packInt(i)
//      case i: AtomicInteger => packer.packInt(i.get)
//      case s: String => packer.packString(s)
//      case b: Boolean => packer.packBoolean(b)
//      case b: AtomicBoolean => packer.packBoolean(b.get)
//      case l: Long => packer.packLong(l)
//      case l: AtomicLong => packer.packLong(l.get)
//      case s: Short => packer.packShort(s)
//      case c: Char => packer.packShort(c.toShort)
//      case b: Byte => packer.packByte(b)
//      case f: Float => packer.packFloat(f)
//      case d: Double => packer.packDouble(d)
//      case bi: BigInteger => packer.packBigInteger(bi)
//      case bi: BigInt => packer.packBigInteger(bi.bigInteger)
//      case bytes: Array[Byte] => {
//        packer.packBinaryHeader(bytes.length)
//        packer.writePayload(bytes)
//      }
//      case array: Array[_] => {
//        packer.packArrayHeader(array.length)
//        for (i <- Range(0, array.length)) {
//          doFeed(array(i), deepth + 1, target)
//        }
//      }
//      case seq: Seq[_] => {
//        packer.packArrayHeader(seq.size)
//        seq.foreach(i => doFeed(i, deepth + 1, target))
//      }
//      case set: Set[_] => {
//        packer.packArrayHeader(set.size)
//        set.foreach(i => doFeed(i, deepth + 1, target))
//      }
//      case set: java.util.Set[_] => {
//        packer.packArrayHeader(set.size)
//        val it = set.iterator
//        while (it.hasNext) {
//          doFeed(it.next, deepth + 1, target)
//        }
//      }
//      case list: java.util.List[_] => {
//        packer.packArrayHeader(list.size)
//        val it = list.iterator
//        while (it.hasNext) {
//          doFeed(it.next, deepth + 1, target)
//        }
//      }
//      case map: scala.collection.Map[_, Any] => {
//        packer.packMapHeader(map.size)
//        map.foreach(entry => {
//          doFeed(entry._1, deepth + 1, target)
//          doFeed(entry._2, deepth + 1, target)
//        })
//      }
//      case map: java.util.Map[_, _] => {
//        packer.packMapHeader(map.size)
//        val it = map.entrySet.iterator
//        while (it.hasNext) {
//          val entry = it.next
//          doFeed(entry.getKey, deepth + 1, target)
//          doFeed(entry.getValue, deepth + 1, target)
//        }
//      }
//      case unit if (unit == Unit || unit == scala.runtime.BoxedUnit.UNIT) => packer.packNil
//      case p: Product => {
//        packer.packArrayHeader(p.productArity)
//        val it = p.productIterator
//        while (it.hasNext) {
//          doFeed(it.next, deepth + 1, target)
//        }
//      }
//      case o => new MessageException(s"class ${o.getClass.toString} can not be serealized.")
//    }
//  }
//
//  def pack(): Array[Byte] = {
//    val array = packer.toByteArray
//    packer.clear
//    array
//  }
//}
//
//class MessageGenerator(val bufferSize: Int = 10 * 1024 * 1024) extends MessageDecoder {
//
//  private val buffer = ByteBuffer.allocate(bufferSize)
//  buffer.limit(0)
//  private val messageGenerated = new AtomicInteger(0)
//  private val bytesConverted = new AtomicLong(0)
//
//  def feed(feed: ByteBuffer) = {
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
//  def feed(feed: Array[Byte]): Int = {
//    val buffer: ByteBuffer = ByteBuffer.wrap(feed)
//    this.feed(buffer)
//    buffer.remaining
//  }
//
//  def next(): Option[Message] = {
//    val unpacker = org.msgpack.core.MessagePack.newDefaultUnpacker(buffer.array, buffer.position(), buffer.limit() - buffer.position());
//    try {
//      val value = unpacker.unpackValue()
//      val unpackCursor = unpacker.getTotalReadBytes()
//      buffer.position((buffer.position() + unpackCursor.asInstanceOf[Int]))
//      messageGenerated.incrementAndGet
//      bytesConverted.addAndGet(unpackCursor)
//      convert(value)
//    } catch {
//      case e: MessageInsufficientBufferException => None
//      case e: Exception => throw new IOException(e)
//    }
//  }
//
//  def remainingBytes: Int = {
//    buffer.remaining
//  }
//
//  def getStatistics = (messageGenerated.get, bytesConverted.get)
//
//  private def convert(value: Value): Option[Message] = {
//    convert(value, 0) match {
//      case map: Map[_, Any] => Some(Message.wrap(map.map((e) => (e._1.toString, e._2))))
//      case None => None
//      case _ => throw new IllegalArgumentException("Message should be a Map");
//    }
//  }
//
//  private def convert(value: Value, deepth: Int): Any = {
//    if (deepth > MessagePack.maxMessageDeepth) throw new IllegalArgumentException("Message over deepth.")
//    import org.msgpack.value.ValueType._
//    value.getValueType match {
//      case ARRAY => {
//        val arrayValue = value.asArrayValue
//        val list: ListBuffer[Any] = ListBuffer()
//        val it = arrayValue.iterator
//        while (it.hasNext) {
//          list += convert(it.next, deepth + 1)
//        }
//        list.toList
//      }
//      case MAP => {
//        val mapValue = value.asMapValue
//        val map: HashMap[Any, Any] = new HashMap
//        val it = mapValue.entrySet.iterator
//        while (it.hasNext) {
//          val entry = it.next
//          map += (convert(entry.getKey, deepth + 1) -> convert(entry.getValue, deepth + 1))
//        }
//        map.toMap
//      }
//      case BINARY => value.asBinaryValue.asByteArray
//      case BOOLEAN => value.asBooleanValue.getBoolean
//      case FLOAT => value.asFloatValue.toDouble
//      case INTEGER => {
//        val integerValue = value.asIntegerValue
//        if (integerValue.isInLongRange) {
//          if (integerValue.isInIntRange) {
//            integerValue.toInt
//          } else {
//            integerValue.toLong
//          }
//        } else {
//          BigInt.javaBigInteger2bigInt(integerValue.asBigInteger)
//        }
//      }
//      case NIL => None
//      case STRING => value.asStringValue.toString
//      case EXTENSION => {
//        val ev = value.asExtensionValue
//        ev.getType match {
//          case t => throw new MessageException(s"Unrecognized extension type: ${t}")
//        }
//      }
//      case _ => throw new MessageException(s"Unknown ValueType: ${value.getValueType}")
//    }
//  }
//}
