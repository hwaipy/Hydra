package com.hydra.io

import com.hydra.core.Message
import com.hydra.core.MessageGenerator
import com.hydra.core.MessagePacker
import com.hydra.core.MessageBuilder
import com.hydra.core.RuntimeInvoker
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleState
import io.netty.util.AttributeKey
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Thread.UncaughtExceptionHandler
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ConcurrentHashMap
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicLong
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.language.dynamics
import scala.language.postfixOps

object MessageTransport {
  //TODO: The configuration is not safe here. Need a immutable one.
  lazy val Configuration = {
    val properties = new Properties
    val propertiesFile = new File("Sydra.properties")
    try {
      val propertiesIn = new FileInputStream(propertiesFile)
      properties.load(propertiesIn)
      propertiesIn.close
    } catch {
      case ex: FileNotFoundException => {
        println("Properties file not found..")
        System.exit(11)
      }
      case ex: IOException => {
        println("Exception in reading properties file.")
        System.exit(12)
      }
    }
    System.setProperty("log4j.configurationFile", properties.getProperty("log4j.configurationFile", "./config/log4j.xml"))
    properties
  }
  lazy val Logger = LoggerFactory.getLogger("MessageTransport")
  implicit lazy val FutureDirect: ExecutionContext = SingleThreadExecutionContext
}

private object SingleThreadExecutionContext extends ExecutionContext with UncaughtExceptionHandler {
  private lazy val SingleThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
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
    MessageTransport.Logger.error("Failure appeared in get Future.", cause)
  }

  def uncaughtException(thread: Thread, cause: Throwable) {
    MessageTransport.Logger.error("Failure appeared in SingleThreadExecutionContext.", cause)
  }
}

class MessageServer(port: Int) {
  protected lazy val bossGroup: EventLoopGroup = new NioEventLoopGroup
  protected lazy val workerGroup: EventLoopGroup = new NioEventLoopGroup

  def start = {
    println("start the server")
    val server = new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ChannelInitializer[SocketChannel]() {
        override def initChannel(ch: SocketChannel) {
          val handler = new MessageServerHandler(ctx => new MessageServerInvokeHandler(ctx))
          ch.pipeline
            .addLast(new IdleStateHandler(20, 0, 0))
            .addLast(new MessagePackDecoder)
            .addLast(new MessagePackEncoder)
            .addLast(handler)
        }
      })
      .option(ChannelOption.SO_BACKLOG.asInstanceOf[ChannelOption[Any]], 128)
      .childOption(ChannelOption.SO_KEEPALIVE.asInstanceOf[ChannelOption[Any]], false)
      .bind(port)
    MessageTransport.Logger.info(s"Hydra Server started on port $port.")
    server
  }

  def stop = {
    MessageTransport.Logger.info("Hydra Server stopping.")
    workerGroup.shutdownGracefully
    bossGroup.shutdownGracefully
    MessageTransport.Logger.info("Hydra Server stopped.")
  }
}

class MessageClient(val name: String, host: String, port: Int, invokeHandler: Any = None, autoReconnect: Boolean = false) {

  import com.hydra.core.MessageType._

  private lazy val workerGroup: EventLoopGroup = new NioEventLoopGroup
  private lazy val handler = new MessageClientHandler(invokeHandler, this)
  private lazy val channelFuture = new Bootstrap()
    .group(workerGroup)
    .channel(classOf[NioSocketChannel])
    .option(ChannelOption.SO_KEEPALIVE.asInstanceOf[ChannelOption[Any]], true)
    .handler(new ChannelInitializer[SocketChannel]() {
      override def initChannel(ch: SocketChannel) {
        ch.pipeline()
          .addLast(new IdleStateHandler(0, 10, 0))
          .addLast(new MessagePackEncoder)
          .addLast(new MessagePackDecoder)
          .addLast(handler)
      }
    })
    .connect(host, port)
  private lazy val channel = channelFuture.channel.asInstanceOf[SocketChannel]

  def start = channelFuture

  def stop = workerGroup.shutdownGracefully

  def toMessageInvoker(target: String = "") = new MessageRemoteObject(this, target)

  def asynchronousInvoker(target: String = "") = new AsynchronousRemoteObject(this, target)

  def blockingInvoker(target: String = "", timeout: Duration = 2 second) = new BlockingRemoteObject(this, target, timeout = timeout)

  def sendMessage(msg: Message) = {
    //    println(s"Send message $msg")
    require(msg.messageType == Request)
    handler.future(channel, msg, msg.messageID)
  }

  def requestMessage(msg: Message, timeout: Duration) = {
    val future = this.sendMessage(msg);
    Await.result[Any](future, timeout)
  }

  def connect() = this.asynchronousInvoker().connect(name)

  private val sessionListeners: collection.concurrent.Map[SessionListener, Int] = new ConcurrentHashMap[SessionListener, Int]().asScala

  def addSessionListener(listener: SessionListener) {
    sessionListeners.put(listener, 0)
  }

  def removeSessionListener(listener: SessionListener) {
    sessionListeners.remove(listener)
  }

  protected[io] def fireSessionConnected(session: String) {
    sessionListeners.keys.foreach(l => l.sessionConnected(session))
  }

  protected[io] def fireSessionDisconnected(session: String) {
    sessionListeners.keys.foreach(l => l.sessionDisconnected(session))
  }
}

object MessageClient {
  def newClient(host: String, port: Int, name: String = "", invokeHandler: Any = None, timeout: Duration = 2 second) = {
    val client = new MessageClient(name, host, port, invokeHandler)
    val f = client.start
    f.await(timeout.toMillis)
    if (f.isDone || f.isSuccess) {
      client.blockingInvoker().connect(name)
      client
    } else {
      //      println(f)
      f.cause.printStackTrace
      throw new RuntimeException(f.cause)
    }
  }
}

class RemoteInvokeException(item: Any) extends Exception(item.toString) {
}

protected class MessagePackEncoder extends MessageToByteEncoder[Message] {
  override def encode(ctx: ChannelHandlerContext, msg: Message, out: ByteBuf) = {
    try {
      val packer = ctx.channel.attr(AttributeKey.valueOf[(Any, Option[String]) => RemoteObject]("Flatter")).get match {
        case null => new MessagePacker
        case flatter => new MessagePacker(flatter)
      }
      val pack = packer.feed(msg.content, msg.to).pack
      ctx.channel.attr[MessageSession](MessageServerHandler.KeySession).get match {
        case null =>
        case session => session.increseMessageSendStatistics(pack.size)
      }
      out.writeBytes(pack)
    } catch {
      case e: Throwable => println(e)
    }
  }
}

//protected class MessageDecoder extends ByteToMessageDecoder {
//  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: java.util.List[Object]) {
//    val generatorAttr = ctx.channel.attr(AttributeKey.valueOf[MessageGenerator]("Generator"))
//
//  }
//}

protected class MessagePackDecoder extends ByteToMessageDecoder {

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: java.util.List[Object]) {
    val generatorAttr = ctx.channel.attr(AttributeKey.valueOf[MessageGenerator]("Generator"))
    val generator = generatorAttr.get match {
      case null => {
        val shapper = ctx.channel.attr(AttributeKey.valueOf[(String, Long) => RemoteObject]("Shapper")).get
        val g = new MessageGenerator(shapper)
        generatorAttr.set(g)
        g
      }
      case g => g
    }
    if (in.readableBytes == 0) return
    in.nioBuffers.foreach(buffer => {
      in.skipBytes(buffer.remaining)
      generator.feed(buffer)
    })
    while (true) {
      generator.next match {
        case Some(x) => {
          out.add(x)
        }
        case _ => {
          ctx.channel.attr[MessageSession](MessageServerHandler.KeySession).get match {
            case null =>
            case session => {
              val sta = generator.getStatistics
              session.updateMessageReceivedStatistics(sta._1, sta._2)
            }
          }
          return
        }
      }
    }
  }
}

protected class MessageTransportHandler extends ChannelInboundHandlerAdapter {

  import com.hydra.core.MessageType._

  override def channelRead(ctx: ChannelHandlerContext, msg: Object) {
    msg match {
      case m: Message => m.messageType match {
        case Request => request(m, ctx)
        case Response => response(m, ctx)
        case Error => error(m, ctx)
        case _ => MessageTransport.Logger.warn("An unknown Message received.", m)
      }
      case _ => throw new RuntimeException("Message object is not a class[Message].")
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    MessageTransport.Logger.warn(s"Exception caught, $ctx is about to close.", cause);
    ctx.close
  }

  protected def request(request: Message, ctx: ChannelHandlerContext) {}

  protected def response(response: Message, ctx: ChannelHandlerContext) {}

  protected def error(error: Message, ctx: ChannelHandlerContext) {}
}

protected class MessageServerHandler(factory: ChannelHandlerContext => Any) extends MessageTransportHandler {
  private val invokerMap: HashMap[ChannelHandlerContext, RuntimeInvoker] = new HashMap

  private def getRuntimeInvoker(ctx: ChannelHandlerContext) = invokerMap.getOrElseUpdate(ctx, new RuntimeInvoker(factory(ctx)))

  override def channelActive(ctx: ChannelHandlerContext) {
    MessageTransport.Logger.info(s"A new connection actived: $ctx")

    def flatter(obj: Any, target: Option[String]): RemoteObject = {
      obj match {
        case ro: RemoteObject => ro
        case o => throw new RuntimeException(s"Object Type $obj.getClass not recgonized.")
      }
    }

    def shapper(name: String, id: Long): RemoteObject = RemoteObject(name, id)

    ctx.channel.attr(AttributeKey.valueOf[(Any, Option[String]) => RemoteObject]("Flatter")).set(flatter)
    ctx.channel.attr(AttributeKey.valueOf[(String, Long) => RemoteObject]("Shapper")).set(shapper)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Object) {
    msg match {
      case m: Message => {
        MessageTransport.Logger.info(s"New message read from $ctx: $m")
        m.to match {
          case None => super.channelRead(ctx, msg)
          case Some(to) => {
            val session = ctx.channel.attr[MessageSession](MessageServerHandler.KeySession).get
            if (session == null) {
              writeAndFlush(ctx, m.error("Client has not connected."))
            } else {
              MessageSession.getSession(to) match {
                case None => writeAndFlush(ctx, m.error(s"Target $to does not exists."))
                case Some(toSession) => {
                  val m2 = m.builder.+=(Message.KeyFrom -> session.name).create
                  toSession.writeAndFlush(m2)
                  dealWithForwardedRemoteObject(ctx, m, session, toSession)
                }
              }
            }
          }
        }
      }
      case _ => throw new RuntimeException("Message object is not a class[Message].")
    }
  }

  override def request(request: Message, ctx: ChannelHandlerContext) {
    try {
      val r = getRuntimeInvoker(ctx).invoke(request)
      writeAndFlush(ctx, r)
    } catch {
      case e: Throwable if (e.isInstanceOf[IllegalArgumentException] || e.isInstanceOf[IllegalStateException]) =>
        writeAndFlush(ctx, request.error(e.getMessage))
      case e: InvocationTargetException => writeAndFlush(ctx, request.error(e.getCause.getMessage))
      case e: Throwable => throw e
    }
  }

  override def userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
    evt match {
      case e: IdleStateEvent if e.state == IdleState.READER_IDLE => ctx.close
    }
  }

  override def channelInactive(ctx: ChannelHandlerContext) {
    MessageTransport.Logger.info(s"A connection is about to close. $ctx")
    ctx.channel.attr[MessageSession](MessageServerHandler.KeySession).get match {
      case null =>
      case session => {
        session.close
        MessageSession.getSessions.foreach(s => s.writeAndFlush(MessageBuilder.newBuilder.asRequest("remoteClientDisconnected", session.name :: Nil).to(s.name).objectID(-1).+=(Message.KeyNoResponse, true).create))
      }
    }
  }

  def dealWithForwardedRemoteObject(ctx: ChannelHandlerContext, msg: Message, fromSession: MessageSession, toSession: MessageSession) {
    def deal(obj: Any) {
      obj match {
        case list: List[_] => list.foreach(deal)
        case map: Map[_, _] => map.foreach(entry => {
          deal(entry._1)
          deal(entry._2)
        })
        case ro: RemoteObject if ro.remoteName != fromSession.name => {
          val msg = MessageBuilder.newBuilder.asRequest("remoteObjectDistributed", ro.remoteID :: toSession.name :: Nil).to(ro.remoteName).objectID(-1).+=(Message.KeyNoResponse, true).create
          MessageSession.getSession(ro.remoteName) match {
            case Some(session) => {
              toSession.writeAndFlush(msg)
            }
            case _ =>
          }
        }
        case _ =>
      }
    }

    deal(msg.content)
  }

  def writeAndFlush(ctx: ChannelHandlerContext, message: Any) {
    MessageTransport.Logger.info(s"New message send to $ctx: $message")
    ctx.writeAndFlush(message)
  }
}

protected object MessageServerHandler {
  val KeySession: AttributeKey[MessageSession] = AttributeKey.valueOf("MesssageSession")
}

private class MessageServerInvokeHandler(ctx: ChannelHandlerContext) {
  private var session: Option[MessageSession] = None

  def connect(name: String) {
    session match {
      case None => {
        val sessions = MessageSession.getSessions
        val sessionName = name match {
          case "" => s"AnanonymousClient_${MessageServerInvokeHandler.ananonymousClientID.getAndIncrement}"
          case n => n
        }
        session = Some(MessageSession.create(sessionName, ctx))
        ctx.channel.attr[MessageSession](MessageServerHandler.KeySession).set(session.get)
        sessions.foreach(s => s.writeAndFlush(MessageBuilder.newBuilder.asRequest("remoteClientConnected", session.get.name :: Nil).to(s.name).objectID(-1).+=(Message.KeyNoResponse, true).create))
      }
      case _ => throw new IllegalStateException(s"Session name already assignged.")
    }
  }

  def ping() = "ping"

  def sessionsInformation() = {
    val sessions = MessageSession.getSessions
    sessions.map(session => session.summary)
  }
}

private object MessageServerInvokeHandler {
  private val ananonymousClientID = new AtomicLong(10)
}

trait SessionListener {
  def sessionConnected(name: String)

  def sessionDisconnected(name: String)
}

protected class MessageClientHandler(defaultInvoker: Any, client: MessageClient) extends MessageTransportHandler {
  private val waitingMap: HashMap[Long, (FutureEntry, Runnable)] = new HashMap
  private val remoteReferenceMap = HashMap[Any, Long]()
  private val remoteReferenceKeyMap = HashMap[Long, Tuple3[Any, RuntimeInvoker, HashMap[String, AtomicInteger]]]()
  private val remoteReferenceID = new AtomicLong(-1)
  private val dynamicInvokerExecutor = Executors.newCachedThreadPool

  class MessageClientSystemLevelHandler {
    def remoteClientConnected(remoteClientName: String) {
      client.fireSessionConnected(remoteClientName)
    }

    def remoteClientDisconnected(remoteClientName: String) {
      MessageClientHandler.this.remoteObjectFinalized(None, remoteClientName)
      client.fireSessionDisconnected(remoteClientName)
    }

    def remoteObjectDistributed(remoteObjectID: Long, distributedClient: String) {
      MessageClientHandler.this.remoteObjectDistributed(remoteObjectID, distributedClient)
    }

    def remoteObjectFinalized(remoteObjectID: Long, finalizedClient: String) {
      MessageClientHandler.this.remoteObjectFinalized(Some(remoteObjectID), finalizedClient)
    }
  }

  instanceRemoteObject(new MessageClientSystemLevelHandler)
  instanceRemoteObject(defaultInvoker)

  private def instanceRemoteObject(obj: Any, target: Option[String] = None) = {
    remoteReferenceMap.synchronized {
      val ro =
        RemoteObject(client.name, remoteReferenceMap.getOrElseUpdate(obj, {
          val id = remoteReferenceID.getAndIncrement
          remoteReferenceMap.put(obj, id)
          remoteReferenceKeyMap.put(id, (obj, new RuntimeInvoker(obj), HashMap()))
          id
        }))
      target match {
        case None =>
        case Some(targetString) => remoteObjectDistributed(ro.remoteID, targetString)
      }
      ro
    }
  }

  private def remoteObjectDistributed(objectID: Long, target: String) {
    remoteReferenceMap.synchronized {
      remoteReferenceKeyMap.get(objectID) match {
        case Some(v) => v._3.getOrElseUpdate(target, new AtomicInteger(0)).getAndIncrement
        case _ =>
      }
    }
  }

  private def remoteObjectFinalized(objectID: Option[Long], target: String) {
    remoteReferenceMap.synchronized {
      objectID match {
        case None => remoteReferenceKeyMap.keys.filter(key => key > 0).foreach(key => fin(key, target))
        case Some(id) => fin(id, target)
      }

      def fin(id: Long, target: String) {
        remoteReferenceKeyMap.get(id) match {
          case None =>
          case Some(value) => {
            val map = value._3
            map.get(target) match {
              case None =>
              case Some(i) => i.decrementAndGet match {
                case 0 => map.remove(target)
                case _ =>
              }
            }
            if (map.isEmpty) {
              remoteReferenceKeyMap.remove(id)
              remoteReferenceMap.remove(value._1)
            }
          }
        }
      }
    }
  }

  private class FutureEntry(var result: Option[Any] = None, var cause: Option[Throwable] = None)

  def future(channel: SocketChannel, msg: Message, id: Long) = {
    val futureEntry = new FutureEntry
    val singleLatch = new AtomicInteger(0)
    var channelFuture: ChannelFuture = null
    Future[Any] {
      if (singleLatch.getAndIncrement == 0) {
        if (!channelFuture.isDone) throw new RuntimeException(s"ChannelFuture not done: $channelFuture")
        if (!channelFuture.isSuccess) throw new RuntimeException(s"ChannelFuture failed: $channelFuture")
        if (futureEntry.result.isDefined) futureEntry.result.get
        else if (futureEntry.cause.isDefined) throw futureEntry.cause.get
        else throw new RuntimeException("Error state: FutureEntry not defined.")
      }
    }(new ExecutionContext {
      def execute(runnable: Runnable): Unit = {
        SingleThreadExecutionContext.execute(new Runnable() {
          override def run() {
            waitingMap.synchronized {
              if (waitingMap.contains(id)) throw new IllegalArgumentException("MessageID have been used.")
              waitingMap.put(id, (futureEntry, runnable))
            }
            channelFuture = channel.writeAndFlush(msg)
            try {
              channelFuture.addListener(new ChannelFutureListener() {
                override def operationComplete(future: ChannelFuture) {
                  if (!future.isSuccess) {
                    futureEntry.cause = Some(future.cause)
                    runnable.run
                  }
                }
              })
            } catch {
              case e: Throwable => e.printStackTrace
            }
          }
        })
      }

      def reportFailure(cause: Throwable): Unit = {
        println(s"nn:$cause")
      }
    })
  }

  override protected def request(request: Message, ctx: ChannelHandlerContext) {
    //    println(s"Read message $request")
    try {
      val objectID = request.objectID match {
        case None => 0
        case Some(id) => id
      }
      val invoker = remoteReferenceKeyMap.get(objectID) match {
        case None => throw new IllegalArgumentException(s"ObjectID $objectID not exists.")
        case Some(inv) => inv._2
      }
      dynamicInvokerExecutor.submit(new Runnable {
        override def run {
          try {
            val r = invoker.invoke(request)
            request.get[Boolean](Message.KeyNoResponse) match {
              case Some(x) if x == true =>
              case _ => ctx.writeAndFlush(r)
            }
          } catch {
            case e: Throwable if (e.isInstanceOf[IllegalArgumentException] || e.isInstanceOf[IllegalStateException]) => ctx.writeAndFlush(request.error(e.getMessage))
            case e: InvocationTargetException => ctx.writeAndFlush(request.error(e.getCause.getMessage))
            case e: Throwable =>
          }
        }
      })
    } catch {
      case e: Throwable if (e.isInstanceOf[IllegalArgumentException] || e.isInstanceOf[IllegalStateException]) => ctx.writeAndFlush(request.error(e.getMessage))
      case e: InvocationTargetException => ctx.writeAndFlush(request.error(e.getCause.getMessage))
      case e: Throwable =>
    }
  }

  override protected def response(response: Message, ctx: ChannelHandlerContext) {
    val (responseItem, id) = response.responseContent
    waitingMap.synchronized {
      waitingMap.get(id) match {
        case None => MessageTransport.Logger.info(s"ResponseID not recgonized: $response")
        case Some((futureEntry, runnable)) => {
          futureEntry.result = Some(responseItem)
          runnable.run
        }
      }
    }
  }

  override protected def error(error: Message, ctx: ChannelHandlerContext) {
    val (errorItem, id) = error.errorContent
    waitingMap.synchronized {
      waitingMap.get(id) match {
        case None => MessageTransport.Logger.info(s"ResponseID not recgonized: $error")
        case Some((futureEntry, runnable)) => {
          futureEntry.cause = Some(new RemoteInvokeException(errorItem))
          runnable.run
        }
      }
    }
  }

  override def userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
    evt match {
      case e: IdleStateEvent if e.state == IdleState.WRITER_IDLE => {
        client.asynchronousInvoker().ping
      }
    }
  }

  override def channelActive(ctx: ChannelHandlerContext) {
    def flatter(obj: Any, target: Option[String]): RemoteObject = {
      obj match {
        case ro: RemoteObject => {
          target match {
            case None =>
            case Some(targetString) => ro.remoteName match {
              case client.name => remoteObjectDistributed(ro.remoteID, targetString)
              case _ =>
              //val msg = MessageBuilder.newBuilder.asRequest("remoteObjectDistributed", ro.remoteID :: targetString :: Nil).to(ro.remoteName).objectID(-1).+=(Message.KeyNoResponse, true).create
              //client.sendMessage(msg)
            }
          }
          ro
        }
        case o => instanceRemoteObject(obj, target)
      }
    }

    def shapper(name: String, id: Long): RemoteObject = RemoteObject(client, name, id)

    ctx.channel.attr(AttributeKey.valueOf[(Any, Option[String]) => RemoteObject]("Flatter")).set(flatter)
    ctx.channel.attr(AttributeKey.valueOf[(String, Long) => RemoteObject]("Shapper")).set(shapper)
  }

  override def channelInactive(ctx: ChannelHandlerContext) {
  }
}

class MessageSession private(val id: Int, val name: String, private val ctx: ChannelHandlerContext) {
  private val connectedTime = System.currentTimeMillis
  private val messageReceived = new AtomicInteger(0)
  private val messageSend = new AtomicInteger(0)
  private val bytesReceived = new AtomicLong(0)
  private val bytesSend = new AtomicLong(0)

  def close {
    MessageSession.unregisterSession(this)
  }

  def writeAndFlush(msg: Message) = {
    MessageTransport.Logger.info(s"New message is send to $ctx: $msg")
    ctx.writeAndFlush(msg)
  }

  override def toString = s"MessageSession[$id, $name]"

  def updateMessageReceivedStatistics(messageReceived: Int, bytesReceived: Long) {
    this.messageReceived.set(messageReceived)
    this.bytesReceived.set(bytesReceived)
  }

  def increseMessageSendStatistics(bytesSend: Long) {
    this.messageSend.incrementAndGet
    this.bytesSend.addAndGet(bytesSend)
  }

  def summary = (id, name, connectedTime, messageSend.get, messageReceived.get, bytesSend.get, bytesReceived.get)
}

object MessageSession {
  private val SessionIDs = new AtomicInteger(0)
  private val SessionsByID: HashMap[Int, MessageSession] = new HashMap()
  private val SessionsByName: HashMap[String, MessageSession] = new HashMap()

  def create(name: String, context: ChannelHandlerContext) = {
    val session = new MessageSession(SessionIDs.getAndIncrement(), name, context)
    MessageTransport.Logger.info(s"${session} connected on context ${context}.")
    if (!registerSession(session)) throw new IllegalArgumentException(s"Session name $name duplicated.")
    session
  }

  private def registerSession(session: MessageSession) = {
    SessionsByName.synchronized {
      if (SessionsByName.contains(session.name)) false
      else {
        SessionsByID += session.id -> session
        SessionsByName += session.name -> session
        MessageTransport.Logger.trace(s"${session} registed. There are currently ${sessionsCount} clients.")
        true
      }
    }
  }

  private def unregisterSession(session: MessageSession) {
    SessionsByName.synchronized {
      SessionsByID.remove(session.id)
      SessionsByName.remove(session.name)
      MessageTransport.Logger.trace(s"${session} unregisted. There are currently ${sessionsCount} clients.")
    }
  }

  def getSession(sessionName: String) = SessionsByName.get(sessionName)

  def getSession(sessionID: Int) = SessionsByID.get(sessionID)

  def getSessions = SessionsByName.values.toList

  def sessionsCount = SessionsByName.size
}

object RemoteObject {
  def apply(remoteName: String, remoteID: Long) = new RemoteObject(remoteName, remoteID)

  def apply(client: MessageClient, remoteName: String, remoteID: Long) = new BlockingRemoteObject(client, remoteName, remoteID)
}

class RemoteObject protected(val remoteName: String, val remoteID: Long) {
  override def toString() = s"RemoteObject[$remoteName,$remoteID]"
}

protected abstract class DynamicRemoteObject(client: MessageClient, remoteName: String = "", remoteID: Long = 0, core: Option[RemoteObject] = None) extends RemoteObject(remoteName, remoteID) with Dynamic {
  def asMessageRemoteObject = new MessageRemoteObject(client, remoteName, remoteID, Some(this))

  def asBlockingRemoteObject(timeout: Duration = 2 second) = new BlockingRemoteObject(client, remoteName, remoteID, timeout, Some(this))

  def asAsynchronousRemoteObject = new AsynchronousRemoteObject(client, remoteName, remoteID, Some(this))

  override def finalize() {
    //    core match {
    //      case None if remoteName != "" && remoteID > 0 => try {
    //        client.sendMessage(MessageBuilder.newBuilder.asRequest("remoteObjectFinalized", remoteID :: client.name :: Nil).to(remoteName).objectID(-1).+=(Message.KeyNoResponse, true).create)
    //      } catch { case _: Throwable => }
    //      case _ =>
    //    }
  }
}

class MessageRemoteObject(client: MessageClient, remoteName: String = "", remoteID: Long = 0, core: Option[RemoteObject] = None) extends DynamicRemoteObject(client, remoteName, remoteID, core) {
  def selectDynamic(name: String): Message = new InvokeItem(client, remoteName, remoteID, name)().toMessage

  def applyDynamic(name: String)(args: Any*): Message = new InvokeItem(client, remoteName, remoteID, name)(args.map(m => ("", m)): _*).toMessage

  def applyDynamicNamed(name: String)(args: (String, Any)*): Message = new InvokeItem(client, remoteName, remoteID, name)(args: _*).toMessage
}

class BlockingRemoteObject(client: MessageClient, remoteName: String = "", remoteID: Long = 0, timeout: Duration = 2 second, core: Option[RemoteObject] = None) extends DynamicRemoteObject(client, remoteName, remoteID, core) {
  def selectDynamic(name: String): Any = new InvokeItem(client, remoteName, remoteID, name)().requestMessage(timeout)

  def applyDynamic(name: String)(args: Any*): Any = new InvokeItem(client, remoteName, remoteID, name)(args.map(m => ("", m)): _*).requestMessage(timeout)

  def applyDynamicNamed(name: String)(args: (String, Any)*): Any = new InvokeItem(client, remoteName, remoteID, name)(args: _*).requestMessage(timeout)
}

class AsynchronousRemoteObject(client: MessageClient, remoteName: String = "", remoteID: Long = 0, core: Option[RemoteObject] = None) extends DynamicRemoteObject(client, remoteName, remoteID, core) {
  def selectDynamic(name: String): Future[Any] = new InvokeItem(client, remoteName, remoteID, name)().sendMessage

  def applyDynamic(name: String)(args: Any*): Future[Any] = new InvokeItem(client, remoteName, remoteID, name)(args.map(m => ("", m)): _*).sendMessage

  def applyDynamicNamed(name: String)(args: (String, Any)*): Future[Any] = new InvokeItem(client, remoteName, remoteID, name)(args: _*).sendMessage
}

private class InvokeItem(client: MessageClient, target: String, id: Long = 0, name: String)(args: (String, Any)*) {
  val argsList: ArrayBuffer[Any] = new ArrayBuffer
  val namedArgsMap: HashMap[String, Any] = new HashMap
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
