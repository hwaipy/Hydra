package com.hwaipy.science.polarizationcontrol.device;

import Jama.Matrix;
import java.util.Random;

/**
 *
 * @author Hwaipy
 */
public class WavePlate extends MuellerMatrix {

    private final double delay;
    private double theta = 0;
    private static final Random RANDOM = new Random(System.nanoTime());

    public WavePlate(double delay, double theta) {
        super(null);
        this.delay = delay;
        this.theta = theta;
        setMatrix(recalculation());
    }

    public void setTheta(double theta) {
        this.theta = theta;
        Matrix m = recalculation();
        setMatrix(m);
    }

    public double getTheta() {
        return theta;
    }

    public void increase(double step) {
        this.theta += step;
        Matrix m = recalculation();
        setMatrix(m);
    }

    public void randomize() {
        setTheta(RANDOM.nextDouble() * Math.PI);
    }

    private Matrix recalculation() {
        double theta2 = theta * 2;
        double c2 = Math.cos(theta2);
        double s2 = Math.sin(theta2);
        double s2c2 = s2 * c2;
        double cd = Math.cos(delay);
        double sd = Math.sin(delay);
        Matrix matrix = new Matrix(new double[][]{
            {1, 0, 0, 0},
            {0, 1 - (1 - cd) * s2 * s2, (1 - cd) * s2c2, -sd * s2},
            {0, (1 - cd) * s2c2, 1 - (1 - cd) * c2 * c2, sd * c2},
            {0, sd * s2, -sd * c2, cd}
        });
        return matrix;
    }
}
