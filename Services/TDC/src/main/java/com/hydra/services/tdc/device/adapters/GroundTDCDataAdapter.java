package com.hydra.services.tdc.device.adapters;

import com.hydra.services.tdc.device.TDCDataAdapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
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
    private LongBuffer timeEvents = null;
    private final int channelBit;
    private final long maxTime;
    private final int channelCount;

    public GroundTDCDataAdapter(int channelCount) {
        try {
            this.calibrator = new FineTimeCalibrator(null, 24);
            validEventCount = new int[channelCount];
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        this.channelCount = channelCount;
        channelBit = (int) Math.ceil(Math.log(channelCount) / Math.log(2));
//        System.out.println(channelBit);
        maxTime = Long.MAX_VALUE >> channelBit;
    }

    @Override
    public Object offer(Object data) {
        if (data == null) {
            return null;
        }
        ByteBuffer buffer;
        if (data instanceof byte[]) buffer = ByteBuffer.wrap((byte[]) data);
        else if (data instanceof ByteBuffer) buffer = (ByteBuffer) data;
        else throw new RuntimeException("Only byte array or ByteBuffer are acceptable for GroundTDCDataAdapter.");
        try {
            dataBuffer.put(buffer);
        } catch (BufferOverflowException e) {
            throw new IllegalArgumentException("Input data too much.", e);
        }
        dataBuffer.flip();
        timeEvents = LongBuffer.allocate(dataBuffer.remaining() / 8);
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
        timeEvents.flip();
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

    private int[] fineTimeStatistics = new int[1000];
    private int fineTimeCount = 0;

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

        AtomicInteger channel0Count = new AtomicInteger(0);
    private void parseToTimeEvent(int position) {
        byte[] array = dataBuffer.array();
        int channel = array[position + 6] - 0x40;
        if(channel == 0) channel0Count.incrementAndGet();
        if (channel > 16 || channel < 0) return;
        for (int i = 0; i < 8; i++) {
            unitLong[i] = array[position + i];
            if (unitLong[i] < 0) {
                unitLong[i] += 256;
            }
        }
        long fineTime = ((unitLong[3] & 0x10) << 4) | unitLong[7];

        /*
        if(fineTime<500 && channel==5){
            fineTimeStatistics[(int)fineTime]++;
            fineTimeCount++;
            if(fineTimeCount>1000000){
                try {
                    PrintWriter  pw = new PrintWriter(System.currentTimeMillis()+".csv");
                    for(int i=0;i<500;i++)
                    {
                        pw.println(i+", "+fineTimeStatistics[i]);
                        fineTimeStatistics[i]=0;
                    }
                    fineTimeCount=0;
                    pw.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }*/

        long coarseTime = (unitLong[4]) | (unitLong[5] << 8)
                | (unitLong[2] << 16) | ((unitLong[3] & 0x0F) << 24);
        if(Math.abs(coarseTime-lastCoarseTime)<COARSE_TIME_LIMIT/100 || Math.abs(coarseTime-lastCoarseTime+COARSE_TIME_LIMIT)<COARSE_TIME_LIMIT/100 ){}else{return;}
        if (coarseTime < lastCoarseTime && (lastCoarseTime > COARSE_TIME_LIMIT * 0.9) && (coarseTime < COARSE_TIME_LIMIT * 0.1)) {
       // if (coarseTime < lastCoarseTime) {

            carry++;
//            System.out.println("We are now carrying to" + carry + ": lastCT=" + lastCoarseTime + ", currentCT=" + coarseTime);
        }
        lastCoarseTime = coarseTime;
        long time = -calibrator.calibration(channel, (int) fineTime)
                + ((coarseTime + (carry << 28)) * 6250);
        if (channel < 0 || channel >= channelCount) {
            unknownChannelEventCount++;
        } else {
            if (time > maxTime) {
                throw new RuntimeException("Time (" + time + ") exceed max time limit (" + maxTime + ").");
            }
            long timeEvent = (time << channelBit) + channel;
            timeEvents.put(timeEvent);
            validEventCount[channel]++;
        }
    }
}
