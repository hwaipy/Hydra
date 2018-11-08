package com.hydra.services.tdc.device.adapters;

/**
 *
 * @author Hwaipy
 */
public class CRC16 {

  private final short[] crcTable = new short[256];
  private final int gPloy;

  public CRC16(int gPloy) {
    this.gPloy = gPloy;
    computeCrcTable();
  }

  private short getCrcOfByte(int aByte) {
    int value = aByte << 8;
    for (int count = 7; count >= 0; count--) {
      if ((value & 0x8000) != 0) {
        value = (value << 1) ^ gPloy;
      } else {
        value = value << 1;
      }
    }
    value = value & 0xFFFF;
    return (short) value;
  }

  private void computeCrcTable() {
    for (int i = 0; i < 256; i++) {
      crcTable[i] = getCrcOfByte(i);
    }
  }

  public int calculateCRC(byte[] data, int offset, int lenth, boolean reverse) {
    int crc = 0;
    for (int i = 0; i < lenth; i++) {
      int ic = i;
      if (reverse) {
        ic = i % 2 == 0 ? i + 1 : i - 1;
      }
      crc = ((crc & 0xFF) << 8) ^ crcTable[(((crc & 0xFF00) >> 8) ^ data[offset + ic]) & 0xFF];
    }
    crc = crc & 0xFFFF;
    return crc;
  }
}
