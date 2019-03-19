package com.hydra.core

import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue, TimeUnit}
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference}

import collection.JavaConverters._
import com.hydra.core.MessageType._

import scala.collection.mutable
import scala.util.Random

class MessageSession(val id: Int, val manager: MessageSessionManager) {
  private val creationTime = System.currentTimeMillis
  private val messageReceived = new AtomicInteger(0)
  private val messageSend = new AtomicInteger(0)
  private val bytesReceived = new AtomicLong(0)
  private val bytesSend = new AtomicLong(0)
  private val serviceName = new AtomicReference[Option[String]](None)
  private val messageSendingQueue = new LinkedBlockingQueue[Message]()
  private val messageSendingListener = new AtomicReference[Option[() => Unit]](None)
  private[core] val lastVisited = new AtomicLong(System.currentTimeMillis)
  private[core] val properties = new ConcurrentHashMap[String, String]()

  def close = {
    setMessageSendingListener(() => {})
    manager.unregisterSession(this)
  }

  def registerAsServcie(serviceName: String) = {
    if (isService) throw new MessageException(s"$toString is already registered as servcie [${this.serviceName.get.get}]")
    this.serviceName set Some(serviceName)
    manager.registerService(this)
  }

  def isService = serviceName.get.isDefined

  def getServiceName = serviceName.get

  override def toString = s"MessageSession[$id${
    serviceName.get match {
      case None => ""
      case Some(name) => s", $name"
    }
  }]"

  //  def updateMessageReceivedStatistics(messageReceived: Int, bytesReceived: Long) {
  //    this.messageReceived.set(messageReceived)
  //    this.bytesReceived.set(bytesReceived)
  //  }
  //
  //  def increseMessageSendStatistics(bytesSend: Long) {
  //    this.messageSend.incrementAndGet
  //    this.bytesSend.addAndGet(bytesSend)
  //  }
  //
  //  def summary = (id, name, connectedTime, messageSend.get, messageReceived.get, bytesSend.get, bytesReceived.get)
  def sendMessage(message: Message) = {
    messageSendingQueue.put(message)
    messageSendingListener.get.foreach(l => l())
  }

  def setMessageSendingListener(listener: () => Unit) = messageSendingListener set Some(listener)

  def getMessageSendingQueueSize = messageSendingQueue.size

  def takeMessageSendingQueueHead = messageSendingQueue.take()

  def pollMessageSendingQueueHead(timeout: Long, unit: TimeUnit) = messageSendingQueue.poll(timeout, unit)

  def pollMessageSendingQueueHead = messageSendingQueue.poll()

  private class Invoker {
    def registerAsService(serviceName: String) = MessageSession.this.registerAsServcie(serviceName)
  }

  private val invoker = new Invoker

  def runtimeInvoker = new RuntimeInvoker(invoker)

  //  def setMessageEncoding(encoding: String) = {
  //    encoding match {
  //      case MessagePack.encoding => {
  //        encoder set Some(new MessagePacker())
  //        decoder set Some(new MessageGenerator())
  //      }
  //      case _ => throw new MessageException(s"Invalid encoding: ${encoding}")
  //    }
  //  }
}

class MessageSessionManager {
  private val SessionIDPool = new AtomicInteger(0)
  private val sessionMap = new ConcurrentHashMap[Int, MessageSession]()
  private val serviceMap = new ConcurrentHashMap[String, MessageSession]()

  def newSession() = {
    val session = new MessageSession(SessionIDPool.getAndIncrement(), this)
    sessionMap.put(session.id, session)
    session
  }

  def newService(serviceName: String) = {
    val session = newSession()
    try {
      session.registerAsServcie(serviceName)
      session
    } catch {
      case e: MessageException => {
        session.close
        throw new MessageException("Failed in creating new Service.", e)
      }
    }
  }

  def messageDispatch(message: Message) = {
    message.to match {
      case None => messageDispatchLocal(message)
      case Some(to) => {
        val sessionOption = to match {
          case id: Int => getSession(id)
          case name: String => getService(name)
          case a => throw new MessageException(s"Invalid TO: $to")
        }
        val session = sessionOption match {
          case None => throw new MessageException(s"Invalid TO: $to")
          case Some(s) => s
        }
        session.sendMessage(message)
      }
    }
  }

  private def messageDispatchLocal(message: Message) {
    message.messageType match {
      case Request => localRequest(message)
      //        case Response => response(m, ctx)
      //        case Error => error(m, ctx)
      //        case _ => MessageTransport.Logger.warn("An unknown Message received.", m)
    }
  }

  def localRequest(request: Message) {
    val fromSession = request.from match {
      case some: Some[Any] => some.get match {
        case id: Int => getSession(id).get
        case name: String => getService(name).get
        case a => throw new MessageException(s"Invalid FROM: ${a}")
      }
      case None => throw new MessageException(s"Bad request: no FROM.")
      case _ => throw new MessageException(s"Bad request: invalid FROM.")
    }
    try {
      val response = fromSession.runtimeInvoker.invoke(request)
      fromSession.sendMessage(response)
    } catch {
      case e: Throwable => fromSession.sendMessage(request.error(e.getMessage))
    }
  }

  private[core] def unregisterSession(session: MessageSession): Unit = {
    session.getServiceName.foreach(unregisterService)
    sessionMap.remove(session.id)
  }

  private[core] def registerService(session: MessageSession): Unit = sessionMap.containsKey(session.id) match {
    case true => session.getServiceName.foreach(serviceName => serviceMap.putIfAbsent(serviceName, session) match {
      case null =>
      case _: MessageSession => throw new MessageException(s"Service name [${serviceName}] duplicated.")
    })
    case false => throw new MessageException(s"Session [${session.id}] does not exist.")
  }

  private[core] def unregisterService(serviceName: String): Unit = serviceMap.remove(serviceName)

  def getService(serviceName: String) = serviceMap.get(serviceName) match {
    case null => None
    case session => Some(session)
  }

  def getSession(id: Int) = sessionMap.get(id) match {
    case null => None
    case session => Some(session)
  }

  def getSessions = sessionMap.values.asScala.toList

  def getServices = serviceMap.keys().asScala.toList

  def getServiceSessions = serviceMap.values().asScala.toList

  def sessionsCount = sessionMap.size

  def servicesCount = serviceMap.size
}

object MessageService {
  private val random = new Random()
  private val alphanumeric = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray

  private[core] def generateRandomString(length: Int) = new String((0 until length).toArray.map(_ => alphanumeric(random.nextInt(alphanumeric.size))))

  private[core] val KeyClientMachineID = "ClientMachineID"
}

abstract class MessageService[T, V](val manager: MessageSessionManager) {

  def messageDispatch(message: Message, t: T): V

  protected def messageDispatch(message: Message, from: Int): Unit = manager.messageDispatch(message + (Message.KeyFrom, from))
}

class StatelessMessageService(manager: MessageSessionManager) extends MessageService[StatelessSessionProperties, String](manager) {
  val statelessSessions = new ConcurrentHashMap[String, Int]()
  val fetchingMap = new mutable.HashMap[Int, Any]()

  //TODO check and remove dead sessions

  def messageDispatch(message: Message, properties: StatelessSessionProperties) = {
    val sessionIDAndToken = properties.sessionToken match {
      case Some(token) => {
        val id = statelessSessions.getOrDefault(token, Int.MinValue)
        if (id == Int.MinValue) throw new MessageException(s"Invalid Stateless session: invalid token.")
        val session = manager.getSession(id) match {
          case None => throw new MessageException(s"Invalid Stateless session: invalid token, session does not exist.")
          case Some(session) => session
        }
        val machineID = session.properties.get(MessageService.KeyClientMachineID) match {
          case null => throw new MessageException(s"Invalid Stateless session: no related machine ID.")
          case mid => mid
        }
        if (machineID != properties.clientMachineID) throw new MessageException(s"Invalid Stateless session: machine ID mismatch.")
        (id, token)
      }
      case None => {
        val newToken = generateStatelessSessionToken(properties.clientMachineID)
        val session = manager.newSession()
        session.properties.put(MessageService.KeyClientMachineID, properties.clientMachineID)
        statelessSessions.put(newToken, session.id)
        (session.id, newToken)
      }
    }
    messageDispatch(message, sessionIDAndToken._1)
    sessionIDAndToken._2
  }

  private def generateStatelessSessionToken(clientMachineID: String) = {
    MessageService.generateRandomString(20)
  }

  def fetchNewMessage(token: String, timeout: Long = 10, unit: TimeUnit = TimeUnit.SECONDS): Option[Message] = {
    val session = statelessSessions.getOrDefault(token, Int.MinValue) match {
      case Int.MinValue => throw new MessageException(s"Session token invalid.")
      case sessionID => manager.getSession(sessionID) match {
        case None => throw new MessageException(s"Session ID ${sessionID} invalid.")
        case Some(s) => s
      }
    }
    session.pollMessageSendingQueueHead(timeout, unit) match {
      case null => None
      case message => Some(message)
    }
  }
}

class StatelessSessionProperties(val clientMachineID: String, val sessionToken: Option[String]) {}