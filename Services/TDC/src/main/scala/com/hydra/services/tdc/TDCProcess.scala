package com.hydra.services.tdc

import java.util.concurrent.atomic.AtomicReference

import com.hydra.core.MessagePack
import com.hydra.io.{BlockingRemoteObject, MessageClient}
import com.hydra.services.tdc.application.MDIQKDAnalyser
import com.hydra.services.tdc.device.adapters.GroundTDCDataAdapter

import scala.collection.mutable
import scala.io.Source

class GroundTDCProcessService(port: Int) {
  private val channelCount = 16
  private val groundTDA = new GroundTDCDataAdapter(channelCount)
  private val dataTDA = new LongBufferToDataBlockListTDCDataAdapter(channelCount)
  private val server = new TDCProcessServer(channelCount, port, dataIncome, List(groundTDA, dataTDA))
  private val analysers = mutable.HashMap[String, DataAnalyser]()
  private val pathRef = new AtomicReference[String]("/test/tdc/default.fs")
  private val storageRef = new AtomicReference[BlockingRemoteObject](null)
  analysers("Counter") = new CounterAnalyser(channelCount)
  analysers("Histogram") = new HistogramAnalyser(channelCount)
  analysers("MDIQKD") = new MDIQKDAnalyser(channelCount)

  def stop() = server.stop

  private def dataIncome(data: Any) = {
    if (!data.isInstanceOf[List[_]]) throw new IllegalArgumentException(s"Wrong type: ${data.getClass}")
    data.asInstanceOf[List[DataBlock]].foreach(dataBlockIncome)
  }

  private def dataBlockIncome(dataBlock: DataBlock) = {
    val result = new mutable.HashMap[String, Any]()
    analysers.map(e => (e._1, e._2.dataIncome(dataBlock))).filter(e => e._2.isDefined).foreach(e => result(e._1) = e._2.get)
    result("Time") = System.currentTimeMillis()
    val bytes = MessagePack.pack(result)
    if (storageRef.get != null) storageRef.get.FSFileAppendFrame("", pathRef.get, bytes)
  }

  def postInit(client: MessageClient) = {
    storageRef set client.blockingInvoker("StorageService")
    storageRef.get.FSFileInitialize("", pathRef.get)
  }

  def turnOnAnalyser(name: String, paras: Map[String, String] = Map()) = analysers.get(name) match {
    case Some(analyser) => analyser.turnOn(paras)
    case None => throw new IllegalArgumentException(s"Analyser ${name} not exists.")
  }

  def configureAnalyser(name: String, paras: Map[String, String]) = analysers.get(name) match {
    case Some(analyser) => analyser.configure(paras)
    case None => throw new IllegalArgumentException(s"Analyser ${name} not exists.")
  }

  def getAnalyserConfiguration(name: String) = analysers.get(name) match {
    case Some(analyser) => analyser.getConfiguration()
    case None => throw new IllegalArgumentException(s"Analyser ${name} not exists.")
  }

  def turnOffAnalyser(name: String) = analysers.get(name) match {
    case Some(analyser) => analyser.turnOff()
    case None => throw new IllegalArgumentException(s"Analyser ${name} not exists.")
  }

  def turnOffAllAnalysers() = analysers.values.foreach(analyser => analyser.turnOff())

  def setDelays(delays: List[Long]) = dataTDA.setDelays(delays)

  def setDelay(channel: Int, delay: Long) = dataTDA.setDelay(channel, delay)

  def getDelays() = dataTDA.getDelays()
}

object TDCProcess extends App {
  val parameters = mutable.HashMap[String, String]()
  args.foreach(arg => {
    val splitted = arg.split("=", 2)
    println(splitted.toList)
    if (splitted.size == 2) parameters.put(splitted(0), splitted(1))
  })

  val port = 20156
  val process = new GroundTDCProcessService(port)
  val client = MessageClient.newClient(parameters.get("server").getOrElse("localhost"), parameters.get("port").getOrElse("20102").toInt, parameters.get("clientName").getOrElse("GroundTDCService"), process)
  process.postInit(client)

  process.turnOnAnalyser("Counter")
  process.turnOnAnalyser("Histogram", Map("Sync" -> "0", "Signal" -> "1", "ViewStart" -> "-100000", "ViewStop" -> "100000"))

  println("Ground TDC Process started on port 20156.")
  Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
  println("Stoping Ground TDC Process...")
  client.stop
  process.stop
}