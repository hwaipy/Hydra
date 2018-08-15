package com.hwaipy.science.polarizationcontrol.device;

import Jama.Matrix;

/**
 *
 * @author Hwaipy
 */
public class Rotate extends MuellerMatrix {

  private double theta = 0;
//    private static final Random RANDOM = new Random(System.nanoTime());

  public Rotate(double theta) {
    super(null);
    this.theta = theta;
    setMatrix(recalculation());
  }

  private Matrix recalculation() {
    double theta2 = theta * 2;
    double c2 = Math.cos(theta2);
    double s2 = Math.sin(theta2);
    Matrix matrix = new Matrix(new double[][]{
      {1, 0, 0, 0},
      {0, c2, s2, 0},
      {0, -s2, c2, 0},
      {0, 0, 0, 1}
    });
    return matrix;
  }
}
