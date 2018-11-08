package com.hwaipy.science.polarizationcontrol;

import com.hwaipy.science.polarizationcontrol.device.WavePlate;
import com.hwaipy.science.polarizationcontrol.device.Polarization;

/**
 *
 * @author Hwaipy
 */
public abstract class PolarizationControlStrategy {

    private boolean completed;
    private Polarization initPolarization = Polarization.H;

    public abstract WavePlate[] getWavePlates();

    public abstract void feedBack(WavePlate[] wavePlates, double pH, double pV, double pD, double pA, double pR, double pL);

    public Polarization getInitPolarization() {
        return initPolarization;
    }

    public boolean completed() {
        return completed;
    }

    protected void switchInitPolarization() {
        if (initPolarization == Polarization.H) {
            initPolarization = Polarization.D;
        } else if (initPolarization == Polarization.D) {
            initPolarization = Polarization.H;
        } else {
            throw new RuntimeException();
        }
    }

    protected void setInitPolarization(Polarization ip) {
        initPolarization = ip;
    }

    protected void complete() {
        completed = true;
    }
}
