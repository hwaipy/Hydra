package com.hydra.app

import java.net.InetAddress

import com.hydra.io.{BroadcastServer, MessageServer, MessageTransport}

import scala.io.Source

object HydraServerApp extends App {
  val port = try {
    MessageTransport.Configuration.getProperty("messageserver.port").toInt
  } catch {
    case e: Throwable => 20102
  }
  val broadcastAddress = try {
    MessageTransport.Configuration.getProperty("messageserver.broadcast.address").toString
  } catch {
    case e: Throwable => "192.168.25.255"
  }
  val broadcastPort = try {
    MessageTransport.Configuration.getProperty("messageserver.broadcast.port").toInt
  } catch {
    case e: Throwable => 20100
  }
  val server = new MessageServer(port)
  val future = server.start
  val broadcastServer = new BroadcastServer(InetAddress.getByName(broadcastAddress), broadcastPort, "Hydra Server Freespace")
  broadcastServer.start
  println(s"Hydra Server started on port $port.")
  println(s"Server broadcast started on port $broadcastPort.")

  println(s"Internal Services started.")
  Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
  println("Stoping Hydra Server...")
  server.stop
  broadcastServer.stop
}
