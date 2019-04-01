package com.hydra.services.tdc.device.adapters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Hwaipy
 */
class FineTimeCalibrator {

  private final boolean enabled;
  private long[][] mapping;

  public FineTimeCalibrator(File file, int channelCount) throws FileNotFoundException, IOException {
    if (file == null) {
      enabled = false;
    } else {
      enabled = true;
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
      mapping = new long[channelCount][0];
      for (int channel = 0; channel < channelCount; channel++) {
        String line = reader.readLine();
        String[] splits = line.split(";");
        mapping[channel] = new long[splits.length - 1];
        for (int i = 1; i < splits.length; i++) {
          String valueString = splits[i];
          double valueDouble = Double.parseDouble(valueString);
          mapping[channel][i - 1] = (long) valueDouble;
        }
      }
    }
    simpleMaxValueMapping = new DistributionMap[channelCount];
    for (int i = 0; i < simpleMaxValueMapping.length; i++) {
      simpleMaxValueMapping[i] = new DistributionMap();
    }
  }

  private final DistributionMap[] simpleMaxValueMapping;

  public long calibration(int channel, int fineTime) {
    int channelMax = simpleMaxValueMapping[channel].update(fineTime);
    if (!enabled) {
      if (fineTime >= channelMax) {
        return 6250;
      } else {
        return (long) (6250. / channelMax * fineTime);
      }
    }
    long[] m = mapping[channel];
    if (fineTime < m.length) {
      return m[fineTime];
    } else {
      return 6250;
    }
  }

  private class DistributionMap {

    private long total = 0;
    private long[] counts = new long[300];
    private int max = 250;

    private int update(int fineTime) {
      if (fineTime >= counts.length || fineTime < 0) {
        return max;
      }
      counts[fineTime]++;
      total++;
      if (total % 10000 == 0) {
        long th = total / 1000;
        long count = 0;
        for (int i = counts.length - 1; i >= 0; i--) {
          count += counts[i];
          if (count > th) {
            max = i;
            break;
          }
        }
      }
      return max;
    }
  }
}
