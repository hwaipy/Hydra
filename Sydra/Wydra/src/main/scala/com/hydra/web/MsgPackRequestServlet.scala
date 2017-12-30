package com.hydra.web

import java.io.DataInputStream
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.hwaipy.hydrogen.web.servlet.AsyncHttpServlet
import com.hydra.core.{Message, MessageBuilder, MessageGenerator, MessagePacker}

import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import java.util.Arrays

object MsgPackRequestServlet {
  val path = "/request/*"
}

class MsgPackRequestServlet extends AsyncHttpServlet {
  println("Creating MsgPackRequestServlet")
  val invokeCounter = new AtomicInteger()

  new Thread(new Runnable {
    override def run(): Unit = {
      while (true) {
        Thread.sleep(10000)
        println(invokeCounter.get())
      }
    }
  }).start()

  override def doPostAsync(req: HttpServletRequest, resp: HttpServletResponse) {
    invokeCounter.incrementAndGet()
    val buffer = new Array[Byte](req.getContentLength)
    new DataInputStream(req.getInputStream()).readFully(buffer)
    val mg = new MessageGenerator()
    val packer = new MessagePacker()
    mg.feed(buffer)
    val hasNext = new AtomicBoolean(true)
    val msgBuffer = new ArrayBuffer[Message]()
    while (hasNext.get) {
      mg.next() match {
        case Some(msg) => msgBuffer += msg
        case None => hasNext.set(false)
      }
    }
    val msgs = msgBuffer.toList.map(m => m + (Message.KeyMessageID -> MessageBuilder.newBuilder().create.messageID))
    val rs = msgs.map(m => {
      try {
        WydraApp.client.requestMessage(m, 10 seconds)
      } catch {
        case e: Throwable => s"Error: $e"
      }
    })
    packer.feed(rs)
    val data = packer.pack
    resp.getOutputStream.write(data)
    invokeCounter.decrementAndGet()
  }
}