package com.hydra.services.tdc.adapters

import java.nio.{BufferOverflowException, ByteBuffer, LongBuffer}

import com.hydra.services.tdc.device.TDCDataAdapter

class SimpleTDCDataAdapter(private val channelBit: Int) extends TDCDataAdapter {
  private val dataBuffer = ByteBuffer.allocate(100000000)

  def offer(data: Any): AnyRef = {
    if (data == null) return null
    val buffer = data match {
      case d: Array[Byte] => ByteBuffer.wrap(data.asInstanceOf[Array[Byte]])
      case d: ByteBuffer => d.asInstanceOf[ByteBuffer]
      case _ => throw new RuntimeException("Only byte array or ByteBuffer are acceptable for SimpleTDCDataAdapter.");
    }
    try dataBuffer.put(buffer) catch {
      case e: BufferOverflowException => throw new IllegalArgumentException("Input data too much.", e);
    }
    dataBuffer.flip
    val timeEvents = LongBuffer.allocate(dataBuffer.remaining() / 8)
    while (dataBuffer.remaining >= 8) timeEvents.put(dataBuffer.getLong)
    dataBuffer.compact
    timeEvents.flip
    timeEvents
  }

  def flush(data: Any): AnyRef = offer(data)
}