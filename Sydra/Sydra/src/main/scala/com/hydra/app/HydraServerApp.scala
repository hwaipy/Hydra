package com.hydra.app

import com.hydra.io.{MessageClient, MessageServer, MessageTransport}

import scala.io.Source

object HydraServerApp extends App {
  args match {
    case a if a.length == 0 => {
      val portMsgpack = try {
        MessageTransport.Configuration.getProperty("messageserver.port.msgpack").toInt
      } catch {
        case e: Throwable => 20102
      }
      val portJSON = try {
        MessageTransport.Configuration.getProperty("messageserver.port.json").toInt
      } catch {
        case e: Throwable => 20103
      }
      val server = new MessageServer(portMsgpack, portJSON)
      val future = server.start
      println(s"Hydra Server started on port $portMsgpack for MsgPack.")
      MessageTransport.Logger.info(s"Hydra Server started on port $portMsgpack for MsgPack.")
      println(s"Hydra Server started on port $portJSON for JSON.")
      MessageTransport.Logger.info(s"Hydra Server started on port $portJSON for JSON.")
      Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
      println("Stoping Hydra Server...")
      server.stop
    }
    case a if a(0) == "test" => {
      val client = MessageClient.newClient("localhost", 20102, "Test-Client")
      client.start
      println("out")
      Source.stdin.getLines.filter(line => line.toLowerCase == "q").next
      println("Stoping Test Client...")
      client.stop
    }
  }
}
