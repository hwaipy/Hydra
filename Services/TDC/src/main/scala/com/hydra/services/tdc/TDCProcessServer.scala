package com.hydra.services.tdc

import java.net.{InetSocketAddress, ServerSocket}
import java.nio.LongBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.{ExecutionContext, Future}
import com.hydra.services.tdc.device.{TDCDataAdapter, TDCParser}

class TDCProcessServer(val channelCount: Int, dataIncome: Any => Unit, adapters: List[TDCDataAdapter]) {
  private val executionContext = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor((r) => {
    val t = new Thread(r)
    t.setDaemon(true)
    t
  }))
  private val server = new ServerSocket()
  private val tdcParser = new TDCParser((data: Any) => dataIncome(data), adapters.toArray)

  def start(port: Int) = {
    server.bind(new InetSocketAddress("", port))
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
  }

  def stop = {
    server.close
    tdcParser.stop
  }
}

class LongBufferToDataBlockDataProcess(channelCount: Int, dataBlockIncome: (DataBlock) => Unit) {
  val delays = Range(0, channelCount).map(_ => 0l).toArray

  def dataIncome(data: Any) = {

  }


  //
  //  private def dataIncome(data: LongBuffer) {
  //    while (data.hasRemaining) {
  //      val item = data.get
  //      val time = item >> 4
  //      val channel = (item & 0xF).toInt
  //      feedTimeEvent(channel, time)
  //    }
  //  }
  //
  //  private val timeEvents = Range(0, channelCount).map(_ => ArrayBuffer[Long]()).toList
  //  private var unitEndTime = Long.MinValue
  //  private val executionContext = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
  //
  //  private def feedTimeEvent(channel: Int, time: Long) {
  //    if (time > unitEndTime) {
  //      if (unitEndTime == Long.MinValue) unitEndTime = time
  //      else flush
  //    }
  //    timeEvents(channel) += time
  //  }
  //
  //  private def flush {
  //    val data = timeEvents.zipWithIndex.map(z => z._1.toList.map(t => t + delays(z._2)))
  //    timeEvents.foreach(_.clear)
  //    Future {
  //      doFlushLoop(new DataBlock(data))
  //    }(executionContext)
  //    unitEndTime += 1000000000000l;
  //  }
  //
  //  private def doFlushLoop (dataBlock: DataBlock) = {
  //    println (dataBlock.data.map (list => list.size) )
  //  }


}

class DataBlock(val content: List[List[Long]])
