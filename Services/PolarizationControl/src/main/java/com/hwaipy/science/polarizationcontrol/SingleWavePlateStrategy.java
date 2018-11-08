package com.hwaipy.science.polarizationcontrol;

import com.hwaipy.science.polarizationcontrol.device.WavePlate;
import com.hwaipy.science.polarizationcontrol.device.HalfWavePlate;

/**
 *
 * @author Hwaipy
 */
public class SingleWavePlateStrategy extends PolarizationControlStrategy {

    @Override
    public WavePlate[] getWavePlates() {
        return new WavePlate[]{new HalfWavePlate(0)};
    }
    private double lastC = -1;
    private double step = Math.PI / 180;

    @Override
    public void feedBack(WavePlate[] wavePlates, double pH, double pV, double pD, double pA, double pR, double pL) {
        WavePlate wavePlate = wavePlates[0];
        double c = pV / pH;
        if (lastC < -0.5) {
            lastC = c;
        } else {
            if (lastC > c) {
            } else {
            }
        }
        wavePlate.increase(step);
    }
}
