package com.hwaipy.science.polarizationcontrol.approach.annealing;

import com.hwaipy.science.polarizationcontrol.device.FiberTransform;
import com.hwaipy.science.polarizationcontrol.device.Polarization;
import com.hwaipy.science.polarizationcontrol.device.WavePlate;
import java.util.Random;

/**
 *
 * @author Hwaipy
 */
public class AnnealingTest {

    private Random random = new Random();
    private double[] angles = new double[3];
    private FiberTransform ft;
    private WavePlate[] wavePlates = new WavePlate[]{new WavePlate(Math.PI / 2, 0), new WavePlate(Math.PI / 2, 0), new WavePlate(Math.PI, 0)};
    private final double r;
    private final double jC;
    public boolean output = false;

    public AnnealingTest(double r, double jC, double[] startPoint, double[] target) {
        this.r = r;
        this.jC = jC;
        ft = FiberTransform.createReverse(target[0] + Math.PI / 2, target[1] + Math.PI / 2, target[2] + Math.PI / 2);
        for (int i = 0; i < wavePlates.length; i++) {
            wavePlates[i].setTheta(startPoint[i]);
        }
    }

    public class Result {

        public final boolean success;
        public final double[] angles;

        public Result(boolean success, double[] angles) {
            this.success = success;
            this.angles = angles;
        }
    }

    public Result process() {
        println(jointContrastDB() + "");
        double T = 6000;
        double Tmin = 25;
        double lastJ = J();
        double J = J();
        double stepCount = 0;
        while (T > Tmin) {
            move();
            lastJ = J;
            J = J();
            double dE = J - lastJ;
            if (dE > 0) {
            } else if (Math.exp((dE / T)) > random.nextDouble()) {
            } else {
                rollback();
            }
            T = r * T;
            stepCount++;
        }
        return new Result(jointContrastDB() > 30, new double[]{wavePlates[0].getTheta(), wavePlates[1].getTheta(), wavePlates[2].getTheta()});
    }

    private double[] preservedThetas = new double[3];

    private void move() {
        for (int i = 0; i < 3; i++) {
            preservedThetas[i] = wavePlates[i].getTheta();
        }
        int indexOfMove;
        float rf = random.nextFloat();
        if (rf < 1. / 3) {
            indexOfMove = 0;
        } else if (rf < 2. / 3) {
            indexOfMove = 1;
        } else {
            indexOfMove = 2;
        }
        double stepLength = (random.nextDouble() - 0.5) / 10 / 180 * Math.PI;
        wavePlates[indexOfMove].increase(stepLength);
    }

    private void rollback() {
        for (int i = 0; i < 3; i++) {
            wavePlates[i].setTheta(preservedThetas[i]);
        }
    }

    private double[] readPower() {
        Polarization measurementH = Polarization.H.transform(ft)
                .transform(wavePlates[0]).transform(wavePlates[1]).transform(wavePlates[2]);
        Polarization measurementD = Polarization.D.transform(ft)
                .transform(wavePlates[0]).transform(wavePlates[1]).transform(wavePlates[2]);
        return new double[]{
                measurementH.getH(), measurementH.getV(), measurementH.getD(), measurementH.getA(),
                measurementD.getH(), measurementD.getV(), measurementD.getD(), measurementD.getA()};
    }

    private double J() {
        return jointContrastDB() * jC;
    }

    private double jointContrastDB() {
        double jointContrast = jointContrast();
        double db = Math.log10(jointContrast);
        return db * 10;
    }

    private double jointContrast() {
        double[] powers = readPower();
        double contrastH = powers[0] / powers[1];
        double contrastD = powers[6] / powers[7];
        double v = Math.sqrt(1 / contrastH / contrastH + 1 / contrastD / contrastD);
        return 1 / v;
    }

    private void println(String s) {
        if (output) {
            System.out.println(s);
        }
    }

    private static void testRound(double r, double jC, int count) {
        double resultSum = 0;
        double resultMin = 150;
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            double[] startPoint = new double[3];
            double[] target = new double[3];
            for (int j = 0; j < 3; j++) {
                startPoint[j] = (random.nextDouble() - 0.5) / 0.5 * Math.PI;
                target[j] = startPoint[j] + (random.nextDouble() * 0.01);
            }
            AnnealingTest annealingTest = new AnnealingTest(r, jC, startPoint, target);
            annealingTest.process();
            double result = annealingTest.jointContrastDB();
            resultSum += result;
            if (result < resultMin) {
                resultMin = result;
            }
        }
        double resultMean = resultSum / count;
        if (resultMean > 20) {
            System.out.println("R=" + r + "\tJC=" + jC + "\tmean=" + (resultSum / count) + "\tmin=" + resultMin);
        }
    }
}
