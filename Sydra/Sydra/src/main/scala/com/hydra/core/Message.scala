package com.hydra.core

import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable.HashMap

object Message {
  val KeyMessageID = "MessageID"
  val KeyResponseID = "ResponseID"
  val KeyObjectID = "ObjectID"
  val KeyRequest = "Request"
  val KeyResponse = "Response"
  val KeyError = "Error"
  val KeyFrom = "From"
  val KeyTo = "To"
  val KeyNoResponse = "NoResponse"
  val Preserved = Set(KeyMessageID, KeyResponseID, KeyRequest, KeyResponse, KeyError, KeyFrom, KeyTo)
  def wrap(content: collection.Map[String, Any]): Message = {
    new Message(content.toMap)
  }
  def newBuilder: MessageBuilder = return MessageBuilder.newBuilder
}

class Message private (val content: Map[String, Any]) {
  import MessageType._
  def messageID: Long = {
    content.get(Message.KeyMessageID) match {
      case v: Some[Any] => v.get match {
        case l: Long => l
        case i: Int => i
        case _ => throw new IllegalArgumentException(s"MessageID ${v} not recognized.")
      }
      case _ => throw new IllegalArgumentException("MessageID not exists in Message.")
    }
  }

  def messageType: MessageType = {
    content.get(Message.KeyRequest).foreach(_ => return Request)
    content.get(Message.KeyResponse).foreach(_ => return Response)
    content.get(Message.KeyError).foreach(_ => return Error)
    Unknown
  }

  def requestContent = {
    if (messageType != Request) throw new IllegalStateException(s"Can not fetch request content in a ${messageType} message.")
    val (name: String, args) = get[Any](Message.KeyRequest, false, false).get match {
      case name: String => (name, Nil)
      case list: List[Any] => (list.head, list.tail)
      case _ => throw new IllegalStateException(s"Illegal message.")
    }
    val map: HashMap[String, Any] = new HashMap
    content.filterKeys(!Message.Preserved.contains(_)).foreach(map += _)
    (name, args, map.toMap)
  }

  def responseContent = {
    if (messageType != Response) throw new IllegalStateException(s"Can not fetch response content in a ${messageType} message.")
    val content = get[Any](Message.KeyResponse, true, false) match {
      case Some(x) => x
      case _ => Unit
    }
    val responseID = getLong(Message.KeyResponseID, false, false).get
    (content, responseID)
  }

  def errorContent = {
    if (messageType != Error) throw new IllegalStateException(s"Can not fetch error content in a ${messageType} message.")
    val content = get[Any](Message.KeyError, false, false).get
    val responseID = getLong(Message.KeyResponseID, false, false).get
    (content, responseID)
  }

  def get[A](key: String, nilValid: Boolean = true, nonKeyValid: Boolean = true) = {
    content.get(key) match {
      case valueSome: Some[Any] => {
        valueSome.get match {
          case value if value == null || value == None =>
            if (nilValid) None else throw new IllegalArgumentException(s"Nil value invalid with key ${key}.");
          case value =>
            try {
              Some(value.asInstanceOf[A])
            } catch {
              case e: Throwable => throw new IllegalArgumentException(s"The value of key ${key} is invalid.")
            }
        }
      }
      case _ => if (nonKeyValid) None else throw new IllegalArgumentException(s"Message does not contains key s{key}.")
    }
  }
  def getLong(key: String, nilValid: Boolean = true, nonKeyValid: Boolean = true): Option[Long] = {
    get[Any](key, nilValid, nonKeyValid) match {
      case Some(v) => v match {
        case i: Int => Some(i)
        case l: Long => Some(l)
        case c: Char => Some(c)
        case b: Byte => Some(b)
        case _ => throw new IllegalArgumentException(s"The value of key ${key} is invalid.")
      }
      case None => None
    }
  }

  def to = {
    get[String](Message.KeyTo)
  }
  def from = {
    get[String](Message.KeyFrom)
  }

  def pack = {
    MessagePack.pack(content)
  }

  def objectID = {
    getLong(Message.KeyObjectID)
  }

  def +(entry: Tuple2[String, Any]) = Message.wrap(content + entry)
  def builder = Message.newBuilder ++= content
  def responseBuilder(content: Any) = Message.newBuilder.asResponse(content, messageID, from)
  def response(content: Any) = responseBuilder(content).create
  def errorBuilder(content: Any) = Message.newBuilder.asError(content, messageID, from)
  def error(content: Any) = errorBuilder(content).create

  override def toString() = {
    val cs = content.toString
    s"Message {${cs.substring(4, cs.length - 1)}}"
  }
}

object MessageBuilder {
  private val MessageIDs = new AtomicLong(0)
  def newBuilder(): MessageBuilder = return new MessageBuilder()
}

class MessageBuilder private (updateID: Boolean = true) {
  import MessageType._
  private val content: HashMap[String, Any] = new HashMap
  if (updateID) { content += Message.KeyMessageID -> MessageBuilder.MessageIDs.getAndIncrement }
  def create: Message = {
    Message.wrap(content)
  }
  def to(target: String) = {
    if (target != null && !target.isEmpty) {
      this += Message.KeyTo -> target
    }
    this
  }
  def objectID(objectID: Long) = {
    this += Message.KeyObjectID -> objectID
    this
  }
  def +=(entry: Tuple2[String, Any]) = {
    content += entry
    this
  }
  def ++=(map: Map[String, Any]) = {
    content ++= map
    this
  }
  private def asType(messageType: MessageType, content: Any) = {
    this.content.remove(Message.KeyRequest)
    this.content.remove(Message.KeyResponse)
    this.content.remove(Message.KeyError)
    messageType match {
      case Request => this.content += Message.KeyRequest -> content
      case Response => this.content += Message.KeyResponse -> content
      case Error => this.content += Message.KeyError -> content
      case _ => throw new IllegalArgumentException("Unknown type can not be set.")
    }
    this
  }
  def asRequest(name: String, args: List[Any] = Nil, kwargs: Map[String, Any] = Map()) = {
    val content = if (args.isEmpty) name else name :: args
    asType(Request, content)
    kwargs.foreach(entry => {
      if (Message.Preserved.contains(entry._1)) throw new IllegalArgumentException(s"${entry._1} can not be a name of parameter.}")
      this.content += entry
    })
    this
  }
  def asResponse(content: Any, responseID: Long, to: Option[String] = None) = {
    asType(Response, content)
    this.content += Message.KeyResponseID -> responseID
    if (to.isDefined) this.content += Message.KeyTo -> to.get
    this
  }
  def asError(content: Any, responseID: Long, to: Option[String] = None) = {
    asType(Error, content)
    this.content += Message.KeyResponseID -> responseID
    if (to.isDefined) this.content += Message.KeyTo -> to.get
    this
  }
}

object MessageType extends Enumeration {
  type MessageType = Value
  val Request, Response, Error, Unknown = Value
}

