__author__ = 'Hwaipy'

from Instruments import DeviceException, Instrument


class VirtualDCVoltageSource(Instrument):
    def __init__(self, voltageRanges=[30, 30, 6], currentLimitRanges=[3, 3, 3], loads=[50, 50, 50],
                 serialNumber="SN:VirtualDCVoltageSource:20180618:001"):
        self.channelCount = len(voltageRanges)
        self.voltageRanges = [vr for vr in voltageRanges]
        self.voltageSetpoints = [0] * self.channelCount
        self.outputStatuses = [False] * self.channelCount
        self.currentLimitRange = [clr for clr in currentLimitRanges]
        self.currentLimits = [clr for clr in currentLimitRanges]
        self.loads = loads
        self.serialNumber = serialNumber
        if len(voltageRanges) != len(currentLimitRanges):
            raise DeviceException("Length of voltageRanges and currentLimitRanges should be the same.")
        if len(voltageRanges) != len(loads):
            raise DeviceException("Length of voltageRanges and loads should be the same.")

    def __checkChannel(self, channel):
        if channel < 0 or channel >= self.channelCount:
            raise DeviceException("channel can only be a Int in [0, {})".format(self.channelCount))

    def getManufacturer(self):
        return "Pydra"

    def getModel(self):
        return "Virtual DC Voltage Source"

    def getSerialNumber(self):
        return self.serialNumber

    def isAvailable(self):
        return True

    def getChannelNumber(self):
        return self.channelCount

    def getVoltageRange(self, channel):
        self.__checkChannel(channel)
        return self.voltageRanges[channel]

    def getCurrentLimitRange(self, channel):
        self.__checkChannel(channel)
        return self.currentLimitRange[channel]

    def setVoltage(self, channel, voltage):
        self.__checkChannel(channel)
        if voltage < 0:
            voltage = 0
        if voltage > self.voltageRanges[channel]:
            voltage = self.voltageRanges[channel]
        self.voltageSetpoints[channel] = voltage

    def setVoltages(self, voltages):
        if len(voltages) != self.getChannelNumber():
            raise DeviceException("voltages should have length of {}".format(self.getChannelNumber()))
        for i in range(0, self.getChannelNumber()):
            self.setVoltage(i, voltages[i])

    def getVoltageSetPoint(self, channel):
        self.__checkChannel(channel)
        return self.voltageSetpoints[channel]

    def setCurrentLimit(self, channel, current):
        self.__checkChannel(channel)
        if current < 0:
            current = 0
        if current > self.currentLimitRange[channel]:
            current = self.currentLimitRange[channel]
        self.currentLimits[channel] = current

    def setCurrentLimits(self, currents):
        if len(currents) != self.getChannelNumber():
            raise DeviceException("currents should have length of {}".format(self.getChannelNumber()))
        for i in range(0, self.getChannelNumber()):
            self.setCurrentLimit(i, currents[i])

    def getCurrentLimitSetPoint(self, channel):
        self.__checkChannel(channel)
        return self.currentLimits[channel]

    def setOutputStatus(self, channel, status):
        self.__checkChannel(channel)
        self.outputStatuses[channel] = status

    def setOutputStatuses(self, statuses):
        if len(statuses) != self.getChannelNumber():
            raise DeviceException("statuses should have length of {}".format(self.getChannelNumber()))
        for i in range(0, self.getChannelNumber()):
            self.setOutputStatus(i, statuses[i])

    def getOutputStatus(self, channel):
        self.__checkChannel(channel)
        return self.outputStatuses[channel]

    def __measure(self, channel):
        self.__checkChannel(channel)
        VSet = self.voltageSetpoints[channel]
        load = self.loads[channel]
        ILimit = self.currentLimits[channel]
        IExpect = VSet / load
        if IExpect <= ILimit:
            return [VSet, ILimit]
        else:
            return [ILimit * load, ILimit]

    def measureVoltage(self, channel):
        self.__checkChannel(channel)
        return self.__measure(channel)[0]

    def measureVoltages(self):
        return [self.measureVoltage(c) for c in range(0, self.getChannelNumber())]

    def measureCurrent(self, channel):
        self.__checkChannel(channel)
        return self.__measure(channel)[1]

    def measureCurrents(self):
        return [self.measureCurrent(c) for c in range(0, self.getChannelNumber())]


if __name__ == '__main__':
    import sys
    import Utils
    import Pydra

    sysArgs = Utils.SystemArguments(sys.argv)
    server = sysArgs.get('server', 'localhost')
    port = sysArgs.get('port', '20102')
    wydraPort = sysArgs.get('wydraPort', '20080')
    clientName = sysArgs.get('clientName', 'test')


    def getSummary():
        return "This is a Virtual DC Voltage Source Instrument."


    def getDocument():
        md = ''.join(open('VirtualDCVoltageSource.md').readlines())
        return md


    invoker = VirtualDCVoltageSource()
    invoker.getSummary = getSummary
    invoker.getDocument = getDocument

    session = Pydra.Session.newSession((server, int(port)), invoker, clientName)

    print('VirtualDCSupply started as {}'.format(clientName))

    while True:
        if sys.stdin.readline() == 'q\n':
            print('Stopping VirtualDCSupply.')
            session.stop()
            break
