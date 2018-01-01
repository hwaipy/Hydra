package com.hydra.services.tdc.TEMP.device.tdc;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The refresh time is used to trigger write events. In some application, the
 * tdc datawriter tends to wait for enough data before write out. So the offer
 * method is invoked in a fixed frequency with arguments null to remaind the
 * adapters to write out the translated data.
 *
 * @author Hwaipy
 */
public class TDCParser {

  private final TDCDataProcessor processor;
  private long flushTime;
  private final TDCDataAdapter[] adapters;
  private final BlockingQueue<Object> dataQueue = new LinkedBlockingQueue<>();

  public TDCParser(TDCDataProcessor processor, long flushTime, TDCDataAdapter... adapters) {
    this.processor = processor;
    this.flushTime = flushTime;
    this.adapters = adapters;
    Timer timer = new Timer("TDCParser refresh timer", true);
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        flush();
      }
    }, flushTime, flushTime);
    Thread processingThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (true) {
            processLoop();
          }
        } catch (InterruptedException ex) {
          ex.printStackTrace();
          throw new RuntimeException(ex);
        }
      }
    }, "TDCParser processor");
    processingThread.start();
  }

  public TDCParser(TDCDataProcessor writer, TDCDataAdapter... adapters) {
    this(writer, 100, adapters);
  }

  public void offer(byte[] data) {
    dataQueue.offer(data);
  }

  public void waitForFinish() throws InterruptedException {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    dataQueue.offer(countDownLatch);
    countDownLatch.await();
  }
  private static final byte[] FLUSH_OBJECT = new byte[0];

  private void flush() {
    dataQueue.offer(FLUSH_OBJECT);
  }

  private void processLoop() throws InterruptedException {
    Object data = dataQueue.take();
    if (data == FLUSH_OBJECT) {
      shrinkQueue();
    }
    if (data instanceof CountDownLatch) {
      CountDownLatch countDownLatch = (CountDownLatch) data;
      countDownLatch.countDown();
      return;
    }
    if (!(data instanceof byte[])) {
      System.out.println(data);
    }
    for (TDCDataAdapter adapter : adapters) {
      data = adapter.offer(data);
    }
    processor.process(data);
  }

  private void shrinkQueue() {
    while (!dataQueue.isEmpty()) {
      Object peeked = dataQueue.peek();
      if (peeked == FLUSH_OBJECT) {
        dataQueue.poll();
      } else {
        return;
      }
    }
  }
}
