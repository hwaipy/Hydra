package com.hydra.services.tdc.TEMP;

import java.util.ArrayList;

/**
 *
 * @author Hwaipy
 */
public class Coincidencer {

  private final ArrayList<Long>[] data;
  private final ArrayList<Long> buffer;
  private final ArrayList<Integer> channels;
  private final long gate;
  private final ArrayList<Long> triggerList;
  private final long range;
  private final int[] coincidences = new int[1 << 18];
  private final ArrayList<Integer> coincidenceList = new ArrayList<>();

  public Coincidencer(ArrayList<Long>[] data, long gate, ArrayList<Long> triggerList, long range) {
    this.data = data;
    if (data.length > 16) {
      throw new RuntimeException();
    }
    Serial serial = new Serial(data);
    serial.serial();
    buffer = serial.buffer;
    channels = serial.channels;
    this.gate = gate;
    this.triggerList = triggerList;
    this.range = range;
    coins();
  }

  private void coins() {
    int index = 0;
    while (index < buffer.size()) {
      int count = loadNextCoincidence(index);
      if (count > 1) {
        int coincMode = parseCoincidence(index, count);
        if (coincMode >= 0) {
          coincidences[coincMode]++;
          coincidenceList.add(coincMode);
        }
      }
      index += count;
    }
  }

  public int[] coincidences() {
    return coincidences;
  }

  public ArrayList<Integer> coincidenceList() {
    return coincidenceList;
  }

  private int parseCoincidence(int fromIndex, int count) {
    if (!inTriggerRange(fromIndex)) {
      return -1;
    }
    int mode = 0;
    for (int i = 0; i < count; i++) {
      int index = fromIndex + i;
      int channel = channels.get(index);
      mode |= (1 << channel);
    }
    return mode;
  }

  private int loadNextCoincidence(int fromIndex) {
    int index = fromIndex;
    long firstTime = buffer.get(index);
    index++;
    long endTime = firstTime + gate;
    while (index < buffer.size()) {
      if (buffer.get(index) <= endTime) {
        index++;
      } else {
        break;
      }
    }
    return index - fromIndex;
  }
  private int triggerIndex = 0;

  private boolean inTriggerRange(int fromIndex) {
    if (triggerList == null) {
      return true;
    }
    Long time = buffer.get(fromIndex);
    for (int ti = triggerIndex; ti < triggerList.size(); ti++) {
      long triggerTime = triggerList.get(ti);
      if (time >= triggerTime) {
        long delta = time - triggerTime;
        if (delta <= range) {
          return true;
        } else {
          triggerIndex++;
        }
      } else {
        return false;
      }
    }
    return false;
  }

  private class Serial {

    private final ArrayList<Long>[] data;
    private final int[] indices = new int[16];
    private final ArrayList<Long> buffer;
    private final ArrayList<Integer> channels;

    public Serial(ArrayList<Long>[] data) {
      this.data = data;
      int length = 0;
      for (ArrayList<Long> list : data) {
        length += list.size();
      }
      buffer = new ArrayList<>(length);
      channels = new ArrayList<>(length);
      serial();
    }
    private long time = 0;
    private int channel = -1;

    private void serial() {
      while (loadNextEvent()) {
        buffer.add(time);
        channels.add(channel);
      }
    }

    private boolean loadNextEvent() {
      long[] times = new long[data.length];
      for (int i = 0; i < data.length; i++) {
        ArrayList<Long> list = data[i];
        int index = indices[i];
        times[i] = (index < list.size()) ? list.get(index) : Long.MAX_VALUE;
      }
      long min = Long.MAX_VALUE;
      int minChannel = -1;
      for (int i = 0; i < data.length; i++) {
        if (times[i] < min) {
          min = times[i];
          minChannel = i;
        }
      }
      if (min == Long.MAX_VALUE) {
        return false;
      } else {
        time = min;
        channel = minChannel;
        indices[channel]++;
        return true;
      }
    }
  }
}
