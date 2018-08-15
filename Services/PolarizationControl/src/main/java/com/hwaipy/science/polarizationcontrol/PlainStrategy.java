package com.hwaipy.science.polarizationcontrol;

import com.hwaipy.science.polarizationcontrol.device.WavePlate;
import com.hwaipy.science.polarizationcontrol.device.QuarterWavePlate;
import com.hwaipy.science.polarizationcontrol.device.HalfWavePlate;
import com.hwaipy.science.polarizationcontrol.device.Polarization;

/**
 *
 * @author Hwaipy
 */
public class PlainStrategy extends PolarizationControlStrategy {

    @Override
    public WavePlate[] getWavePlates() {
        return new WavePlate[]{new QuarterWavePlate(0), new QuarterWavePlate(0), new HalfWavePlate(0)};
    }

    @Override
    public void feedBack(WavePlate[] wavePlates, double pH, double pV, double pD, double pA, double pR, double pL) {
        WavePlate wavePlate1;
        Polarization ip = getInitPolarization();
        if (ip == Polarization.H) {
            wavePlate1 = wavePlates[0];
        } else if (ip == Polarization.D) {
            wavePlate1 = wavePlates[1];
        } else {
            throw new RuntimeException();
        }
        WavePlate wavePlate2 = wavePlates[2];
    }
}
