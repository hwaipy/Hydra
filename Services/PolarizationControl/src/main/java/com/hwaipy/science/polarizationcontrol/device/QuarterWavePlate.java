package com.hwaipy.science.polarizationcontrol.device;

/**
 *
 * @author Hwaipy
 */
public class QuarterWavePlate extends WavePlate {

    public QuarterWavePlate(double theta) {
        super(Math.PI / 2, theta);
    }
//    @Override
//    protected Matrix recalculation(double theta) {
//        double theta2 = theta * 2;
//        double c2 = Math.cos(theta2);
//        double s2 = Math.sin(theta2);
//        double s2c2 = s2 * c2;
//        Matrix matrix = new Matrix(new double[][]{
//            {1, 0, 0, 0},
//            {0, c2 * c2, s2c2, -s2},
//            {0, s2c2, s2 * s2, c2},
//            {0, s2, -c2, 0}
//        });
//        return matrix;
//    }
}
