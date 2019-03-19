package com.hydra.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference}
import collection.JavaConverters._
import com.hydra.core.MessageType._
import java.util.concurrent.LinkedBlockingQueue

class MessageSession(val id: Int, val manager: MessageSessionManager) {
  private val creationTime = System.currentTimeMillis
  private val messageReceived = new AtomicInteger(0)
  private val messageSend = new AtomicInteger(0)
  private val bytesReceived = new AtomicLong(0)
  private val bytesSend = new AtomicLong(0)
  private val serviceName = new AtomicReference[Option[String]](None)
  private val messageSendingQueue = new LinkedBlockingQueue[Message]()
  private val messageSendingListener = new AtomicReference[Option[() => Unit]](None)

  def close = {
    manager.unregisterSession(this)
  }

  def registerAsServcie(serviceName: String) = {
    if (isService) throw new MessageException(s"$toString is already registered as servcie [${this.serviceName.get.get}]")
    this.serviceName set Some(serviceName)
    manager.registerService(this)
  }

  def isService = serviceName.get.isDefined

  def getServiceName = serviceName.get

  //  def writeAndFlush(msg: Message) = {
  //    MessageTransport.Logger.info(s"New message is send to $ctx: $msg")
  //    ctx.writeAndFlush(msg)
  //  }
  //
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

  private class Invoker {
    def registerAsService(serviceName: String) = MessageSession.this.registerAsServcie("")
  }

  private val invoker = new Invoker

  def runtimeInvoker = new RuntimeInvoker(invoker)
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
      //      case Some(to) => {
      //        val session = ctx.channel.attr[MessageSession](MessageServerHandler.KeySession).get
      //        if (session == null) {
      //          writeAndFlush(ctx, m.error("Client has not connected."))
      //        } else {
      //          MessageSession.getSession(to) match {
      //            case None => writeAndFlush(ctx, m.error(s"Target $to does not exists."))
      //            case Some(toSession) => {
      //              val m2 = m.builder.+=(Message.KeyFrom -> session.name).create
      //              toSession.writeAndFlush(m2)
      //              dealWithForwardedRemoteObject(ctx, m, session, toSession)
      //            }
      //          }
      //        }
      //      }
      case Some(a) => println("333333333333333333333333333e")
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
      case someID: Some[Int] => getSession(someID.get).get
      case someName: Some[String] => getService(someName.get).get
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