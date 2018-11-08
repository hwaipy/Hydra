package com.hwaipy.science.polarizationcontrol.device;

import Jama.Matrix;

/**
 *
 * @author Hwaipy
 */
public class Polarization implements Cloneable {

    private Matrix stokes;

    public Polarization(double H, double D, double R) {
        stokes = new Matrix(new double[][]{{1}, {H}, {D}, {R}});
    }

    public Polarization transform(MuellerMatrix muellerMatrix) {
        Polarization clone = clone();
        Matrix matrix = muellerMatrix.getMatrix();
        clone.stokes = matrix.times(stokes);
        return clone;
    }

    @Override
    public Polarization clone() {
        return new Polarization(stokes.get(1, 0), stokes.get(2, 0), stokes.get(3, 0));
    }

    public double getH() {
        return (1 + stokes.get(1, 0)) / 2;
    }

    public double getV() {
        return (1 - stokes.get(1, 0)) / 2;
    }

    public double getD() {
        return (1 + stokes.get(2, 0)) / 2;
    }

    public double getA() {
        return (1 - stokes.get(2, 0)) / 2;
    }

    public double getR() {
        return (1 + stokes.get(3, 0)) / 2;
    }

    public double getL() {
        return (1 - stokes.get(3, 0)) / 2;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(stokes.get(0, 0)).append(", ")
                .append(stokes.get(1, 0)).append(", ")
                .append(stokes.get(2, 0)).append(", ")
                .append(stokes.get(3, 0)).append(")");
        return sb.toString();
    }
    public static final Polarization H = new Polarization(1, 0, 0);
    public static final Polarization V = new Polarization(-1, 0, 0);
    public static final Polarization D = new Polarization(0, 1, 0);
    public static final Polarization A = new Polarization(0, -1, 0);
    public static final Polarization R = new Polarization(0, 0, 1);
    public static final Polarization L = new Polarization(0, 0, -1);
    public static final Polarization ZERO = new Polarization(0, 0, 0);
}
