package com.hydra.services.polarizationcontrol;

import Jama.Matrix;
import com.hwaipy.science.polarizationcontrol.device.MuellerMatrix;
import com.hwaipy.science.polarizationcontrol.device.Rotate;
import com.hwaipy.science.polarizationcontrol.device.WavePlate;

/**
 *
 * @author Hwaipy
 */
public class TelescopeTransform extends MuellerMatrix {

  private TelescopeTransform(Matrix matrix) {
    super(matrix);
  }

//  public static TelescopeTransform createRandomTelescope(Random random) {
//    HalfWavePlate hwp = new HalfWavePlate(random.nextDouble() * Math.PI);
//    QuarterWavePlate qwp2 = new QuarterWavePlate(random.nextDouble() * Math.PI);
//    QuarterWavePlate qwp1 = new QuarterWavePlate(random.nextDouble() * Math.PI);
//    return new TelescopeTransform(MuellerMatrix.merge(hwp, qwp2, qwp1).getMatrix());
//  }
  public static TelescopeTransform create(double PA, double PB, double PC, double RV, double RH, boolean mir) {
    return create(PA, PB, PC, RV, RH, 0, 0, mir, false, 0);
  }

  public static TelescopeTransform create(double PA, double PB, double PC, double RV, double RH, double phase, double rotate, boolean mir, boolean tomoQWP, double AES) {
    return new TelescopeTransform(MuellerMatrix.merge(
            new Rotate(AES),
            //            new WavePlate(tomoQWP ? -Math.PI / 2 : 0, Math.PI / 4),
            new WavePlate(phase, 0),
            new Rotate(rotate),
            //            new WavePlate(mir ? Math.PI : 0, Math.PI / 4),
            //                                    new WavePlate(Math.PI, Math.PI / 4),
            new WavePlate(PA, 0),
            new Rotate(RV),
            new WavePlate(PB, 0),
            new Rotate(RH),
            new WavePlate(PC, 0)
    ).getMatrix());
  }
}
