package com.hydra.services.tdc

import java.net.ServerSocket
import java.nio.LongBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ExecutionContext, Future}
import com.hydra.services.tdc.device.{TDCDataAdapter, TDCParser}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import com.hydra.`type`.NumberTypeConversions._

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

  def setDelays(delays: List[Long]) = {
    if (delays.size != this.delays.size) throw new IllegalArgumentException(s"Delays should has length of ${this.delays.size}.")
    delays.zipWithIndex.foreach(z => this.delays(z._2) = z._1)
  }

  def setDelay(channel: Int, delay: Long) = {
    if (channel >= this.delays.size || channel < 0) throw new IllegalArgumentException(s"Channel $channel out of range.")
    delays(channel) = delay
  }

  def getDelays() = delays.toList

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
    val data = timeEvents.zipWithIndex.map(z => z._1.toArray.map(t => t + delays(z._2))).toArray
    timeEvents.foreach(_.clear)
    dataBlocks += new DataBlock(data)
    unitEndTime += 1000000000000l
  }
}

class DataBlock(val content: Array[Array[Long]])

abstract class DataAnalyser {
  protected val on = new AtomicBoolean(false)
  protected val configuration = new mutable.HashMap[String, Any]()

  def dataIncome(dataBlock: DataBlock): Option[Any] = if (on.get) Some(analysis(dataBlock)) else None

  def turnOn(paras: Map[String, Any]) {
    on.set(true)
    configure(paras)
  }

  def turnOff() = on.set(false)

  protected def analysis(dataBlock: DataBlock): Any

  def configure(paras: Map[String, Any]): Unit = paras.foreach(e => if (configure(e._1, e._2)) configuration(e._1) = e._2)

  protected def configure(key: String, value: Any) = true

  def getConfiguration() = configuration.toMap

  def isTurnedOn() = on.get
}

class CounterAnalyser(channelCount: Int) extends DataAnalyser {

  override protected def analysis(dataBlock: DataBlock) = dataBlock.content.map(list => list.size).toList
}

class HistogramAnalyser(channelCount: Int) extends DataAnalyser {
  configuration("Sync") = 1
  configuration("Signal") = 1
  configuration("ViewStart") = -100000
  configuration("ViewStop") = 100000
  configuration("BinCount") = 1000
  configuration("Divide") = 1

  override def configure(key: String, value: Any) = {
    key match {
      case "Sync" => {
        val sc: Int = value
        sc >= 0 && sc < channelCount
      }
      case "Signal" => {
        val sc: Int = value
        sc >= 0 && sc < channelCount
      }
      case "ViewStart" => true
      case "ViewStop" => true
      case "BinCount" => {
        val sc: Int = value
        sc > 0 && sc < 2000
      }
      case "Divide" => {
        val sc: Int = value
        sc > 0
      }
      case _ => false
    }
  }

  override protected def analysis(dataBlock: DataBlock) = {
    val deltas = new ArrayBuffer[Long]()
    val syncChannel: Int = configuration("Sync")
    val signalChannel: Int = configuration("Signal")
    val viewStart: Long = configuration("ViewStart")
    val viewStop: Long = configuration("ViewStop")
    val binCount: Int = configuration("BinCount")
    val divide: Int = configuration("Divide")
    val tList = dataBlock.content(syncChannel)
    val sList = dataBlock.content(signalChannel)
    val viewFrom = viewStart
    val viewTo = viewStop
    if (tList.size > 0 && sList.size > 0) {
      var preStartT = 0
      val lengthT = tList.size
      sList.foreach(s => {
        var cont = true
        while (preStartT < lengthT && cont) {
          val t = tList(preStartT)
          val delta = s - t
          if (delta > viewTo) {
            preStartT += 1
          } else cont = false
        }
        var tIndex = preStartT
        cont = true
        while (tIndex < lengthT && cont) {
          val t = tList(tIndex)
          val delta = s - t
          if (delta > viewFrom) {
            deltas += delta
            tIndex += 1
          } else cont = false
        }
      })
    }
    val histo = new Histogram(deltas.toArray, binCount, viewFrom, viewTo, divide)
    Map[String, Any]("SyncChannel" -> syncChannel, "SignalChannel" -> signalChannel,
      "ViewFrom" -> viewFrom, "ViewTo" -> viewTo, "Divide" -> divide, "Histogram" -> histo.yData.toList)
  }

  override def turnOff() {
    super.turnOff()
  }
}

class Histogram(deltas: Array[Long], binCount: Int, viewFrom: Long, viewTo: Long, divide: Int) {
  val min = viewFrom.toDouble
  val max = viewTo.toDouble
  val binSize = (max - min) / binCount / divide
  val xData = Range(0, binCount).map(i => (i * binSize + min) + binSize / 2).toArray
  val yData = new Array[Int](binCount)
  deltas.foreach(delta => {
    val deltaDouble = delta.toDouble
    if (deltaDouble < min) {
      /* this data is smaller than min */
    } else if (deltaDouble == max) { // the value falls exactly on the max value
      yData(binCount - 1) += 1
    } else if (deltaDouble > max) {
      /* this data point is bigger than max */
    } else {
      val bin = ((deltaDouble - min) / binSize).toInt % binCount
      yData(bin) += 1
    }
  })
}

