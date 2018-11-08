import math
import array


class AWGEncoder:
    def __init__(self):
        self.sampleRate = 25e9
        self.pulseWidth = 2e-9
        self.repetationRate = 100e6
        self.firstPulseMode = False
        self.period = 1 / self.repetationRate
        self.ampSignalTime = 1
        self.ampSignalPhase = 0.5
        self.ampDecoyTime = 0.4
        self.ampDecoyPhase = 0.2
        self.pulseDiff = 3e-9
        self.delays = {'AMDecoy': 0, 'AMTime1': 0, 'AMTime2': 0, 'PM': 0, 'Laser': 0, 'Sync': 0}
        self.enable = {'AMDecoy': True, 'AMTime1': True, 'AMTime2': True, 'PM': True, 'Laser': True, 'Sync': True}

    # Defination of Random Number:
    # parameter ``randomNumbers'' should be a list of RN
    # RN is a list with length of 3.
    # The first element (0, 1, 2) represent for (Vacumn, Decoy, Signal)
    # The second element (0, 1) represent for basis (Time, Phase)
    # The third element (0, 1) represent for encoding (0, 1)
    def generateWaveforms(self, randomNumbers):
        return {
            'AMDecoy': self._generateWaveform(randomNumbers, self._decoyWaveformAmp, 'AMDecoy'),
            'AMTime1': self._generateWaveform(randomNumbers, self._timeWaveformAmp, 'AMTime1'),
            'AMTime2': self._generateWaveform(randomNumbers, self._timeWaveformAmp, 'AMTime2'),
            'PM': self._generateWaveform(randomNumbers, self._phaseWaveformAmp, 'PM'),
            'Laser': self._generateWaveform(randomNumbers, self._laserWaveformAmp, 'Laser'),
            'Sync': self._generateWaveform(randomNumbers, self._syncWaveformAmp, 'Sync')
        }

    # Marker, 1 bit
    def _laserWaveformAmp(self, timeInPulse, pulseIndex, randomNumber):
        return 1 if ((timeInPulse <= self.pulseWidth) and ((not self.firstPulseMode) or (pulseIndex == 0))) else 0

    # Main, -1 ~ 1 float.
    def _decoyWaveformAmp(self, timeInPulse, pulseIndex, randomNumber):
        if timeInPulse > self.pulseWidth:
            amp = 0
        elif randomNumber[0] == 0:
            amp = 0
        elif randomNumber[0] == 1:
            amp = self.ampDecoyTime if randomNumber[1] == 0 else self.ampDecoyPhase
        else:
            amp = self.ampSignalTime if randomNumber[1] == 0 else self.ampSignalPhase
        return amp * 2 - 1

    # Marker, 1 bit
    def _timeWaveformAmp(self, timeInPulse, pulseIndex, randomNumber):
        inPulse1 = (timeInPulse >= 0) and (timeInPulse < self.pulseWidth)
        inPulse2 = (timeInPulse >= self.pulseDiff) and (timeInPulse < self.pulseDiff + self.pulseWidth)
        if (not inPulse1) and (not inPulse2):
            amp = 0
        elif randomNumber[0] == 0:
            amp = 0
        elif randomNumber[1] == 1:
            amp = 1
        elif inPulse1:
            amp = 1 if randomNumber[2] == 0 else 0
        else:
            amp = 1 if randomNumber[2] == 1 else 0
        return amp

    # Marker, 1 bit
    def _phaseWaveformAmp(self, timeInPulse, pulseIndex, randomNumber):
        inPulse1 = (timeInPulse >= 0) and (timeInPulse < self.pulseWidth)
        # inPulse2 = (timeInPulse >= self.pulseDiff) and (timeInPulse < self.pulseDiff + self.pulseWidth)
        amp = -1
        if inPulse1 and randomNumber[2] == 0 and randomNumber[0] != 0:
            amp = 1
        return amp

    # Marker, 1 bit
    def _syncWaveformAmp(self, timeInPulse, pulseIndex, randomNumber):
        return 1 if pulseIndex <= 5 else 0

    def _generateWaveform(self, randomNumbers, amp, name):
        wf = [amp((i / self.sampleRate) % self.period, int(i / self.sampleRate / self.period),
                  randomNumbers[int(i / self.sampleRate / self.period)]) for i in
              range(0, int(len(randomNumbers) * self.period * self.sampleRate))]
        delaySample = -math.floor(self.delays[name] * self.sampleRate + 0.5)
        if not self.enable[name]:
            return [0 for w in wf]
        return wf[delaySample:] + wf[:delaySample]
        return ''


RND_ST0 = 12
RND_ST1 = 13
RND_SP0 = 14
RND_SP1 = 15
RND_DT0 = 8
RND_DT1 = 9
RND_DP0 = 10
RND_DP1 = 11
RND_VT0 = 0
RND_VT1 = 1
RND_VP0 = 2
RND_VP1 = 3

if __name__ == '__main__':
    import time
    import os

    start = time.time()

    # dataRoot = '/Users/hwaipy/GitHub/Hydra/Soap/MDI-QKD/AWGWaveformCreator/'
    dataRoot = './'

    rns = ([0, 1, 2, 3, 8, 9, 10, 11, 12, 13, 14, 15] * 1000)[:10000]
    rns.reverse()
    rndFile = open('{}/RNDs'.format(dataRoot), 'wb')
    rndFile.write(bytearray(rns))
    rndFile.close()

    timeParameters = {
        'delayAMDecoy': 0,  # ns
        'delaySync': 0,  # ns
        'delayLaser': 0,  # ns
        'delayPM': 0,  # ns
        'delayAMTime1': 0,  # ns
        'delayAMTime2': 0,  # ns
        'pulseWidth': 2,  # ns
        'laserPulseWidth': 2.5  # ns
    }
    modeParameters = {
        'firstLaserPulseMode': False,
        'firstModulationPulseMode': False,
        'specifiedRandomNumberMode': False,
        'specifiedRandomNumber': 3}
    amplituteParameters = {  # should be a float between -1 and 1
        'amplituteSignalTime': 1,
        'amplituteSignalPhase': 0,
        'amplituteDecoyTime': -0.2,
        'amplituteDecoyPhase': -0.9,
    }

    parameters = []
    for key in timeParameters.keys():
        parameters.append('{}={}'.format(key, timeParameters[key] * 1e-9))
    for key in modeParameters.keys():
        if key.endswith('Mode'):
            parameters.append('{}={}'.format(key, 'true' if modeParameters[key] else 'false'))
        else:
            parameters.append('{}={}'.format(key, modeParameters[key]))
    for key in amplituteParameters.keys():
        parameters.append('{}={}'.format(key, amplituteParameters[key]))
    args = ' '.join(parameters)
    print(args)
    os.system("java -jar awgwaveformcreator_2.12-0.1.0.jar {}".format(args))

    waveformNames = 'AMDecoy', 'Laser', 'Sync', 'PM', 'AMTime1', 'AMTime2'
    path = '{}/test.wave'.format(dataRoot)
    fileSize = os.path.getsize(path)
    waveformSize = int(fileSize / 6)
    waveforms = {}
    file = open(path, 'br')
    for waveformName in waveformNames:
        bytes = file.read(waveformSize)
        waveforms[waveformName] = [b for b in bytes]

    stop = time.time()
    print('{} s!'.format(stop - start))

    import matplotlib.pyplot as plt

    wf = waveforms['AMDecoy']
    # wf = waveforms['Laser']
    # wf = waveforms['Sync']
    # wf = waveforms['PM']
    # wf = waveforms['AMTime1']
    # wf = waveforms['AMTime2']
    plt.plot([i / 25.0 for i in range(0, len(wf))], wf)
    plt.show()
