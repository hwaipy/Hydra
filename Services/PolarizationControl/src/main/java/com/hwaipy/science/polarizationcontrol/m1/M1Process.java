package com.hwaipy.science.polarizationcontrol.m1;

import Jama.Matrix;
import com.hwaipy.science.polarizationcontrol.device.QuarterWavePlate;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author HwaipyLab
 */
public class M1Process {

  private final double IHH;
  private final double IHV;
  private final double IHD;
  private final double IHA;
  private final double IHL;
  private final double IHR;
  private final double IDH;
  private final double IDV;
  private final double IDD;
  private final double IDA;
  private final double IDL;
  private final double IDR;
  private double normalizeParaSH;
  private double normalizeParaSR;
  private double[] results;

  private M1Process(double[] inputs) throws M1ProcessException {
    if (inputs.length != 12) {
      throw new IllegalArgumentException();
    }
    IHH = inputs[0];
    IHV = inputs[1];
    IHD = inputs[2];
    IHA = inputs[3];
    IHL = inputs[4];
    IHR = inputs[5];
    IDH = inputs[6];
    IDV = inputs[7];
    IDD = inputs[8];
    IDA = inputs[9];
    IDL = inputs[10];
    IDR = inputs[11];
    calculate();
  }

  private M1Process(double[] inputs, double[] effction) throws M1ProcessException {
    if (inputs.length != 12 || effction.length != 4) {
      throw new IllegalArgumentException();
    }
    IHH = inputs[0] / effction[3];
    IHV = inputs[1] / effction[2];
    IHD = inputs[2] / effction[0];
    IHA = inputs[3] / effction[1];
    IHL = inputs[4] / effction[3];
    IHR = inputs[5] / effction[2];
    IDH = inputs[6] / effction[3];
    IDV = inputs[7] / effction[2];
    IDD = inputs[8] / effction[0];
    IDA = inputs[9] / effction[1];
    IDL = inputs[10] / effction[3];
    IDR = inputs[11] / effction[2];
    System.out.println(IHH + ", " + IHV + ", " + IHD + ", " + IHA + ", " + IHL + ", " + IHR + ", " + IDH + ", " + IDV + ", " + IDD + ", " + IDA + ", " + IDL + ", " + IDR);
    System.out.println(IHH + IHV - IHD);
    System.out.println(IDH + IDV - IDD);
    calculate();
  }

  public static M1Process calculate(double[] inputs) throws M1ProcessException {
    return new M1Process(inputs);
  }

  public static M1Process calculate(double[] inputs, double[] efficiences) throws M1ProcessException {
    return new M1Process(inputs, efficiences);
  }

  private void calculate() throws M1ProcessException {
    double[] sh = new double[]{(IHH - IHV) / (IHH + IHV), (IHD - IHA) / (IHD + IHA), (IHR - IHL) / (IHR + IHL)};
    double[] sd = new double[]{(IDH - IDV) / (IDH + IDV), (IDD - IDA) / (IDD + IDA), (IDR - IDL) / (IDR + IDL)};
    normalizeParaSH = normalize(sh);
    normalizeParaSR = normalize(sd);
//        System.out.println(Arrays.toString(sh));
//        System.out.println(Arrays.toString(sd));

    double[] sr = new double[]{sh[1] * sd[2] - sh[2] * sd[1], sh[2] * sd[0] - sh[0] * sd[2], sh[0] * sd[1] - sh[1] * sd[0]};
//        System.out.println(Arrays.toString(sr));

    double m = Math.sqrt(Math.pow(sr[0], 2) + Math.pow(sr[1], 2) + Math.pow(sr[2], 2));
    Matrix SH = new Matrix(new double[][]{{1, sh[0], sh[1], sh[2]}}).transpose();
    Matrix SR = new Matrix(new double[][]{{1, sr[0] / m, sr[1] / m, sr[2] / m}}).transpose();
    double theta1 = 0.5 * Math.atan(SR.get(2, 0) / SR.get(1, 0));

    QuarterWavePlate qwp1 = new QuarterWavePlate(theta1);
    Matrix SR1 = qwp1.getMatrix().times(SR);
    Matrix SH1 = qwp1.getMatrix().times(SH);

//        System.out.println("SR:");
//        System.out.println(SR.get(1, 0));
//        System.out.println(SR.get(2, 0));
//        System.out.println(SR.get(3, 0));
//        System.out.println("SR1:");
//        System.out.println(SR1.get(1, 0));
//        System.out.println(SR1.get(2, 0));
//        System.out.println("SH1");
//        System.out.println(SH1.get(1, 0));
//        System.out.println(SH1.get(2, 0));
//        System.out.println(SH1.get(3, 0));
    double a2 = 0.5 * Math.atan(SH1.get(2, 0) / SH1.get(1, 0));
    double a3 = 0.5 * Math.asin(SH1.get(3, 0));
//        System.out.println("a2=arctan " + SH1.get(2, 0) + "/" + SH1.get(1, 0));
//        System.out.println("a2=" + a2);
//        System.out.println("a3=" + a3);
    results = new double[3];
    results[0] = theta1;

//        System.out.println(SH1.get(1, 0));
//        System.out.println(SH1.get(2, 0));
//        System.out.println(SH1.get(3, 0));
//        System.out.println(SR1.get(1, 0));
//        System.out.println(SR1.get(2, 0));
    int flag = 0;
    if (SH1.get(1, 0) > 0) {
      flag |= 1 << 4;
    }
    if (SH1.get(2, 0) > 0) {
      flag |= 1 << 3;
    }
    if (SH1.get(3, 0) > 0) {
      flag |= 1 << 2;
    }
    if (SR1.get(1, 0) > 0) {
      flag |= 1 << 1;
    }
    if (SR1.get(2, 0) > 0) {
      flag |= 1;
    }
//        System.out.println(flag);
    switch (flag) {
      case 23:
      case 19:
      case 29:
      case 25:
        //A
        results[1] = a2;
        results[2] = (a2 + a3) / 2;
        break;
      case 15:
        //B
        results[1] = a2;
        results[2] = (a2 - a3) / 2 + Math.PI / 4;
        break;
      case 11:
      case 5:
      case 1:
//                System.out.println(123);
        //C
        results[1] = a2;
        results[2] = (a2 - a3) / 2;
        results[2] = results[2] > 0 ? results[2] - Math.PI / 4 : results[2] + Math.PI / 4;
        break;
      case 20:
      case 16:
      case 30:
      case 26:
        //D
        results[1] = a2 + Math.PI / 2;
        results[2] = (a2 - a3) / 2;
        break;
      case 6:
        //E
        results[1] = a2 + Math.PI / 2;
        results[2] = (a2 + a3) / 2 - Math.PI / 4;
        break;
      case 8:
        //F
        results[1] = a2 + Math.PI / 2;
        results[2] = (a2 + a3) / 2 + Math.PI / 4;
        break;
      case 12:
      case 2:
        //G
        results[1] = a2 + Math.PI / 2;
        results[2] = (a2 + a3) / 2;
        results[2] = results[2] > 0 ? results[2] - Math.PI / 4 : results[2] + Math.PI / 4;
        break;
      default:
        //Z
        throw new M1ProcessException();
    }
  }

  private double trim(double theta) {
    double t = theta;
    while (t < Math.PI / 2) {
      t += Math.PI;
    }
    while (t > Math.PI / 2) {
      t -= Math.PI;
    }
    return t;
  }

  private double normalize(double[] data) {
    if (data.length != 3) {
      throw new IllegalArgumentException();
    }
    double c = 0;
    for (double d : data) {
      c += d * d;
    }
    c = Math.sqrt(c);
    for (int i = 0; i < 3; i++) {
      data[i] /= c;
    }
    return c;
  }

  public double getNormalizeParaSH() {
    return normalizeParaSH;
  }

  public double getNormalizeParaSR() {
    return normalizeParaSR;
  }

  public double getTheta1() {
    return results[0];
  }

  public double getTheta2() {
    return results[1];
  }

  public double getTheta3() {
    return results[2];
  }

  public double[] getResults() {
    return Arrays.copyOf(results, 3);
  }
}
