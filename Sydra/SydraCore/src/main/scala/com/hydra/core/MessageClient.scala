package com.hydra.core

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import com.hydra.core.MessageType._
import scala.language.dynamics

protected abstract class DynamicRemoteObject(val client: MessageClient, val remoteName: String = "", val remoteID: Long = 0) extends Dynamic {
  //  def asMessageRemoteObject = new MessageRemoteObject(client, remoteName, remoteID, Some(this))
  //
  //  def asBlockingRemoteObject(timeout: Duration = 10 second) = new BlockingRemoteObject(client, remoteName, remoteID, timeout, Some(this))
  //
  //  def asAsynchronousRemoteObject = new AsynchronousRemoteObject(client, remoteName, remoteID, Some(this))

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


class MessageClient(channel: MessageChannel, serviceName: Option[String] = None, invokeHandler: Any = None) {
  def sendMessage(msg: Message) = {
    require(msg.messageType == Request)
    channel.future(msg)
  }

  def requestMessage(msg: Message, timeout: Duration) = {
    val future = this.sendMessage(msg)
    Await.result[Any](future, timeout)
  }

}

trait MessageChannel {
  def future(msg: Message): Future[Any]

  //  def future(channel: SocketChannel, msg: Message, id: Long) = {
  //    val futureEntry = new FutureEntry
  //    val singleLatch = new AtomicInteger(0)
  //    var channelFuture: ChannelFuture = null
  //    Future[Any] {
  //      if (singleLatch.getAndIncrement == 0) {
  //        if (!channelFuture.isDone) throw new RuntimeException(s"ChannelFuture not done: $channelFuture")
  //        if (!channelFuture.isSuccess) throw new RuntimeException(s"ChannelFuture failed: $channelFuture")
  //        if (futureEntry.result.isDefined) futureEntry.result.get
  //        else if (futureEntry.cause.isDefined) throw futureEntry.cause.get
  //        else throw new RuntimeException("Error state: FutureEntry not defined.")
  //      }
  //    }(new ExecutionContext {
  //      def execute(runnable: Runnable): Unit = {
  //        SingleThreadExecutionContext.execute(new Runnable() {
  //          override def run() {
  //            waitingMap.synchronized {
  //              if (waitingMap.contains(id)) throw new IllegalArgumentException("MessageID have been used.")
  //              waitingMap.put(id, (futureEntry, runnable))
  //            }
  //            channelFuture = channel.writeAndFlush(msg)
  //            try {
  //              channelFuture.addListener(new ChannelFutureListener() {
  //                override def operationComplete(future: ChannelFuture) {
  //                  if (!future.isSuccess) {
  //                    futureEntry.cause = Some(future.cause)
  //                    runnable.run
  //                  }
  //                }
  //              })
  //            } catch {
  //              case e: Throwable => e.printStackTrace
  //            }
  //          }
  //        })
  //      }
  //
  //      def reportFailure(cause: Throwable): Unit = {
  //        println(s"nn:$cause")
  //      }
  //    })
  //  }
}

//class MessageClient(val name: String, host: String, port: Int, invokeHandler: Any = None, autoReconnect: Boolean = false, protocol: String = MessageEncodingProtocol.PROTOCOL_MSGPACK) {
//
//  import com.hydra.core.MessageType._
//
//  private val handler = new MessageClientHandler(invokeHandler, this)
//
//  def start = channelFuture
//
//  def stop = workerGroup.shutdownGracefully
//
//  def toMessageInvoker(target: String = "") = new MessageRemoteObject(this, target)
//
//  def asynchronousInvoker(target: String = "") = new AsynchronousRemoteObject(this, target)
//
//  def blockingInvoker(target: String = "", timeout: Duration = 10 second) = new BlockingRemoteObject(this, target, timeout = timeout)
//
//  def connect() = this.asynchronousInvoker().connect(name)
//
//  private val sessionListeners: collection.concurrent.Map[SessionListener, Int] = new ConcurrentHashMap[SessionListener, Int]().asScala
//
//  def addSessionListener(listener: SessionListener) {
//    sessionListeners.put(listener, 0)
//  }
//
//  def removeSessionListener(listener: SessionListener) {
//    sessionListeners.remove(listener)
//  }
//
//  protected[io] def fireSessionConnected(session: String) {
//    sessionListeners.keys.foreach(l => l.sessionConnected(session))
//  }
//
//  protected[io] def fireSessionDisconnected(session: String) {
//    sessionListeners.keys.foreach(l => l.sessionDisconnected(session))
//  }
//}
//
//object MessageClient {
//  def newClient(host: String, port: Int, name: String = "", invokeHandler: Any = None, timeout: Duration = 10 second, protocol: String = MessageEncodingProtocol.PROTOCOL_MSGPACK) = {
//    val client = new MessageClient(name, host, port, invokeHandler, protocol = protocol)
//    try {
//      val f = client.start
//      f.await(timeout.toMillis)
//      if (f.isDone || f.isSuccess) {
//        client.blockingInvoker().connect(name)
//        client
//      } else {
//        client.stop
//        if (f.cause() == null) {
//          throw new RuntimeException("Timeout on start client.")
//        } else {
//          throw new RuntimeException(f.cause)
//        }
//      }
//    } catch {
//      case e: Throwable => client.stop; throw e
//    }
//  }
//}


//
//protected class MessageClientHandler(defaultInvoker: Any, client: MessageClient) extends MessageTransportHandler {
//  private val waitingMap: HashMap[Long, (FutureEntry, Runnable)] = new HashMap
//  private val remoteReferenceMap = HashMap[Any, Long]()
//  private val remoteReferenceKeyMap = HashMap[Long, Tuple3[Any, RuntimeInvoker, HashMap[String, AtomicInteger]]]()
//  private val remoteReferenceID = new AtomicLong(-1)
//  private val dynamicInvokerExecutor = Executors.newCachedThreadPool
//
//  class MessageClientSystemLevelHandler {
//    def remoteClientConnected(remoteClientName: String) {
//      client.fireSessionConnected(remoteClientName)
//    }
//
//    def remoteClientDisconnected(remoteClientName: String) {
//      MessageClientHandler.this.remoteObjectFinalized(None, remoteClientName)
//      client.fireSessionDisconnected(remoteClientName)
//    }
//
//    def remoteObjectDistributed(remoteObjectID: Long, distributedClient: String) {
//      MessageClientHandler.this.remoteObjectDistributed(remoteObjectID, distributedClient)
//    }
//
//    def remoteObjectFinalized(remoteObjectID: Long, finalizedClient: String) {
//      MessageClientHandler.this.remoteObjectFinalized(Some(remoteObjectID), finalizedClient)
//    }
//  }
//
//  instanceRemoteObject(new MessageClientSystemLevelHandler)
//  instanceRemoteObject(defaultInvoker)
//
//  private def instanceRemoteObject(obj: Any, target: Option[String] = None) = {
//    remoteReferenceMap.synchronized {
//      val ro =
//        RemoteObject(client.name, remoteReferenceMap.getOrElseUpdate(obj, {
//          val id = remoteReferenceID.getAndIncrement
//          remoteReferenceMap.put(obj, id)
//          remoteReferenceKeyMap.put(id, (obj, new RuntimeInvoker(obj), HashMap()))
//          id
//        }))
//      target match {
//        case None =>
//        case Some(targetString) => remoteObjectDistributed(ro.remoteID, targetString)
//      }
//      ro
//    }
//  }
//
//  private def remoteObjectDistributed(objectID: Long, target: String) {
//    remoteReferenceMap.synchronized {
//      remoteReferenceKeyMap.get(objectID) match {
//        case Some(v) => v._3.getOrElseUpdate(target, new AtomicInteger(0)).getAndIncrement
//        case _ =>
//      }
//    }
//  }
//
//  private def remoteObjectFinalized(objectID: Option[Long], target: String) {
//    remoteReferenceMap.synchronized {
//      objectID match {
//        case None => remoteReferenceKeyMap.keys.filter(key => key > 0).foreach(key => fin(key, target))
//        case Some(id) => fin(id, target)
//      }
//
//      def fin(id: Long, target: String) {
//        remoteReferenceKeyMap.get(id) match {
//          case None =>
//          case Some(value) => {
//            val map = value._3
//            map.get(target) match {
//              case None =>
//              case Some(i) => i.decrementAndGet match {
//                case 0 => map.remove(target)
//                case _ =>
//              }
//            }
//            if (map.isEmpty) {
//              remoteReferenceKeyMap.remove(id)
//              remoteReferenceMap.remove(value._1)
//            }
//          }
//        }
//      }
//    }
//  }
//
//  private class FutureEntry(var result: Option[Any] = None, var cause: Option[Throwable] = None)
//
//  def future(channel: SocketChannel, msg: Message, id: Long) = {
//    val futureEntry = new FutureEntry
//    val singleLatch = new AtomicInteger(0)
//    var channelFuture: ChannelFuture = null
//    Future[Any] {
//      if (singleLatch.getAndIncrement == 0) {
//        if (!channelFuture.isDone) throw new RuntimeException(s"ChannelFuture not done: $channelFuture")
//        if (!channelFuture.isSuccess) throw new RuntimeException(s"ChannelFuture failed: $channelFuture")
//        if (futureEntry.result.isDefined) futureEntry.result.get
//        else if (futureEntry.cause.isDefined) throw futureEntry.cause.get
//        else throw new RuntimeException("Error state: FutureEntry not defined.")
//      }
//    }(new ExecutionContext {
//      def execute(runnable: Runnable): Unit = {
//        SingleThreadExecutionContext.execute(new Runnable() {
//          override def run() {
//            waitingMap.synchronized {
//              if (waitingMap.contains(id)) throw new IllegalArgumentException("MessageID have been used.")
//              waitingMap.put(id, (futureEntry, runnable))
//            }
//            channelFuture = channel.writeAndFlush(msg)
//            try {
//              channelFuture.addListener(new ChannelFutureListener() {
//                override def operationComplete(future: ChannelFuture) {
//                  if (!future.isSuccess) {
//                    futureEntry.cause = Some(future.cause)
//                    runnable.run
//                  }
//                }
//              })
//            } catch {
//              case e: Throwable => e.printStackTrace
//            }
//          }
//        })
//      }
//
//      def reportFailure(cause: Throwable): Unit = {
//        println(s"nn:$cause")
//      }
//    })
//  }
//
//  override protected def request(request: Message, ctx: ChannelHandlerContext) {
//    //    println(s"Read message $request")
//    try {
//      val objectID = request.objectID match {
//        case None => 0
//        case Some(id) => id
//      }
//      val invoker = remoteReferenceKeyMap.get(objectID) match {
//        case None => throw new IllegalArgumentException(s"ObjectID $objectID not exists.")
//        case Some(inv) => inv._2
//      }
//      dynamicInvokerExecutor.submit(new Runnable {
//        override def run {
//          try {
//            val r = invoker.invoke(request)
//            request.get[Boolean](Message.KeyNoResponse) match {
//              case Some(x) if x == true =>
//              case _ => ctx.writeAndFlush(r)
//            }
//          } catch {
//            case e: Throwable if (e.isInstanceOf[IllegalArgumentException] || e.isInstanceOf[IllegalStateException]) => ctx.writeAndFlush(request.error(e.getMessage))
//            case e: InvocationTargetException => ctx.writeAndFlush(request.error(e.getCause.getMessage))
//            case e: Throwable =>
//          }
//        }
//      })
//    } catch {
//      case e: Throwable if (e.isInstanceOf[IllegalArgumentException] || e.isInstanceOf[IllegalStateException]) => ctx.writeAndFlush(request.error(e.getMessage))
//      case e: InvocationTargetException => ctx.writeAndFlush(request.error(e.getCause.getMessage))
//      case e: Throwable =>
//    }
//  }
//
//  override protected def response(response: Message, ctx: ChannelHandlerContext) {
//    val (responseItem, id) = response.responseContent
//    waitingMap.synchronized {
//      waitingMap.get(id) match {
//        case None => MessageTransport.Logger.info(s"ResponseID not recgonized: $response")
//        case Some((futureEntry, runnable)) => {
//          futureEntry.result = Some(responseItem)
//          runnable.run
//        }
//      }
//    }
//  }
//
//  override protected def error(error: Message, ctx: ChannelHandlerContext) {
//    val (errorItem, id) = error.errorContent
//    waitingMap.synchronized {
//      waitingMap.get(id) match {
//        case None => MessageTransport.Logger.info(s"ResponseID not recgonized: $error")
//        case Some((futureEntry, runnable)) => {
//          futureEntry.cause = Some(new RemoteInvokeException(errorItem))
//          runnable.run
//        }
//      }
//    }
//  }
//
//  override def userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
//    evt match {
//      case e: IdleStateEvent if e.state == IdleState.WRITER_IDLE => {
//        client.asynchronousInvoker().ping
//      }
//    }
//  }
//
//  override def channelActive(ctx: ChannelHandlerContext) {
//    def flatter(obj: Any, target: Option[String]): RemoteObject = {
//      obj match {
//        case ro: RemoteObject => {
//          target match {
//            case None =>
//            case Some(targetString) => ro.remoteName match {
//              case client.name => remoteObjectDistributed(ro.remoteID, targetString)
//              case _ =>
//              //val msg = MessageBuilder.newBuilder.asRequest("remoteObjectDistributed", ro.remoteID :: targetString :: Nil).to(ro.remoteName).objectID(-1).+=(Message.KeyNoResponse, true).create
//              //client.sendMessage(msg)
//            }
//          }
//          ro
//        }
//        case o => instanceRemoteObject(obj, target)
//      }
//    }
//
//    def shapper(name: String, id: Long): RemoteObject = RemoteObject(client, name, id)
//
//    ctx.channel.attr(AttributeKey.valueOf[(Any, Option[String]) => RemoteObject]("Flatter")).set(flatter)
//    ctx.channel.attr(AttributeKey.valueOf[(String, Long) => RemoteObject]("Shapper")).set(shapper)
//  }
//
//  override def channelInactive(ctx: ChannelHandlerContext) {
//  }
//}