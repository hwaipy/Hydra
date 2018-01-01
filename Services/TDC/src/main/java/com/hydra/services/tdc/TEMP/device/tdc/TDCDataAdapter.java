package com.hydra.services.tdc.TEMP.device.tdc;

/**
 *
 * @author Hwaipy
 */
public interface TDCDataAdapter {

  /**
   * Offering new data. The translated data can be directly returned or restored
   * in the adapter until next flush event.
   *
   * @param data
   * @return
   */
  public Object offer(Object data);

  /**
   * Notify the adapter to flush out the restored data.
   *
   * @param data
   * @return
   */
  public Object flush(Object data);
}
