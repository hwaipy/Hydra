package com.hydra.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference}
import collection.JavaConverters._

class MessageSession(val id: Int, val manager: MessageSessionManager) {
  private val creationTime = System.currentTimeMillis
  private val messageReceived = new AtomicInteger(0)
  private val messageSend = new AtomicInteger(0)
  private val bytesReceived = new AtomicLong(0)
  private val bytesSend = new AtomicLong(0)
  private val serviceName = new AtomicReference[Option[String]](None)

  def close = {
    println(s"closing ${id}")
    println(manager.servicesCount)
    manager.unregisterSession(this)
    println(manager.servicesCount)
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