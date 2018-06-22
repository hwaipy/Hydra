__author__ = 'Hwaipy'
__version__ = 'v1.0.20180618'

from Instruments import DeviceException, Instrument
import visa
from SCPI import SCPI
from Instruments import DeviceException, VISAInstrument
from Utils import SingleThreadProcessor
import math


class IT6322(VISAInstrument):
    manufacturer = 'ITECH Ltd.'
    model = 'IT6322'

    def __init__(self, resourceID):
        super().__init__(resourceID, 3)
        self.__remote()
        self.voltageSetpoints = self.getVoltageSetpoints()
        self.currentLimits = self.getCurrentLimits()
        self.outputStatuses = self.getOutputStatuses()

    def __remote(self):
        return self.scpi.SYSTem.REMote.write()
        ##SYSTem:RWLock[:STATe]

    # def getManufacturer(self):
    #     return "Pydra"
    #
    # def getModel(self):
    #     return "Virtual DC Voltage Source"
    #
    # def getSerialNumber(self):
    #     return self.serialNumber
    #
    # def isAvailable(self):
    #     return True
    #
    # def getChannelNumber(self):
    #     return self.channelCount
    #
    # def getVoltageRange(self, channel):
    #     self.__checkChannel(channel)
    #     return self.voltageRanges[channel]
    #
    # def getCurrentLimitRange(self, channel):
    #     self.__checkChannel(channel)
    #     return self.currentLimitRange[channel]
    #
    # def setVoltage(self, channel, voltage):
    #     self.__checkChannel(channel)
    #     if voltage < 0:
    #         voltage = 0
    #     if voltage > self.voltageRanges[channel]:
    #         voltage = self.voltageRanges[channel]
    #     self.voltageSetpoints[channel] = voltage
    #
    # def setVoltages(self, voltages):
    #     if len(voltages) != self.getChannelNumber():
    #         raise DeviceException("voltages should have length of {}".format(self.getChannelNumber()))
    #     for i in range(0, self.getChannelNumber()):
    #         self.setVoltage(i, voltages[i])
    #
    # def getVoltageSetPoint(self, channel):
    #     self.__checkChannel(channel)
    #     return self.voltageSetpoints[channel]
    #
    # def setCurrentLimit(self, channel, current):
    #     self.__checkChannel(channel)
    #     if current < 0:
    #         current = 0
    #     if current > self.currentLimitRange[channel]:
    #         current = self.currentLimitRange[channel]
    #     self.currentLimits[channel] = current
    #
    # def setCurrentLimits(self, currents):
    #     if len(currents) != self.getChannelNumber():
    #         raise DeviceException("currents should have length of {}".format(self.getChannelNumber()))
    #     for i in range(0, self.getChannelNumber()):
    #         self.setCurrentLimit(i, currents[i])
    #
    # def getCurrentLimitSetPoint(self, channel):
    #     self.__checkChannel(channel)
    #     return self.currentLimits[channel]
    #
    # def setOutputStatus(self, channel, status):
    #     self.__checkChannel(channel)
    #     self.outputStatuses[channel] = status
    #
    # def setOutputStatuses(self, statuses):
    #     if len(statuses) != self.getChannelNumber():
    #         raise DeviceException("statuses should have length of {}".format(self.getChannelNumber()))
    #     for i in range(0, self.getChannelNumber()):
    #         self.setOutputStatus(i, statuses[i])
    #
    # def getOutputStatus(self, channel):
    #     self.__checkChannel(channel)
    #     return self.outputStatuses[channel]
    #
    # def __measure(self, channel):
    #     self.__checkChannel(channel)
    #     VSet = self.voltageSetpoints[channel]
    #     load = self.loads[channel]
    #     ILimit = self.currentLimits[channel]
    #     IExpect = VSet / load
    #     if IExpect <= ILimit:
    #         return [VSet, ILimit]
    #     else:
    #         return [ILimit * load, ILimit]
    #
    # def measureVoltage(self, channel):
    #     self.__checkChannel(channel)
    #     return self.__measure(channel)[0]
    #
    # def measureVoltages(self):
    #     return [self.measureVoltage(c) for c in range(0, self.getChannelNumber())]
    #
    # def measureCurrent(self, channel):
    #     self.__checkChannel(channel)
    #     return self.__measure(channel)[1]
    #
    # def measureCurrents(self):
    #     return [self.measureCurrent(c) for c in range(0, self.getChannelNumber())]


if __name__ == '__main__':
    import sys
    import Utils
    import Pydra

    # sysArgs = Utils.SystemArguments(sys.argv)
    # server = sysArgs.get('server', 'localhost')
    # port = sysArgs.get('port', '20102')
    # wydraPort = sysArgs.get('wydraPort', '20080')
    # clientName = sysArgs.get('clientName', 'test')
    #
    #
    # def getSummary():
    #     return "This is a Virtual DC Voltage Source Instrument."
    #
    #
    # def getDocument():
    #     md = ''.join(open('VirtualDCVoltageSource.md').readlines())
    #     return md
    #
    #
    # invoker = VirtualDCVoltageSource()
    # invoker.getSummary = getSummary
    # invoker.getDocument = getDocument
    #
    # session = Pydra.Session.newSession((server, int(port)), invoker, clientName)
    #
    # print('VirtualDCSupply started as {}'.format(clientName))
    #
    # while True:
    #     if sys.stdin.readline() == 'q\n':
    #         print('Stopping VirtualDCSupply.')
    #         session.stop()
    #         break


    def getIdentity(self):
        idn = self.scpi._IDN.query()
        if idn is None:
            return [''] * 4
        if len(idn) is 0:
            return [''] * 4
        idns = idn.split(',')
        idns = [idn.strip(' ') for idn in idns]
        while len(idns) < 4:
            idns.append('')
        return idns[:4]

    def getVersion(self):
        return self.scpi.SYSTem.VERSion.query()

    def beeper(self):
        self.scpi.SYSTem.BEEPer.write()
        self.getIdentity()

    def measureVoltages(self):
        voltageString = self.scpi.MEAS.VOLT.ALL.query().split(', ')
        voltages = [float(vs) for vs in voltageString]
        return voltages

    def measureCurrents(self):
        currentString = self.scpi.MEAS.CURR.ALL.query().split(', ')
        currents = [float(vs) for vs in currentString]
        return currents

    def getVoltageSetpoints(self):
        vs = self.scpi.APP.VOLT.query()
        vs = vs.split(', ')
        self.voltageSetpoints = [float(v) for v in vs]
        return self.voltageSetpoints

    def getCurrentLimits(self):
        cs = self.scpi.APP.CURR.query()
        cs = cs.split(', ')
        self.currentLimits = [float(v) for v in cs]
        return self.currentLimits

    def setVoltages(self, voltages):
        if len(voltages) is not self.channelCount:
            raise DeviceException('Length of voltages do not match the channel number.')
        outputCodeString = ['{}'.format(v) for v in voltages]
        outputCode = ', '.join(outputCodeString)
        self.scpi.APP.VOLT.write(outputCode)
        setted = self.getVoltageSetpoints()
        same = [math.fabs(voltages[i] - setted[i]) < 0.001 for i in range(self.channelCount)]
        if sum(same) is not self.channelCount:
            raise DeviceException('Voltage out of range.')

    def setVoltage(self, channel, voltage):
        self.checkChannel(channel)
        voltages = self.voltageSetpoints.copy()
        voltages[channel] = voltage
        self.setVoltages(voltages)

    def setCurrentLimits(self, currents):
        if len(currents) is not self.channelCount:
            raise DeviceException('Length of currents do not match the channel number.')
        outputCodeString = ['{}'.format(v) for v in currents]
        outputCode = ', '.join(outputCodeString)
        self.scpi.APP.CURR.write(outputCode)
        setted = self.getCurrentLimits()
        same = [math.fabs(currents[i] - setted[i]) < 0.001 for i in range(self.channelCount)]
        if sum(same) is not self.channelCount:
            raise DeviceException('Current out of range.')

    def setCurrentLimit(self, channel, currentLimit):
        self.checkChannel(channel)
        currentLimits = self.currentLimits.copy()
        currentLimits[channel] = currentLimit
        self.setCurrentLimits(currentLimits)

    def getOutputStatuses(self):
        os = self.scpi.APP.OUT.query()
        os = os.split(', ')
        self.outputStatuses = [o == '1' for o in os]
        return self.outputStatuses

    def setOutputStatuses(self, outputStatuses):
        if len(outputStatuses) is not self.channelCount:
            raise DeviceException('Length of outputStatuses do not match the channel number.')
        outputCodeString = ['{}'.format(1 if v else 0) for v in outputStatuses]
        outputCode = ', '.join(outputCodeString)
        self.scpi.APP.OUT.write(outputCode)
        setted = self.getOutputStatuses()
        same = [outputStatuses[i] == setted[i] for i in range(self.channelCount)]
        if sum(same) is not self.channelCount:
            raise DeviceException('OutputStatus error.')

    def setOutputStatus(self, channel, outputStatus):
        self.checkChannel(channel)
        outputStatuses = self.outputStatuses.copy()
        outputStatuses[channel] = outputStatus
        self.setOutputStatuses(outputStatuses)

    def reset(self):
        self.scpi._RST.write()



if __name__ == '__main__':
    print('BKPrecision_IT6322')
    pm = IT6322('ASRL11::INSTR')
    # pm.setCurrentLimits([1, 1, 1])
    # pm.setOutputStatuses([False]*3)
    pm.beeper()
    import time

    time.sleep(1000)
