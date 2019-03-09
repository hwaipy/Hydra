package com.hydra.core

import java.nio.ByteBuffer

trait MessageEncoder {
  def feed(msg: Message): MessageEncoder

  def pack(): Array[Byte]
}

trait MessageDecoder {
  def feed(feed: ByteBuffer): Int

  def next(): Option[Message]

  def getStatistics: Tuple2[Int, Long]
}

object MessageEncodingProtocol {
  val PROTOCOL_MSGPACK = "MSGPACK"
  val PROTOCOL_JSON = "JSON"
//  val PROTOCOL_MSGPACK_PREFIX = "MSGPACK_PREFIX"
  val PROTOCOL_JSON_PREFIX = "JSON_PREFIX"
  val protocols = Seq(PROTOCOL_MSGPACK, PROTOCOL_JSON, PROTOCOL_JSON_PREFIX)
}