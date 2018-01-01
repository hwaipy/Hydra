package com.hydra.services.tdc.TEMP.device.tdc.adapters;

import com.hydra.services.tdc.TEMP.device.tdc.TDCDataAdapter;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Hwaipy
 */
public class GroundTDCDataAdapter implements TDCDataAdapter {

  private final ByteBuffer dataBuffer = ByteBuffer.allocate(100000000);
  private static final int FRAME_SIZE = 2048;
  private static final long COARSE_TIME_LIMIT = 1 << 28;
  private long carry = 0;
  private long lastCoarseTime = -1;
  private final long[] unitLong = new long[8];
  private final FineTimeCalibrator calibrator;
  private static final CRC16 CRC16 = new CRC16(0x8005);
  private final ArrayList<Long> timeEvents = new ArrayList<>(100000);
  private final int[] channelMapping;
  private final int[] antiChannelMapping;
  private final int channelBit;
  private final long maxTime;

  public GroundTDCDataAdapter(int[] channelMapping) {
    try {
      this.calibrator = new FineTimeCalibrator(null, 24);
      validEventCount = new int[channelMapping.length];
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    this.channelMapping = channelMapping;
    int maxChannel = 0;
    for (int channel : channelMapping) {
      if (channel > maxChannel) {
        maxChannel = channel;
      }
    }
    maxChannel++;
    antiChannelMapping = new int[maxChannel];
    Arrays.fill(antiChannelMapping, -1);
    for (int i = 0; i < channelMapping.length; i++) {
      antiChannelMapping[channelMapping[i]] = i;
    }
    channelBit = (int) Math.ceil(Math.log(channelMapping.length) / Math.log(2));
    System.out.println(channelBit);
    maxTime = Long.MAX_VALUE >> channelBit;
  }

  @Override
  public Object offer(Object data) {
    if (data == null) {
      return null;
    }
    if (!(data instanceof byte[])) {
      throw new IllegalArgumentException("Input data of GroundTDCDataAdapter should be byte array, not " + data.getClass());
    }
    byte[] dataB = (byte[]) data;
    try {
      dataBuffer.put(dataB);
    } catch (BufferOverflowException e) {
      throw new IllegalArgumentException("Input data too much.", e);
    }
    dataBuffer.flip();
    timeEvents.clear();
    while (dataBuffer.hasRemaining()) {
      if (!seekForFrameHead()) {
        break;
      }
      if (dataBuffer.remaining() < FRAME_SIZE) {
        break;
      }
      if (checkFrameTail()) {
        frameCount++;
        if (crc()) {
          validFrameCount++;
          int pStart = dataBuffer.position() + 8;
          int pEnd = pStart + FRAME_SIZE - 16;
          for (int p = pStart; p < pEnd; p += 8) {
            parseToTimeEvent(p);
          }
          dataBuffer.position(dataBuffer.position() + FRAME_SIZE);
        } else {
          dataBuffer.position(dataBuffer.position() + FRAME_SIZE);
        }
      } else {
        dataBuffer.position(dataBuffer.position() + 4);
        skippedInSeekingHead += 4;
      }
    }
    dataBuffer.compact();
    return timeEvents;
  }

  @Override
  public Object flush(Object data) {
    return offer(data);
  }

  private int frameCount = 0;
  private int validFrameCount = 0;
  private int unknownChannelEventCount = 0;
  private final int validEventCount[];
  private long skippedInSeekingHead = 0;

  public long getSkippedInSeekingHead() {
    return skippedInSeekingHead;
  }

  public int getFrameCount() {
    return frameCount;
  }

  public int getValidFrameCount() {
    return validFrameCount;
  }

  public int getUnknownChannelEventCount() {
    return unknownChannelEventCount;
  }

  public int[] getValidEventCount() {
    return Arrays.copyOf(validEventCount, validEventCount.length);
  }

  public long getDataRemaining() {
    return dataBuffer.position();
  }

  private boolean seekForFrameHead() {
    int headFlagCount = 0;
    int startPosition = dataBuffer.position();
    while (dataBuffer.hasRemaining()) {
      byte b = dataBuffer.get();
      if (b == -91) {
        headFlagCount++;
      } else {
        headFlagCount = 0;
      }
      if (headFlagCount == 4) {
        dataBuffer.position(dataBuffer.position() - 4);
        skippedInSeekingHead += (dataBuffer.position() - startPosition);
        return true;
      }
    }
    if (headFlagCount > 0) {
      dataBuffer.position(dataBuffer.position() - headFlagCount);
    }
    skippedInSeekingHead += (dataBuffer.position() - startPosition);
    return false;
  }

  private boolean checkFrameTail() {
    int tailPosition = dataBuffer.position() + FRAME_SIZE - 8;
    return dataBuffer.get(tailPosition + 2) == 71 && dataBuffer.get(tailPosition + 3) == 71
            && dataBuffer.get(tailPosition + 4) == 71 && dataBuffer.get(tailPosition + 5) == 71
            && dataBuffer.get(tailPosition + 6) == 71 && dataBuffer.get(tailPosition + 7) == 71;
  }

  private boolean crc() {
    int p = dataBuffer.position() + 4;
    int crc = CRC16.calculateCRC(dataBuffer.array(), p, FRAME_SIZE - 12, true);
    int dc1 = dataBuffer.get(p + FRAME_SIZE - 12);
    int dc2 = dataBuffer.get(p + FRAME_SIZE - 11);
    dc1 = dc1 >= 0 ? dc1 : dc1 + 256;
    dc2 = dc2 >= 0 ? dc2 : dc2 + 256;
    int datacrc = dc1 + dc2 * 256;
    return crc == datacrc;
  }

  private void parseToTimeEvent(int position) {
    byte[] array = dataBuffer.array();
    int channel = array[position + 6] & 0xFF - 64;
    for (int i = 0; i < 8; i++) {
      unitLong[i] = array[position + i];
      if (unitLong[i] < 0) {
        unitLong[i] += 256;
      }
    }
    long fineTime = ((unitLong[3] & 0x10) << 4) | unitLong[7];
    long coarseTime = (unitLong[4]) | (unitLong[5] << 8)
            | (unitLong[2] << 16) | ((unitLong[3] & 0x0F) << 24);
    if (coarseTime < lastCoarseTime && (lastCoarseTime > COARSE_TIME_LIMIT / 2) && (coarseTime < COARSE_TIME_LIMIT / 2)) {
      carry++;
    }
    lastCoarseTime = coarseTime;
    long time = -calibrator.calibration(channel, (int) fineTime)
            + ((coarseTime + (carry << 28)) * 6250);
    if (channel < 0 || channel >= antiChannelMapping.length || antiChannelMapping[channel] == -1) {
      unknownChannelEventCount++;
    } else {
      int mappedChannel = antiChannelMapping[channel];
      if (time > maxTime) {
        throw new RuntimeException("Time (" + time + ") exceed max time limit (" + maxTime + ").");
      }
      long timeEvent = (time << channelBit) + mappedChannel;
      timeEvents.add(timeEvent);
      validEventCount[mappedChannel]++;
    }
  }
}
