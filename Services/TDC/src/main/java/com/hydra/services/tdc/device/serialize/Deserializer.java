package com.hydra.services.tdc.device.serialize;

import java.nio.ByteBuffer;

/**
 *
 * @author Hwaipy
 */
public class Deserializer {

  private static final int[][] CARRY_MAP;
  private static final int[][] BYTE_MASK;
//  private static final int[] CARRY_MAP;
  private static final int STATUS_CARRY = 0;
  private static final int STATUS_CARRY_END = 1;
  private static final int STATUS_TIME_EVENT = 2;
  private byte previousByte = 0;
  private byte previousByteRemaining = 8;

  public Deserializer() {
  }

  public long[] deserialize(byte[] dataBlock) {
    ByteBuffer data = ByteBuffer.wrap(dataBlock);
    int dataByte = data.getInt();
    int timeEventsLength = data.getInt();
    int timeBit = data.getShort();
    int channelBit = data.getShort();
    long timeMask = (1 << (timeBit + channelBit)) - (1 << channelBit);
    long channelMask = (1 << channelBit) - 1;
    long startTime = data.getLong();
    long resolution = data.getLong();
    long carryUnit = ((long) 1) << timeBit;
    if (dataByte != dataBlock.length) {
      throw new IllegalArgumentException("Data block size " + dataBlock.length + " not match the in-data value " + dataByte + ".");
    }
    long currentCarry = 0;
    int timeEventBitRemaining = timeBit + channelBit;
    long timeEventValue = 0;
    int status = STATUS_CARRY;
    long[] timeEvents = new long[timeEventsLength];
    int timeEventsIndex = 0;

    for (int i = 28; i < dataBlock.length; i++) {
      final byte byteValue = dataBlock[i];
      int b = byteValue >= 0 ? byteValue : byteValue + 256;
//      System.out.println("---------------------------------Next byte: " + b);
      int byteRemaining = 8;
      while (byteRemaining > 0) {
        switch (status) {
          case STATUS_CARRY:
//            System.out.println("In Status Carry.");
            int carryCount = CARRY_MAP[8 - byteRemaining][b];
            if (carryCount > 0) {
//              System.out.println("Carry increased: " + carryCount);
            }
//            System.out.println("Carry Count: " + carryCount);
            currentCarry += carryCount;
            byteRemaining -= carryCount;
            if (byteRemaining > 0) {
              status = STATUS_CARRY_END;
//              System.out.println("Status change to Carry_End");
            }
            break;
          case STATUS_CARRY_END:
//            System.out.println("In Status Carry_End.");
            byteRemaining--;
            status = STATUS_TIME_EVENT;
            timeEventValue = 0;
//            System.out.println("Status change to TimeEvent");
            timeEventBitRemaining = timeBit + channelBit;
            break;
          case STATUS_TIME_EVENT:
//            System.out.println("In Status TimeEvent.");
            int readBit = timeEventBitRemaining > byteRemaining ? byteRemaining : timeEventBitRemaining;
//            System.out.println("ReadBit: " + readBit);
//            System.out.println("rm: " + byteRemaining);
//            System.out.println("mask is " + BYTE_MASK[8 - byteRemaining][8 - byteRemaining + readBit]);
            timeEventValue <<= readBit;
            int append = ((b & BYTE_MASK[8 - byteRemaining][8 - byteRemaining + readBit]) >> (byteRemaining - readBit));
//            System.out.println("append: " + append);
            timeEventValue += append;
//            System.out.println("tevalue: " + timeEventValue);
            byteRemaining -= readBit;
            timeEventBitRemaining -= readBit;
            if (timeEventBitRemaining == 0) {
              long timeValue = (timeEventValue & timeMask) >> channelBit;
//              System.out.println("new time value: " + timeValue);
              long channelValue = timeEventValue & channelMask;
              long time = (currentCarry * carryUnit + timeValue) * resolution + startTime;
//              System.out.println("dtime: " + timeValue);
              timeEvents[timeEventsIndex] = (time << channelBit) + channelValue;
//              debugTime(time);
              timeEventsIndex++;
              status = STATUS_CARRY;
//              System.out.println("Status change to Carry");
            }
            break;
          default:
            throw new RuntimeException();
        }
//        System.out.println("Summary: bbrm(" + byteRemaining + "), term(" + timeEventBitRemaining + ")");
      }
    }
//    System.out.println(Arrays.toString(timeEvents));
    return timeEvents;
  }

  static {
    CARRY_MAP = new int[8][256];
    for (int startPoint = 0; startPoint < 8; startPoint++) {
      for (int value = 0; value < 256; value++) {
        int c = 8 - startPoint;
        int mask = (1 << c) - 1;
        int v = value & mask;
        while (v > 0) {
          v >>= 1;
          c--;
        }
        CARRY_MAP[startPoint][value] = c;
      }
    }
    BYTE_MASK = new int[8][9];
    for (int start = 0; start < 8; start++) {
      for (int end = 0; end < 9; end++) {
        BYTE_MASK[start][end] = (0x100 >> start) - (0x100 >> end);
      }
    }
  }
//  static {
//    CARRY_MAP = new int[256];
//    for (int value = 0; value < 256; value++) {
//      int v = value;
//      int c = 8;
//      while (v > 0) {
//        v >>= 1;
//        c--;
//      }
//      CARRY_MAP[value] = c;
//    }
//  }

  private long lastTime = 0;
  private int timeIndex = 0;

  private final void debugTime(long time) {
    long diff = time - lastTime;
    System.out.println(time);
//    System.out.println("t" + timeIndex + ": " + diff);
    if (diff > 1100000) {
      System.out.println("diff: " + diff);
      System.exit(0);
    }
    lastTime = time;
    timeIndex++;
  }
}
