package com.hydra.core

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.{ConcurrentHashMap, Executors, LinkedBlockingQueue, TimeUnit}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong, AtomicReference}
import collection.JavaConverters._
import com.hydra.core.MessageType._
import scala.collection.mutable
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Random
import scala.language.postfixOps

class MessageSession(val id: Int, val manager: MessageSessionManager) {
  private val creationTime = System.currentTimeMillis
  private val messageReceived = new AtomicInteger(0)
  private val messageSend = new AtomicInteger(0)
  private val bytesReceived = new AtomicLong(0)
  private val bytesSend = new AtomicLong(0)
  private val serviceName = new AtomicReference[Option[String]](None)
  private val messageSendingQueue = new LinkedBlockingQueue[Message]()
  private val messageFetchers = new LinkedBlockingQueue[Tuple2[(Option[Message]) => Unit, Long]]()
  private val lastFetched = new AtomicLong(creationTime)
  private[core] val lastVisited = new AtomicLong(System.currentTimeMillis)
  private[core] val properties = new ConcurrentHashMap[String, String]()
  private[core] val running = new AtomicBoolean(true)

  private[core] def close = {
    manager.unregisterSession(this)
    running set false
  }

  def registerAsServcie(serviceName: String) = {
    if (isService) throw new MessageException(s"$toString is already registered as servcie [${this.serviceName.get.get}]")
    this.serviceName set Some(serviceName)
    manager.registerService(this)
  }

  def unregisterAsServcie() = {
    if (!isService) throw new MessageException(s"$toString is not registered as servcie.")
    manager.unregisterService(serviceName.get.get)
    this.serviceName set None
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
    manager.notifyMessageFetchMatch(this)
  }

  def addMessageFetcher(fetcher: (Option[Message]) => Unit) = {
    messageFetchers.offer((fetcher, System.currentTimeMillis()))
    lastFetched set System.currentTimeMillis()
    manager.notifyMessageFetchMatch(this)
  }

  //  def getMessageSendingQueueSize = messageSendingQueue.size
  //
  //  def takeMessageSendingQueueHead = messageSendingQueue.take()
  //
  //  def pollMessageSendingQueueHead(timeout: Long, unit: TimeUnit) = messageSendingQueue.poll(timeout, unit)
  //
  //  def pollMessageSendingQueueHead = messageSendingQueue.poll()

  private[core] def completeMessageFetch = while (messageSendingQueue.size > 0 && messageFetchers.size > 0) {
    val fetcher = messageFetchers.poll()._1
    val message = messageSendingQueue.poll()
    try {
      fetcher(Some(message))
    } catch {
      case e: Throwable => println(e)
    }
  }

  private[core] def completeOverdueFetchers(before: Long) = {
    val cont = new AtomicBoolean(true)
    while (messageFetchers.size > 0 && cont.get) {
      val peek = messageFetchers.peek()
      if (peek._2 < before) {
        val fetcher = messageFetchers.poll()._1
        try {
          fetcher(None)
        } catch {
          case e: Throwable => println(e)
        }
      }
      else cont set false
    }
  }

  private[core] def isDiscarded(before: Long) = lastFetched.get < before

  private class Invoker {
    def registerAsService(serviceName: String) = MessageSession.this.registerAsServcie(serviceName)

    def unregisterAsService() = MessageSession.this.unregisterAsServcie()

    def ping() = Unit

    def getServiceList() = manager.getServices
  }

  private val invoker = new Invoker

  def runtimeInvoker = new RuntimeInvoker(invoker)
}

/*
* Thread model in MessageSessionManager:
* 1. invoke messageDispatch to submit a Message to the service. This method simply put the message into a blocking queue
* and returns immediately with current Token.
* 2. A MessageDispatch Thread will work in the background. This thread fetch messages from the blocking queue and dispatch
* them, either forward to the target Session's queue, or deal with it and return to the current Session's queue.
* 3. A MessageSend Thread will work in the background. This thread will perform several tasks one by one. a) Session with
* new message. Notified when a message is put in a Session's queue. Check if this message can be send. b) Message fetching.
* Notified when a message fetcher is registered in a Session. Check if any message can be send. c) Clean. Performed periodically.
* Check for any potential Message-Fetcher match. Check for overdue Fetchers and Sessions.
*/
class MessageSessionManager(val sessionOverdue: Long = 30000) {
  private val SessionIDPool = new AtomicInteger(0)
  private val sessionMap = new ConcurrentHashMap[Int, MessageSession]()
  private val serviceMap = new ConcurrentHashMap[String, MessageSession]()
  private val dispatchingMessageQueue = new LinkedBlockingQueue[Message]()
  private val messageDispatchExecutor = Executors.newSingleThreadExecutor()
  private val messageSendExecutor = Executors.newSingleThreadExecutor()
  private val timer = Executors.newScheduledThreadPool(1)

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

  def messageDispatch(message: Message) = dispatchingMessageQueue.offer(message)

  messageDispatchExecutor.submit(new Runnable {
    override def run() = while (!messageDispatchExecutor.isShutdown) {
      dispatchingMessageQueue.poll(1, TimeUnit.SECONDS) match {
        case null =>
        case message => doMessageDispatch(message)
      }
    }
  })
  timer.scheduleWithFixedDelay(() => messageSendExecutor.submit(new Runnable {
    override def run() = checkClean
  }), 1, 1, TimeUnit.SECONDS)

  private def checkClean = {
    sessionMap.values().asScala.toList.foreach(session => {
      try {
        session.completeMessageFetch
        val overdue = System.currentTimeMillis() - sessionOverdue
        session.completeOverdueFetchers(overdue)
        if (session.isDiscarded(overdue) || !session.running.get) {
          session.completeOverdueFetchers(Long.MaxValue)
          session.close
        }
      } catch {
        case e: Throwable => println(e)
      }
    })
  }

  def stop = {
    messageDispatchExecutor.shutdown()
    messageSendExecutor.submit(new Runnable {
      override def run(): Unit = {
        sessionMap.values().asScala.toList.foreach(_.close)
        checkClean
      }
    })
    messageSendExecutor.shutdown()
    timer.shutdown()
  }

  private[core] def notifyMessageFetchMatch(session: MessageSession) = {
    messageSendExecutor.submit(new Runnable {
      override def run(): Unit = try {
        session.completeMessageFetch
        if (!session.running.get) {
          session.completeOverdueFetchers(Long.MaxValue)
        }
      } catch {
        case e: Throwable => println(e)
      }
    })
  }

  private def doMessageDispatch(message: Message) = {
    val fromSession = message.from match {
      case some: Some[Any] => some.get match {
        case id: Int => getSession(id)
        case name: String => getService(name)
        case a => {
          println(s"Invalid FROM: ${a}")
          None
        }
      }
      case None => {
        println(s"Bad request: no FROM.")
        None
      }
      case _ => {
        println(s"Bad request: invalid FROM.")
        None
      }
    }
    fromSession match {
      case None => println("No fromSession.")
      case Some(from) => try {
        message.to match {
          case None => messageDispatchLocal(message, from)
          case Some(to) => {
            val sessionOption = to match {
              case id: Int => getSession(id)
              case name: String => getService(name)
              case a => throw new MessageException(s"Invalid TO: $to")
            }
            val session = sessionOption match {
              case None => throw new MessageException(s"TO not exits: $to")
              case Some(s) => s
            }
            session.sendMessage(message)
          }
        }
      } catch {
        case e: Throwable if (e.isInstanceOf[IllegalArgumentException] || e.isInstanceOf[IllegalStateException]) => from.sendMessage(message.error(e.getMessage))
        case e: InvocationTargetException => from.sendMessage(message.error(e.getCause.getMessage))
        case e: Throwable => from.sendMessage(message.error(e.getMessage))
      }
    }
  }

  private def messageDispatchLocal(message: Message, fromSession: MessageSession) {
    message.messageType match {
      case Request => localRequest(message, fromSession)
      case _ => throw new MessageException(s"Undealable message to server: ${message}")
    }
  }

  def localRequest(request: Message, fromSession: MessageSession) {
    val response = fromSession.runtimeInvoker.invoke(request)
    fromSession.sendMessage(response)
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

  def fetchNewMessage(token: String): Future[Option[Message]] = {
    val session = statelessSessions.getOrDefault(token, Int.MinValue) match {
      case Int.MinValue => throw new MessageException(s"Session token invalid.")
      case sessionID => manager.getSession(sessionID) match {
        case None => throw new MessageException(s"Session ID ${sessionID} invalid.")
        case Some(s) => s
      }
    }
    val messageOptionRef = new AtomicReference[Option[Message]](None)
    Future[Option[Message]] {
      messageOptionRef get
    }(new ExecutionContext {
      def execute(runnable: Runnable): Unit = {
        session.addMessageFetcher((messageOption) => {
          messageOptionRef set messageOption
          runnable.run()
        })
      }

      def reportFailure(cause: Throwable): Unit = {
        println(s"mm:$cause")
      }
    })
  }

  def stop = {
    manager.stop
  }
}

class StatelessSessionProperties(val clientMachineID: String, val sessionToken: Option[String]) {}