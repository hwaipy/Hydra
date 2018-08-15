package com.hwaipy.science.polarizationcontrol.device;

import Jama.Matrix;

/**
 *
 * @author Hwaipy
 */
public class MuellerMatrix {

    private Matrix matrix;

    public MuellerMatrix(Matrix matrix) {
        this.matrix = matrix;
    }

    public Matrix getMatrix() {
        return matrix;
    }

    protected void setMatrix(Matrix newMatrix) {
        this.matrix = newMatrix;
    }

    public static MuellerMatrix merge(MuellerMatrix... matrixs) {
        Matrix matrix = new Matrix(new double[][]{
            {1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}});
        for (MuellerMatrix muellerMatrix : matrixs) {
            matrix = muellerMatrix.getMatrix().times(matrix);
        }
        return new MuellerMatrix(matrix);
    }
}
