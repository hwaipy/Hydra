package com.hwaipy.science.polarizationcontrol;

import com.hwaipy.science.polarizationcontrol.device.WavePlate;
import com.hwaipy.science.polarizationcontrol.device.FiberTransform;
import com.hwaipy.science.polarizationcontrol.device.Polarization;
import java.util.Random;

/**
 *
 * @author Hwaipy
 */
public class PolarizationController {

    private final PolarizationControlStrategy strategy;
    private final WavePlate[] wavePlates;
    private final FiberTransform fiberTransform;

    public PolarizationController(PolarizationControlStrategy strategy, FiberTransform fiberTransform) {
        this.strategy = strategy;
        wavePlates = strategy.getWavePlates();
        this.fiberTransform = fiberTransform;
    }

    public PolarizationController(PolarizationControlStrategy strategy) {
        this(strategy, FiberTransform.createRandomFiber(new Random()));
    }

    public Polarization transform(Polarization polarization) {
        for (WavePlate wavePlate : wavePlates) {
            polarization.transform(wavePlate);
        }
        return polarization;
    }

    public void randomizeWavePlates() {
        for (WavePlate wavePlate : wavePlates) {
            wavePlate.randomize();
        }
    }

    public void control() {
        while (!strategy.completed()) {
            Polarization initPolarization = strategy.getInitPolarization();
            Polarization p = transform(initPolarization);
            strategy.feedBack(wavePlates, p.getH(), p.getV(), p.getD(), p.getA(), p.getR(), p.getL());
        }
    }
}
