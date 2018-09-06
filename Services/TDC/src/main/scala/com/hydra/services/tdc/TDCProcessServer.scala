package com.hydra.services.tdc

import java.net.ServerSocket
import java.nio.LongBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import com.hydra.io.BlockingRemoteObject

import scala.concurrent.{ExecutionContext, Future}
import com.hydra.services.tdc.device.{TDCDataAdapter, TDCParser}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class TDCProcessServer(val channelCount: Int, port: Int, dataIncome: Any => Unit, adapters: List[TDCDataAdapter]) {
  private val executionContext = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor((r) => {
    val t = new Thread(r)
    t.setDaemon(true)
    t
  }))
  private val tdcParser = new TDCParser((data: Any) => dataIncome(data), adapters.toArray)
  private val server = new ServerSocket(port)
  val buffer = new Array[Byte](1024 * 1024 * 16)
  Future {
    while (!server.isClosed) {
      println("start accept")
      val socket = server.accept
      println(s"accepted: ${socket}")
      val remoteAddress = socket.getRemoteSocketAddress
      System.out.println(s"Connected from ${remoteAddress}")
      try {
        val in = socket.getInputStream
        val loopEndRef = new AtomicBoolean(false)
        while (!socket.isClosed && !loopEndRef.get) {
          val read = in.read(buffer)
          if (read < 0) loopEndRef set true
          else {
            val array = new Array[Byte](read)
            Array.copy(buffer, 0, array, 0, read)
            tdcParser.offer(array)
          }
        }
      } finally {
        println(s"End of connection: ${remoteAddress}")
      }
    }
  }(executionContext)

  def stop() = {
    server.close
    tdcParser.stop
  }
}

class LongBufferToDataBlockListTDCDataAdapter(channelCount: Int) extends TDCDataAdapter {
  val delays = Range(0, channelCount).map(_ => 0l).toArray
  private val dataBlocks = new ArrayBuffer[DataBlock]()

  def offer(data: Any): AnyRef = {
    dataBlocks.clear
    dataIncome(data)
    dataBlocks.toList
  }

  def flush(data: Any): AnyRef = offer(data)

  private def dataIncome(data: Any) = {
    if (!data.isInstanceOf[LongBuffer]) throw new IllegalArgumentException(s"LongBuffer expected, not ${data.getClass}")
    val buffer = data.asInstanceOf[LongBuffer]
    while (buffer.hasRemaining) {
      val item = buffer.get
      val time = item >> 4
      val channel = (item & 0xF).toInt
      feedTimeEvent(channel, time)
    }
  }

  private val timeEvents = Range(0, channelCount).map(_ => ArrayBuffer[Long]()).toList
  private var unitEndTime = Long.MinValue

  private def feedTimeEvent(channel: Int, time: Long) {
    if (time > unitEndTime) {
      if (unitEndTime == Long.MinValue) unitEndTime = time
      else flush
    }
    timeEvents(channel) += time
  }

  private def flush() {
    val data = timeEvents.zipWithIndex.map(z => z._1.toList.map(t => t + delays(z._2)))
    timeEvents.foreach(_.clear)
    dataBlocks += new DataBlock(data)
    unitEndTime += 1000000000000l
  }
}

class DataBlock(val content: List[List[Long]])

abstract class DataAnalyser(protected val storageInvoker: BlockingRemoteObject) {
  protected val on = new AtomicBoolean(false)
  protected val recentResults = new ListBuffer[Any]

  def dataIncome(dataBlock: DataBlock) = if (on.get) analysis(dataBlock)

  def turnOn(paras: Map[String, String]) {
    on.set(true)
    reset(paras)
  }

  def turnOff() = on.set(false)

  protected def analysis(dataBlock: DataBlock)

  protected def reset(paras: Map[String, String])

  def fetchResult() = {
    val ret = recentResults.toList
    recentResults.clear()
    ret
  }
}

class CounterAnalyser(invoker: BlockingRemoteObject) extends DataAnalyser(invoker) {
  private val storagePath = new AtomicReference[String]()

  override protected def reset(paras: Map[String, String]) {
    paras.get("StorageElement").foreach(path => {
      storagePath.set(path)
      //      if (!invoker.exists("", path).asInstanceOf[Boolean]) {
      //      }
    })
  }

  override protected def analysis(dataBlock: DataBlock) {
    val r = dataBlock.content.map(list => list.size)
    recentResults += r
  }

  override def turnOff() {
    super.turnOff()
    storagePath.set(null)
  }
}
