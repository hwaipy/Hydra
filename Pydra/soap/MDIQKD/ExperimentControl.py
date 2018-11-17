import Pydra


class ExperimentControl:
    def __init__(self):
        self.session = Pydra.Session.newSession(('192.168.25.27', 20102), None, 'MDI-QKD-Controller-Test')

    def testAWGControl(self, name):
        awg = self.session.blockingInvoker('AWG-MDI-' + name)

        # Below is the parameters for configure the AWG

        # Time Parameters. All values should be a float (ns):
        # 'delayAMDecoy'
        # 'delaySync'
        # 'delayLaser'
        # 'delayPM'
        # 'delayAMTime1'
        # 'delayAMTime2'
        # 'pulseWidth'
        # 'laserPulseWidth'
        # 'interferometerDiff'

        # Mode Parameters. All values should be True or False except 'specifiedRandomNumber', which should be a Integer.
        # !!!Important Note: As the TDCParser is online now, these mode parameters should all be False.
        # 'firstLaserPulseMode'
        # 'firstModulationPulseMode'
        # 'specifiedRandomNumberMode'
        # 'specifiedRandomNumber'

        # Amplitute Parameters. All values should be a float between -1 and 1
        # 'amplituteSignalTime'
        # 'amplituteSignalPhase'
        # 'amplituteDecoyTime'
        # 'amplituteDecoyPhase'
        # 'amplitutePM'

        awg.configure('amplituteSignalTime', 0)
        awg.configure('delayAMDecoy', 5.5)
        awg.setRandomNumbers([0, 1, 2, 3, 8, 9, 10, 11, 12, 13, 14, 15])
        awg.generateNewWaveform()

    def stop(self):
        self.session.stop()

    def test(self):
        awg = self.session.blockingInvoker('AWG-MDI-Bob')
        awg .configure('firstLaserPulseMode', True)
        awg .configure('specifiedRandomNumberMode', False)
        awg .configure('specifiedRandomNumber', 14)
        awg.configure('syncWidth', 300.0)
        awg.configure('syncPeriod', 8000.0)

        awg .setRandomNumbers([0,1,2,3,8,9,10,11,12,13,14,15]*100)
        awg .generateNewWaveform()


if __name__ == '__main__':
    ec = ExperimentControl()
    # ec.testAWGControl('Alice')

    ec.test()



    ec.stop()
