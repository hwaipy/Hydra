package com.hydra.core

import java.lang.Thread.UncaughtExceptionHandler
import java.net.{HttpURLConnection, URL}
import java.nio.ByteBuffer
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import com.hydra.core.MessageType._
import com.hydra.io._

import scala.language.dynamics
import scala.language.postfixOps

protected abstract class DynamicRemoteObject(val client: MessageClient, val remoteName: String = "", val remoteID: Long = 0) extends Dynamic {
  override def toString() = s"RemoteObject[$remoteName,$remoteID]"
}

class MessageRemoteObject(client: MessageClient, remoteName: String = "", remoteID: Long = 0) extends DynamicRemoteObject(client, remoteName, remoteID) {
  def selectDynamic(name: String): Message = new InvokeItem(client, remoteName, remoteID, name)().toMessage

  def applyDynamic(name: String)(args: Any*): Message = new InvokeItem(client, remoteName, remoteID, name)(args.map(m => ("", m)): _*).toMessage

  def applyDynamicNamed(name: String)(args: (String, Any)*): Message = new InvokeItem(client, remoteName, remoteID, name)(args: _*).toMessage
}

class BlockingRemoteObject(client: MessageClient, remoteName: String = "", remoteID: Long = 0, timeout: Duration = 10 second) extends DynamicRemoteObject(client, remoteName, remoteID) {
  def selectDynamic(name: String): Any = new InvokeItem(client, remoteName, remoteID, name)().requestMessage(timeout)

  def applyDynamic(name: String)(args: Any*): Any = new InvokeItem(client, remoteName, remoteID, name)(args.map(m => ("", m)): _*).requestMessage(timeout)

  def applyDynamicNamed(name: String)(args: (String, Any)*): Any = new InvokeItem(client, remoteName, remoteID, name)(args: _*).requestMessage(timeout)
}

class AsynchronousRemoteObject(client: MessageClient, remoteName: String = "", remoteID: Long = 0) extends DynamicRemoteObject(client, remoteName, remoteID) {
  def selectDynamic(name: String): Future[Any] = new InvokeItem(client, remoteName, remoteID, name)().sendMessage

  def applyDynamic(name: String)(args: Any*): Future[Any] = new InvokeItem(client, remoteName, remoteID, name)(args.map(m => ("", m)): _*).sendMessage

  def applyDynamicNamed(name: String)(args: (String, Any)*): Future[Any] = new InvokeItem(client, remoteName, remoteID, name)(args: _*).sendMessage
}

private class InvokeItem(client: MessageClient, target: String, id: Long = 0, name: String)(args: (String, Any)*) {
  val argsList: ArrayBuffer[Any] = new ArrayBuffer
  val namedArgsMap = new mutable.HashMap[String, Any]
  args.foreach(m => m match {
    case (name, value) if name == null || name.isEmpty => argsList += value
    case (name, value) =>
      if (Message.Preserved.contains(name)) throw new IllegalArgumentException(s"${name} is preserved.")
      else namedArgsMap.put(name, value)
  })

  def sendMessage = client.sendMessage(toMessage)

  def requestMessage(timeout: Duration) = client.requestMessage(toMessage, timeout)

  def toMessage = {
    val builder = Message.newBuilder
      .asRequest(name, argsList.toList, namedArgsMap.toMap)
    if (target != null && target.nonEmpty) builder.to(target)
    if (id > 0) builder.objectID(id)
    builder.create
  }
}

object MessageClient {
  def create(channel: MessageChannel): MessageClient = create(channel, "", None)

  def create(channel: MessageChannel, serviceName: String, invokeHandler: Any): MessageClient = {
    val client = new MessageClient(channel, invokeHandler)
    serviceName match {
      case "" => client.blockingInvoker().ping()
      case name => try {
        client.blockingInvoker().registerAsService(serviceName)
      } catch {
        case e: MessageException => {
          client.close
          throw e
        }
      }
    }
    client
  }
}

class MessageClient(channel: MessageChannel, invokeHandler: Any) extends Dynamic {
  private val invoker = new RuntimeInvoker(invokeHandler)

  def sendMessage(msg: Message) = {
    require(msg.messageType == Request)
    channel.future(msg)
  }

  def requestMessage(msg: Message, timeout: Duration) = {
    val future = this.sendMessage(msg)
    Await.result[Any](future, timeout)
  }

  def messageInvoker(target: String = "") = new MessageRemoteObject(this, target)

  def asynchronousInvoker(target: String = "") = new AsynchronousRemoteObject(this, target)

  def blockingInvoker(target: String = "", timeout: Duration = 10 second) = new BlockingRemoteObject(this, target, timeout = timeout)

  def close = {
    try {
      this.unregisterAsService()
    } catch {
      case e: Throwable => //println(e.getMessage)
    }
    channel.close
  }

  channel.onRequest set Some((message) => invoker.invoke(message))

  def selectDynamic(name: String): BlockingRemoteObject = new BlockingRemoteObject(this, name)

  def applyDynamic(name: String)(args: Any*): Any = new BlockingRemoteObject(this).applyDynamic(name)(args)

  //  def applyDynamicNamed(name: String)(args: (String, Any)*): Future[Any] = new InvokeItem(client, remoteName, remoteID, name)(args: _*).sendMessage
}

abstract class MessageChannel {
  //TODO here is an potential overflow point
  private val waitingMap: HashMap[Long, (FutureEntry, Runnable)] = new HashMap

  private class FutureEntry(var result: Option[Any] = None, var cause: Option[Throwable] = None)

  private[core] val onRequest = new AtomicReference[Option[(Message) => Message]](None)
  private val dynamicInvokerExecutor = Executors.newCachedThreadPool

  def future(message: Message): Future[Any] = {
    val futureEntry = new FutureEntry
    val singleLatch = new AtomicInteger(0)
    Future[Any] {
      if (singleLatch.getAndIncrement == 0) {
        if (futureEntry.result.isDefined) futureEntry.result.get
        else if (futureEntry.cause.isDefined) throw futureEntry.cause.get
        else throw new RuntimeException("Error state: FutureEntry not defined.")
      }
    }(new ExecutionContext {
      def execute(runnable: Runnable): Unit = {
        SingleThreadExecutionContext.execute(() => {
          waitingMap.synchronized {
            if (waitingMap.contains(message.messageID)) throw new IllegalArgumentException("MessageID have been used.")
            waitingMap.put(message.messageID, (futureEntry, runnable))
          }
          try {
            sendMessage(message)
          } catch {
            case e: Throwable => {
              futureEntry.cause = Some(e)
              runnable.run
            }
          }
        })
      }

      def reportFailure(cause: Throwable): Unit = {
        println(s"nn:$cause")
      }
    })
  }

  def messageReceived(message: Message) = {
    message.messageType match {
      case Request => request(message)
      case Response => response(message)
      case Error => error(message)
      case _ => throw new MessageException(s"An unknown Message received: $message")
    }
  }

  private def request(request: Message) {
    val function = onRequest.get match {
      case None => throw new MessageException("Invalid client, can not handler Request.")
      case Some(func) => func
    }
    dynamicInvokerExecutor.submit(new Runnable {
      override def run {
        try {
          val result = function(request)
          request.get[Boolean](Message.KeyNoResponse) match {
            case Some(x) if x == true =>
            case _ => sendMessage(result)
          }
        } catch {
          case e: Throwable => sendMessage(request.error({
            e.getCause match {
              case null => e
              case cause => cause
            }
          }.getMessage))
        }
      }
    })
  }

  private def response(response: Message): Unit = {
    val (responseItem, id) = response.responseContent
    waitingMap.synchronized {
      waitingMap.get(id) match {
        case None => println(s"ResponseID not recgonized: $response")
        case Some((futureEntry, runnable)) => {
          futureEntry.result = Some(responseItem)
          runnable.run
        }
      }
    }
  }

  private def error(error: Message) {
    val (errorItem, id) = error.errorContent
    waitingMap.synchronized {
      waitingMap.get(id) match {
        case None => println(s"ResponseID not recgonized: $error")
        case Some((futureEntry, runnable)) => {
          futureEntry.cause = Some(new MessageException(errorItem.toString))
          runnable.run
        }
      }
    }
  }

  def sendMessage(message: Message): Unit

  def close: Unit = {}
}

class LocalStatelessMessageChannel(service: StatelessMessageService, encoding: String = "MSGPACK") extends MessageChannel {
  private val machineID = MessageService.generateRandomString(15)
  private val token = new AtomicReference[Option[String]](None)
  private val closed = new AtomicBoolean(false)
  private val fetchThreadPool = Executors.newScheduledThreadPool(50)

  fetchThreadPool.scheduleWithFixedDelay(() => if (token.get.isDefined) {
    val future = service.fetchNewMessage(token.get.get)
    val messageOption = Await.result(future, 1 minute)
    messageOption.foreach(messageReceived)
  }, 1, 1, TimeUnit.SECONDS)

  def sendMessage(message: Message): Unit = {
    val enAndDecoder = encoding match {
      case "MSGPACK" => (new MessagePacker(), new MessageGenerator())
      case "JSON" => (new MessageJsonPacker(), new MessageJsonGenerator())
      case _ => throw new UnsupportedOperationException
    }
    val bytes = enAndDecoder._1.feed(message).pack()
    enAndDecoder._2.feed(ByteBuffer.wrap(bytes))
    val convertedMessage = enAndDecoder._2.next().get
    val newToken = service.messageDispatch(convertedMessage, new StatelessSessionProperties(machineID, token.get))
    if (!token.get.isDefined) {
      token set Some(newToken)
    }
  }

  override def close: Unit = {
    super.close
    closed set true
    fetchThreadPool.shutdown()
  }
}

class HttpMessageChannel(url: String, encoding: String = "MSGPACK") extends MessageChannel {

  //  implicit val system = ActorSystem()
  //  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = SingleThreadExecutionContext
  private val token = new AtomicReference[Option[String]](None)
  //  private val contentType = ContentType(MediaType.customWithFixedCharset("application", encoding.toLowerCase(), HttpCharsets.`UTF-8`))
  private val closed = new AtomicBoolean(false)
  private val fetchExecutor = Executors.newSingleThreadExecutor((runnable) => {
    val thread = new Thread(runnable)
    thread.setDaemon(true)
    thread
  })
  private val sendMessageExecutor = Executors.newCachedThreadPool((runnable) => {
    val thread = new Thread(runnable)
    thread.setDaemon(true)
    thread
  })

  private def startFetchLoop: Unit = {
    fetchExecutor.submit(new Runnable {
      override def run(): Unit = {
        while (!closed.get) {
          try {
            if (!makeHttpRequest(None)) Thread.sleep(1000)
          }
          catch {
            case e: Throwable => {
              e.printStackTrace()
              Thread.sleep(1000)
            }
          }
        }
      }
    })
  }

  def sendMessage(message: Message) = sendMessageExecutor.submit(new Runnable {
    override def run(): Unit = makeHttpRequest(Some(message))
  })

  private def makeHttpRequest(messageOption: Option[Message]): Boolean = {
    val bytes = messageOption match {
      case None => new Array[Byte](0)
      case Some(message) => {
        encoding match {
          case "MSGPACK" => new MessagePacker()
          case "JSON" => new MessageJsonPacker()
          case _ => throw new UnsupportedOperationException
        }
      }.feed(message).pack()
    }
    val connection = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setRequestProperty("Content-Type", s"application/${encoding.toLowerCase()}")
    connection.setDoInput(true)
    if (token.get isDefined) connection.setRequestProperty("Cookie", s"HydraToken=${token.get.get}")
    if (bytes.size > 0) {
      connection.setDoOutput(true)
      val out = connection.getOutputStream
      out.write(bytes)
      out.close()
    }
    connection.getResponseCode match {
      case 200 => {
        connection.getHeaderField("Set-Cookie") match {
          case null =>
          case setCookie => {
            setCookie.split("; *").filter(_.startsWith("HydraToken=")).foreach(c => {
              val newToken = c.substring("HydraToken=".size)
              if (token.get.isEmpty) startFetchLoop
              token set Some(newToken)
            })
          }
        }
        val contentLength = connection.getHeaderField("Content-Length") match {
          case null => 0
          case l => l.toInt
        }
        if (contentLength > 10e6) throw new IllegalArgumentException("Response too large.")
        if (contentLength > 0) {
          val decoder = encoding match {
            case "MSGPACK" => new MessageGenerator()
            case "JSON" => new MessageJsonGenerator()
            case _ => throw new UnsupportedOperationException
          }
          val in = connection.getInputStream
          val readBytes = in.readNBytes(contentLength)
          in.close()
          decoder.feed(ByteBuffer.wrap(readBytes))
          decoder.next() match {
            case Some(responseMessage) => {
              messageReceived(responseMessage)
            }
            case None =>
          }
        }
        true
      }
      case rc => {
        println(s"Invalid response: ${rc}")
        false
      }
    }
  }

  //  private def makeHttpRequest(messageOption: Option[Message]) = {
  //    val bytes = messageOption match {
  //      case None => new Array[Byte](0)
  //      case Some(message) => {
  //        println(s"sending: ${message}")
  //        encoding match {
  //          case "MSGPACK" => new MessagePacker()
  //          case "JSON" => new MessageJsonPacker()
  //          case _ => throw new UnsupportedOperationException
  //        }
  //      }.feed(message).pack()
  //    }
  //    val requestPre = HttpRequest(
  //      method = HttpMethods.POST,
  //      uri = url,
  //      entity = HttpEntity(contentType, bytes)
  //    )
  //    val request = token get match {
  //      case None => requestPre
  //      case Some(tk) => requestPre.withHeaders(List(akka.http.scaladsl.model.headers.Cookie("HydraToken", tk)))
  //    }
  //    val response = Http().singleRequest(request)
  //    println("req ed")
  //    response onComplete {
  //      case Success(response) => response.status match {
  //        case StatusCodes.OK => {
  //          println(s"OK for ${}")
  //          val optionalCookie = response.getHeader("Set-Cookie")
  //          if (optionalCookie.isPresent) {
  //            val cookie = optionalCookie.get.asInstanceOf[akka.http.scaladsl.model.headers.`Set-Cookie`].cookie
  //            if (cookie.name == "HydraToken") {
  //              if (token.get.isEmpty) startFetchLoop
  //              token set Some(cookie.value)
  //            }
  //          }
  //          val contentLength = response.entity.contentLengthOption.get
  //          if (contentLength > 10e6) throw new IllegalArgumentException("Response too large.")
  //          if (contentLength > 0) {
  //            val decoder = encoding match {
  //              case "MSGPACK" => new MessageGenerator()
  //              case "JSON" => new MessageJsonGenerator()
  //              case _ => throw new UnsupportedOperationException
  //            }
  //            response.entity.dataBytes.runForeach(bs => {
  //              try {
  //                decoder.feed(bs.asByteBuffer)
  //                decoder.next() match {
  //                  case Some(responseMessage) => {
  //                    println(s"message got: $responseMessage")
  //                    messageReceived(responseMessage)
  //                  }
  //                  case None =>
  //                }
  //              }
  //              catch {
  //                case e: Throwable => e.printStackTrace()
  //              }
  //            })
  //          }
  //        }
  //        case _ => println(s"Invalid response: ${response.status}")
  //      }
  //      case Failure(exception) => exception.printStackTrace()
  //    }
  //    response
  //  }

  override def close: Unit = {
    closed set true
    super.close
    fetchExecutor.shutdown()
    sendMessageExecutor.shutdown()
  }
}

private object SingleThreadExecutionContext extends ExecutionContext with UncaughtExceptionHandler {
  private lazy val SingleThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory {
    private lazy val ThreadCount = new AtomicInteger(0)

    def newThread(runnable: Runnable): Thread = {
      val t = new Thread(runnable)
      t.setDaemon(true)
      t.setName(s"SingleThreadExecutionContextThread-${ThreadCount.getAndIncrement}")
      t.setUncaughtExceptionHandler(SingleThreadExecutionContext.this)
      t.setPriority(Thread.NORM_PRIORITY)
      t
    }
  })

  def execute(runnable: Runnable): Unit = {
    SingleThreadPool.submit(runnable).get
  }

  def reportFailure(cause: Throwable): Unit = {
    cause.printStackTrace()
  }

  def uncaughtException(thread: Thread, cause: Throwable) {
    cause.printStackTrace()
  }
}