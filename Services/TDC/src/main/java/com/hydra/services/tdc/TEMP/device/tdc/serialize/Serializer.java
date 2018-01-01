package com.hydra.services.tdc.TEMP.device.tdc.serialize;

import java.nio.ByteBuffer;
import java.util.List;

/**
 *
 * @author Hwaipy
 */
public class Serializer {

  private final long resolution;
  private final int channelDivision;
  private final int channelBit;

  public Serializer(int channel, long resolution) {
    this.resolution = resolution;
    int cd = 1;
    int cb = 0;
    while (channel > 0) {
      channel >>= 1;
      cd <<= 1;
      cb++;
    }
    this.channelDivision = cd;
    this.channelBit = cb;
  }

  public byte[] serialize(List<Long> timeEvents) {
//    for (int i = 0; i < 25000; i++) {
//      long timeEvent = timeEvents.get(i);
//      long time = (timeEvent >> 2);
//      System.out.println(time);
//    }
//    System.exit(0);
//    System.out.println("TimeEventSize: " + timeEvents.size());
    byte previousByte = 0;
    byte previousByteRemaining = 8;
    long startTime = timeEvents.get(0) / channelDivision;
    long endTime = timeEvents.get(timeEvents.size() - 1) / channelDivision;
    int timeBit = optimizeTimeBit();
    int timeEventBit = timeBit + channelBit;
    long timeEventMask = 1 << timeEventBit;
    long carryUnit = ((long) 1) << timeBit;
    long dataBitL = timeEvents.size() * (channelBit + timeBit + 1) + ((endTime - startTime) / carryUnit / resolution) + 224;
    if (dataBitL > Integer.MAX_VALUE) {
      throw new RuntimeException("Data rate not acceptable.");
    }
    int dataBit = (int) dataBitL;
    int dataByte = ((dataBit + 7) / 8);
    byte[] bytes = new byte[dataByte];
    ByteBuffer data = ByteBuffer.wrap(bytes);
    data.putInt(dataByte);
    data.putInt(timeEvents.size());
    data.putShort((short) timeBit);
    data.putShort((short) channelBit);
    data.putLong(startTime);
    data.putLong(resolution);
    long previousCarry = 0;
    for (Long timeEvent : timeEvents) {
      int channel = (int) (timeEvent % channelDivision);
      long fullTime = (timeEvent / channelDivision - startTime) / resolution;
      long carry = fullTime / carryUnit;
      long time = fullTime % carryUnit;
      long sTimeEvent = ((time << channelBit) + channel) | timeEventMask;
      int carrys = (int) (carry - previousCarry);
      while (carrys >= previousByteRemaining) {
        data.put(previousByte);
        carrys -= previousByteRemaining;
        previousByte = 0;
        previousByteRemaining = 8;
      }
      previousByteRemaining -= carrys;
      int timeEventRemaining = timeEventBit + 1;
      while (timeEventRemaining >= previousByteRemaining) {
        byte t = (byte) ((sTimeEvent & ((1 << timeEventRemaining) - 1)) >> (timeEventRemaining - previousByteRemaining));
        previousByte |= t;
        data.put(previousByte);
        timeEventRemaining -= previousByteRemaining;
        previousByte = 0;
        previousByteRemaining = 8;
      }
      byte t = (byte) ((sTimeEvent & ((1 << timeEventRemaining) - 1)) << (previousByteRemaining - timeEventRemaining));
      previousByte |= t;
      previousByteRemaining -= timeEventRemaining;
      previousCarry = carry;
    }
    if (previousByteRemaining < 8) {
      data.put(previousByte);
    }
    return bytes;
  }

  private int optimizeTimeBit() {
    return 26;
  }
}
