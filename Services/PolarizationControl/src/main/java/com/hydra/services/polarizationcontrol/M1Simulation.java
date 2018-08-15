package com.hydra.services.polarizationcontrol;

import com.hwaipy.science.polarizationcontrol.device.MuellerMatrix;
import com.hwaipy.science.polarizationcontrol.device.Polarization;
import com.hwaipy.science.polarizationcontrol.device.WavePlate;
import com.hwaipy.science.polarizationcontrol.m1.M1Process;
import com.hwaipy.science.polarizationcontrol.m1.M1ProcessException;

/**
 *
 * @author HwaipyLab
 */
public class M1Simulation {

    public M1Simulation() {
    }

    public M1SimulationResult calculate(double rH, double rV) {
        return calculate(rH, rV, 0, 0, true, false, 0);
    }

    public M1SimulationResult calculate(double rH, double rV, double phase, double rotate, boolean mir) {
        return calculate(rH, rV, 0, 0, true, false, 0);
    }

    public M1SimulationResult calculate(double rH, double rV, double phase, double rotate, boolean mir, boolean tomoQWP, double AES) {
        return calculate(rH, rV, phase, rotate, mir, tomoQWP, AES, 0, 0, 0);
    }

    public M1SimulationResult calculate(double rH, double rV, double phase, double rotate, boolean mir, boolean tomoQWP, double AES, double phase1, double phase2, double phase3) {
//        rH = -(rH - 32.75);
//        rV = -(rV);
//    TelescopeTransform tt = TelescopeTransform.create(0.2310963252426152, -1.236449209171316, 0.419646965349516, (-45.0001 / 180. * Math.PI), (-20.0000001 / 180. * Math.PI));
//    TelescopeTransform tt = TelescopeTransform.create(0.3110963252426152, -1.236449209171316, 0.419646965349516, (rV / 180. * Math.PI), (rH / 180. * Math.PI));
//    TelescopeTransform tt = TelescopeTransform.create(0.28, -1.33, 0.48, (rV / 180. * Math.PI), (rH / 180. * Math.PI), phase, rotate, mir);
//    TelescopeTransform tt = TelescopeTransform.create(0.43, -1.19, 0.25, (rV / 180. * Math.PI), (rH / 180. * Math.PI), phase, rotate, mir);
        TelescopeTransform tt = TelescopeTransform.create(phase1, phase2, phase3, (rV / 180. * Math.PI), (rH / 180. * Math.PI), phase, rotate, mir, tomoQWP, AES);
        WavePlate qwp1 = new WavePlate(Math.PI / 2, 0);
        WavePlate qwp2 = new WavePlate(Math.PI / 2, 0);
        WavePlate hwp = new WavePlate(Math.PI, 0);

        Polarization measurementH1 = Polarization.H.transform(tt)
                .transform(qwp1).transform(qwp2).transform(hwp);

        double mHH = measurementH1.getH();
        double mHV = measurementH1.getV();
        double mHD = measurementH1.getD();
        double mHA = measurementH1.getA();
        Polarization measurementD1 = Polarization.D.transform(tt)
                .transform(qwp1).transform(qwp2).transform(hwp);
        double mDH = measurementD1.getH();
        double mDV = measurementD1.getV();
        double mDD = measurementD1.getD();
        double mDA = measurementD1.getA();
        qwp2.increase(-Math.PI / 4);
        hwp.increase(-Math.PI / 8);
        Polarization measurementH2 = Polarization.H.transform(tt)
                .transform(qwp1).transform(qwp2).transform(hwp);
        double mHL = measurementH2.getH();
        double mHR = measurementH2.getV();
        Polarization measurementD2 = Polarization.D.transform(tt)
                .transform(qwp1).transform(qwp2).transform(hwp);
        double mDL = measurementD2.getH();
        double mDR = measurementD2.getV();

        M1Process m1Process = null;
        double cH = 0;
        double cD = 0;

//    System.out.println(Arrays.toString(new double[]{mHH, mHV, mHD, mHA, mHL, mHR}));
//    System.out.println(Arrays.toString(new double[]{mDH, mDV, mDD, mDA, mDL, mDR}));
        try {
            m1Process = M1Process.calculate(new double[]{mHH, mHV, mHD, mHA, mHL, mHR, mDH, mDV, mDD, mDA, mDL, mDR});
        } catch (M1ProcessException ex) {
        }
        if (m1Process != null) {
//      double[] result = m1Process.getResults();
//      qwp1.setTheta(15.1 / 180 * Math.PI);
//      qwp2.setTheta(-51.3 / 180 * Math.PI);
//      hwp.setTheta(15.3 / 180 * Math.PI);
//      System.out.println("" + (result[0] / Math.PI * 180 - 15.1));
//      System.out.println("" + (result[1] / Math.PI * 180 + 51.3));
//      System.out.println("" + (result[2] / Math.PI * 180 - 15.3));
//      TelescopeTransform ttReal = TelescopeTransform.create(0.38, -1.33, 0.40, (rV / 180. * Math.PI), (rH / 180. * Math.PI), 60.0 / 180.0 * Math.PI, rotate, mir, tomoQWP);
//      double p = -56;
//      Polarization resultH = Polarization.H.transform(ttReal).transform(qwp1).transform(qwp2).transform(hwp).transform(new WavePlate(p, 0));
//      Polarization resultD = Polarization.D.transform(ttReal).transform(qwp1).transform(qwp2).transform(hwp).transform(new WavePlate(p, 0));
//      cH = resultH.getH() / resultH.getV();
//      cD = resultD.getD() / resultD.getA();
//      System.out.print(cH + "\t" + cD + "\t");
        }
        return new M1SimulationResult(m1Process != null, cH, cD,
                //            (m1Process == null ? 0 : m1Process.getTheta1() / Math.PI * 180 - 15.1),
                //            (m1Process == null ? 0 : m1Process.getTheta2() / Math.PI * 180 + 51.3),
                //            (m1Process == null ? 0 : m1Process.getTheta3() / Math.PI * 180 - 15.3));
                (m1Process == null ? 0 : m1Process.getTheta1()),
                (m1Process == null ? 0 : m1Process.getTheta2()),
                (m1Process == null ? 0 : m1Process.getTheta3()));
    }

    public class M1SimulationResult {

        public final boolean success;
        public final double cH;
        public final double cD;
        public final double[] angles;

        public M1SimulationResult(boolean success, double cH, double cD, double angle1, double angle2, double angle3) {
            this.success = success;
            this.cH = cH;
            this.cD = cD;
            angles = new double[]{angle1, angle2, angle3};
        }

        public double[] getAngles() {
            return angles;
        }
    }
}
