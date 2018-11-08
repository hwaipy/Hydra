package com.hwaipy.science.polarizationcontrol.device;

/**
 *
 * @author Hwaipy
 */
public class HalfWavePlate extends WavePlate {

    public HalfWavePlate(double theta) {
        super(Math.PI, theta);
    }
//    @Override
//    protected Matrix recalculation(double theta) {
//        double theta4 = theta * 4;
//        double c4 = Math.cos(theta4);
//        double s4 = Math.sin(theta4);
//        Matrix matrix = new Matrix(new double[][]{
//            {1, 0, 0, 0},
//            {0, c4, s4, 0},
//            {0, s4, -c4, 0},
//            {0, 0, 0, -1}
//        });
//        return matrix;
//    }
}
