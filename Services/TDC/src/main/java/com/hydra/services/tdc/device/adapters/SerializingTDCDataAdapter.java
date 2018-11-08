package com.hydra.services.tdc.device.adapters;

import com.hydra.services.tdc.device.TDCDataAdapter;
import com.hydra.services.tdc.device.serialize.Serializer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Hwaipy
 */
public class SerializingTDCDataAdapter implements TDCDataAdapter {

  private static final int BUFFER_SIZE_THRESHOLD = 100000;
  private final ArrayList<Long> buffer;
  private final Serializer serializer;

  public SerializingTDCDataAdapter(int channel, long resolution) {
    this.buffer = new ArrayList<>(1000000);
    serializer = new Serializer(channel, resolution);
  }

  @Override
  public byte[] offer(Object data) {
    return translate(data, false);
  }

  @Override
  public byte[] flush(Object data) {
    return translate(data, true);
  }

  private byte[] translate(Object data, boolean force) {
    if (data == null) {
      return null;
    }
    if (!(data instanceof List)) {
      throw new IllegalArgumentException("The input data of SerializingTDCDataAdapter should be List.");
    }
    List<Long> timeEvents = (List<Long>) data;
    buffer.addAll(timeEvents);
    return (force || (buffer.size() > BUFFER_SIZE_THRESHOLD)) ? doTranslate() : null;
  }

  private byte[] doTranslate() {
    if (buffer.isEmpty()) {
      return null;
    }
    byte[] bytes = serializer.serialize(buffer);
    buffer.clear();
    return bytes;
  }
}
