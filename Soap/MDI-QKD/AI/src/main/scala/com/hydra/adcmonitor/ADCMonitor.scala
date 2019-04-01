package com.hydra.adcmonitor

//import Common._
import java.util.concurrent.atomic.AtomicReference
import scala.collection.JavaConverters._
import Automation.BDaq._
import org.mongodb.scala.bson.{BsonArray, BsonDouble, BsonInt32, BsonInt64}
import org.mongodb.scala.{Completed, Document, MongoClient, Observer}

object ADCMonitor extends App {
  println("ADCMonitor!")

  val deviceName = "PCI-1742U,BID#12"
  val valueRange = "+/- 5 V"
  val clockRatePerChan = 1000.0
  val aiCtrl = new WaveformAiCtrl()
  aiCtrl.setSelectedDevice(new DeviceInformation(deviceName))
  val channels = aiCtrl.getChannels()
  channels.foreach(c => c.setSignalType(AiSignalType.SingleEnded))

  aiCtrl.getConversion().setChannelStart(0)
  aiCtrl.getConversion().setChannelCount(3)
  aiCtrl.getRecord().setSectionLength(1000)
  aiCtrl.getRecord().setSectionCount(0) //0 means Streaming mode;
  aiCtrl.getConversion().setClockRate(1000)
  aiCtrl.addDataReadyListener(new DataReadyEventListener())

  val startTimeMilli = System.currentTimeMillis()
  val startTimeNano = System.nanoTime()
  val previousTime = new AtomicReference[Option[Long]](None)

  val mongoClient: MongoClient = MongoClient("mongodb://MDIQKD:freespace@192.168.25.27:27019")
  val database = mongoClient.getDatabase("Freespace_MDI_QKD")
  val collection = database.getCollection("ChannelMonitor")

  class DataReadyEventListener extends BfdAiEventListener {
    def BfdAiEvent(sender: Any, args: BfdAiEventArgs) {
      val currentTime = startTimeMilli + ((System.nanoTime() - startTimeNano) / 1e6).toLong
      if (args.Count != 3000) println(s"Not 3000!!!! ${args.Count}")
      val data = new Array[Double](args.Count)
      val errorCode = aiCtrl.GetData(args.Count, data, 0, null, null, null, null)
      if (previousTime.get.isDefined) {
        val timeStep = (currentTime - previousTime.get.get) / 1000.0
        val dbd = Range(0, 1000).toList.map(i => data.slice(i * 3, i * 3 + 3).toList ++ List(previousTime.get.get + i * timeStep))

        val seq = Seq(("Monitor", BsonArray(dbd.map(dbd2 => BsonArray(dbd2.map(d => BsonDouble(d)))))), ("SystemTime", BsonInt64(currentTime)))
        val reportDoc = Document.fromSeq(seq)
        collection.insertOne(reportDoc).subscribe(new Observer[Completed] {

          override def onNext(result: Completed): Unit = println("Inserted")

          override def onError(e: Throwable): Unit = e.printStackTrace()

          override def onComplete(): Unit = {}
        })
      }
      previousTime set Some(currentTime)
    }
  }

  aiCtrl.Start()
}