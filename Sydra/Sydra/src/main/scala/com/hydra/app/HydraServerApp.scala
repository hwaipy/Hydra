package com.hydra.app

import com.hydra.io.MessageServer
import com.hydra.io.MessageTransport

import scala.io.Source

object HydraServerApp extends App {
  val port = try {
    MessageTransport.Configuration.getProperty("messageserver.port").toInt
  } catch {
    case e: Throwable => 20102
  }
  val server = new MessageServer(port)
  val future = server.start
  println(s"Hydra Server started on port $port.")
  MessageTransport.Logger.info(s"Hydra Server started on port $port.")
  Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
  println("Stoping Hydra Server...")
  server.stop
}
