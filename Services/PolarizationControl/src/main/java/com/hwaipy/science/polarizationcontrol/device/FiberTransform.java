package com.hwaipy.science.polarizationcontrol.device;

import Jama.Matrix;
import java.util.Random;

/**
 *
 * @author Hwaipy
 */
public class FiberTransform extends MuellerMatrix {

    private FiberTransform(Matrix matrix) {
        super(matrix);
    }
//    private static final Random RANDOM = new Random(System.nanoTime());

    public static FiberTransform createRandomFiber(Random random) {
        HalfWavePlate hwp = new HalfWavePlate(random.nextDouble() * Math.PI);
        QuarterWavePlate qwp2 = new QuarterWavePlate(random.nextDouble() * Math.PI);
        QuarterWavePlate qwp1 = new QuarterWavePlate(random.nextDouble() * Math.PI);
        return new FiberTransform(MuellerMatrix.merge(hwp, qwp2, qwp1).getMatrix());
    }

    public static FiberTransform createReverse(double qwp1, double qwp2, double hwp) {
        return new FiberTransform(MuellerMatrix.merge(
                new HalfWavePlate(hwp), new QuarterWavePlate(qwp2), new QuarterWavePlate(qwp1)).getMatrix());
    }
}
