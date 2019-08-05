# -*- coding: utf-8 -*-
"""
Created on Dec 05 2016

@author: LiuYang lfliuy@gmail.com
# Following Li Yu Huai's program.
"""
import pyvisa as visa
import numpy as np
import enum
import time
import csv
import os

class Instrument:
    visa_resources = {'AWG70002A': 'GPIB8::1::INSTR'}

    def __init__(self, instr_name=""):
        self.instr_name = instr_name
        self.rm = visa.ResourceManager()
        self._inited = False
        if not instr_name in self.visa_resources:
            print("Open resource: No Resource Named:'", instr_name, "' Found")
            return
        self.instr_handle = self.rm.open_resource(self.visa_resources[instr_name])
        print("Open resource: Resource opened: ", self.instr_handle.query("*IDN?"))
        self._inited = True

    def __enter__(self):
        # we can initialize here, in #with
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()
        #if exc_tb is None:
        #    print '[Exit %s]: Exited without exception.' % self.tag
        #else:
        #    print '[Exit %s]: Exited with exception raised.' % self.tag
        #    return False   # 可以省略，缺省的None也是被看做是False

    def list_resource(self):
        resources = self.rm.list_resources()
        print("PyVISA have found the following resources:")
        for res in resources:
            print(res)

    def _is_valid(self):
        return self._inited

    def _query(self, msg):
        ans = ""
        if self._is_valid():
            try:
                ans = self.instr_handle.query(msg)
            except:
                ans = ""
        return ans

    def _query_ascii(self, msg):
        ''' Return List values queried'''
        if self._is_valid():
            return self.instr_handle.query_ascii_values(msg)
            # if we want numpy return, and hex format, with separater '$'
            #return self.instr_handle.query_ascii_values(msg, container=numpy.array, converter='x', separator='$')

    def _query_binary(self, msg, datatype='B'):
        ''' Return List values queried in binary'''
        if self._is_valid():
            return self.instr_handle.query_binary_values(msg, datatype)
            # if we have double 'd' in big endian:
            #return self.instr_handle.query_binary_values(msg, datatype='d', is_big_endian=True)

    def _write_ascii(self, msg, values):
        ''' write values (a list)'''
        if self._is_valid():
            self.instr_handle.write_ascii_values(msg, values)
            # if we want to convert to hex, and separate with '$'
            # default converter = 'f', separator = ','
            #self.instr_handle.write_ascii_values(msg, values, converter='x', separator='$')

    def _write_binary(self, msg, values, datatype='f', is_big_endian=False):
        ''' write values (a list)'''
        if self._is_valid():
            self.instr_handle.write_binary_values(msg, values, datatype, is_big_endian)
            # if we have double 'd' in big endian:
            # default converter = 'f', separator = ','
            #self.instr_handle.write_ascii_values(msg, values, datatype='d', is_big_endian=True)

    def _write_raw(self, msg):
        if self._is_valid():
            self.instr_handle.write(msg)

    def _read_raw(self):
        if self._is_valid():
            return self.instr_handle.read_raw()

    def close(self):
        if self._is_valid():
            print('Close Resource: ', self.instr_name)
            self.instr_handle.close()
        else:
            print('Close Resource: ', self.instr_name, '[Does not exist]')


class SCPI:
    def __init__(self, instrument):
        self.instrument = instrument
        self.query = self.instrument._query             #instr_handle.query
        self.write = self.instrument._write_raw         #instr_handle.write
        self.writeValue = self.instrument._write_binary #instr_handle.write_binary_values
        self.queryValue = self.instrument._query_binary #instr_handle.query_binary_values

    def __getattr__(self, item):
        return SCPI.Command(self, item)

    class Command:
        def __init__(self, scpi, cmd, parent=None):
            self.scpi = scpi
            self.parent = parent
            self.cmd = cmd
            #print(cmd, parent)
            if self.cmd[0] == '_':
                self.cmd = '*' + self.cmd[1:]
            if parent:
                self.fullCmd = parent.fullCmd + ":" + self.cmd
            else:
                self.fullCmd = self.cmd

        def query(self, *args):
            re = self.scpi.query(self.createCommand(True, [arg for arg in args]))
            if re is not None:
                if (len(re) > 0) and (re[-1] == '\n'):
                    re = re[:-1]
            return re

        def queryInt(self, *args):
            return int(self.query(*args))

        def queryString(self, *args):
            return self.query(*args)[1:-1]

        def write(self, *args):
            self.scpi.write(self.createCommand(False, [arg for arg in args]))

        def createCommand(self, isQuery, args=[]):
            cmd = self.fullCmd
            if isQuery:
                cmd += '?'
            if len(args) > 0:
                cmd += ' {}'.format(args[0])
                for i in range(1, len(args)):
                    cmd += ',{}'.format(args[i])
            return cmd

        def __getattr__(self, item):
            return SCPI.Command(self.scpi, item, self)

        def __str__(self):
            return '[SCPI]' + self.fullCmd


class AWG70002(Instrument):
    def __init__(self, instr_name="AWG70002A"):
        super(AWG70002, self).__init__(instr_name)
        self.scpi = SCPI(self)

    def writeWaveform(self, name, data):
        if self._isWaveformExists(name):
            self._deleteWaveform(name)
        self._createWaveform(name, len(data))
        self._writeWaveformData(name, data)

    def _markerForm(self, marker1, marker2):
        return ((marker1&0x1) << 6) | ((marker2&0x1) << 7)

    def writeMarker(self, name, marker):
        # Marker is a N*1 or N*2 np.array of 0/1, describing the marker data.
        # e.g., marker[:,0]=np.array([0,0,0,1,1]), marker[:,1]=np.array([0,1,1,1,0]),
        # then we set both markers accordingly.
        # if marker has only one column, we set both marker the same.
        # by translating it to marker_raw = [0, 0x8, 0x8, 0xC, 0x4]
        if self._isWaveformExists(name):
            self._deleteWaveform(name)

        np_marker = np.array(marker)
        self._createWaveform(name, np_marker.shape[0])
        if len(np_marker.shape) == 2:
            raw_marker = self._markerForm(np_marker[:, 0], np_marker[:, 1])
        else:
            raw_marker = self._markerForm(np_marker, np_marker)

        self._writeMarkerData(name, raw_marker)

    def addMarker(self, name, marker):
        np_marker = np.array(marker)
        if not self._isWaveformExists(name):
            self._createWaveform(name, np_marker.shape[0])

        if len(np_marker.shape) == 2:
            raw_marker = self._markerForm(np_marker[:, 0], np_marker[:, 1])
        else:
            raw_marker = self._markerForm(np_marker, np_marker)

        self._writeMarkerData(name, raw_marker)


    def writeSequence(self, name, sequenceItems):
        if self._isSequenceExists(name):
            self._deleteSequence(name)
        self._createSequence(name, len(sequenceItems), 1)
        for i in range(len(sequenceItems)):
            self._setSequenceItem(name, i+1, sequenceItems[i])
            if i%10==0:
                print(i)
            #print("Send %d SeqItem to AWG"%i)

    def assignOutputSeq(self, channel, sequence):
        self._assignSequence(channel, sequence, 1)

    def assignOutput(self, channel, waveform):
        self._assignWaveform(channel, waveform)

    def assignOutputs(self, waveforms):
        for channel in range(len(waveforms)):
            self._assignWaveform(channel + 1, waveforms[channel])

    def setClock(self, rate=1E9):
        self._setClockSource("INT")
        self._setIntClockRate(rate)

    def start(self, channels=[1, 2]):
        if not isinstance(channels, list):
            channels = [channels]
        for channel in channels:
            print("Start Channel", channel)
            self._setOutput(channel, True)
        self._start()

    def stop(self):
        self._setOutput(1, False)
        self._setOutput(2, False)
        print("Stop Channels 1&2")
        self._stop()

    class SequenceItem():
        def __init__(self, waveformName, waitMode, repeat=1, goto='NEXT', jumpMode='OFF', jumpTarget='NEXT'):
            self.waveformName = waveformName
            self.repeat = repeat
            self.waitMode = waitMode
            self.goto = goto
            self.jumpMode = jumpMode
            self.jumpTarget = jumpTarget

        class TriggerMode(enum.Enum):
            OFF = 'OFF'
            TriggerA = 'ATR'
            TrigegrB = 'BTR'
            Integer = 'ITR'

        class Target(enum.Enum):
            NEXT = 'NEXT'
            FIRST = 'FIRST'
            LAST = 'LAST'
            END = 'END'

    # Normal operation/inquiry #
    def _waitCMD(self, waitTime=10):
        wt = 0
        while not bool(self._query_ascii('*OPC?')):
            time.sleep(0.1)
            wt += 0.1
            if wt >= waitTime:
                break
        if wt >= waitTime:
            return False
        else:
            return True

    def _identity(self):
        return self.scpi._IDN.query()

    def _isOperationCompleted(self):
        return bool(self._query("*OPC?")[0])

    def _getVersion(self):
        return self.scpi.SYSTem.VERSion.query()

    # Waveform operations #
    def _createWaveform(self, name, length):
        assert isinstance(name, str)
        assert isinstance(length, int)
        self.scpi.WLISt.WAVeform.NEW.write('"{}"'.format(name), length)

    def _getWaveformListSize(self):
        return self.scpi.WLISt.SIZE.queryInt()

    def _getWaveformLength(self, name):
        return self.scpi.WLISt.WAVeform.LENGth.queryInt('"{}"'.format(name))

    def _getWaveformName(self, index):
        assert isinstance(index, int)
        return self.scpi.WLISt.NAME.queryString(index)

    def _listWaveforms(self):
        size = self._getWaveformListSize()
        return [self._getWaveformName(i + 1) for i in range(size)]

    def _isWaveformExists(self, name):
        assert isinstance(name, str)
        return self._listWaveforms().__contains__(name)

    def _deleteWaveform(self, name):
        assert isinstance(name, str)
        self.scpi.WLISt.WAVeform.DELete.write('"{}"'.format(name))
        #self.instr_handle.write('WLISt:WAVeform:DELete "{}"'.format(name))

    def _deleteAllWaveforms(self):
        self.scpi.WLISt.WAVeform.DELete.write('ALL')

    def _getWaveformType(self, name):
        return self.scpi.WLISt.WAVeform.TYPE.query('"{}"'.format(name))

    def _writeWaveformData(self, name, data, start=0):
        self._write_binary('WLISt:WAVeform:DATA "{}",{},{},'.format(name, start, len(data)), data,
                                              datatype='f', is_big_endian=False)

    def _writeMarkerData(self, name, data, start=0):
        print(data)
        self._write_binary('WLISt:WAVeform:MARKer:DATA "{}",{},{},'.format(name, start, len(data)), data,
                                              datatype='B', is_big_endian=False)

    def _setOutput(self, channel, status):
        #self.scpi.OUTPUT.write('{} {}'.format(channel, 1 if status else 0))
        self._write_raw('OUTPUT{}:{}'.format(channel, 1 if status else 0))

    def _start(self):
        self.scpi.AWGControl.RUN.IMMediate.write()
        #self.instr_handle.write('AWGControl:RUN')

    def _stop(self):
        self.scpi.AWGControl.STOP.IMMediate.write()
        #self.instr_handle.write('AWGControl:STOP')

    # Sequence Operations #
    def _getSequenceName(self, index):
        return self._query('SLISt:NAME? {}'.format(index))[1:-2]

    def _getSequenceLength(self, name):
        return int(self._query('SLISt:SEQuence:LENGth? "'+name+'"')[0:-1])

    def _deleteSequence(self, name):
        self._write_raw('SLISt:SEQuence:DELete "{}"'.format(name))

    def _deleteAllSequence(self):
        self._write_raw('SLISt:SEQuence:DELete ALL')

    def _createSequence(self, name, step, track):
        self._write_raw('SLISt:SEQuence:NEW "{}",{},{}'.format(name, step, track))

    def _getSequenceListSize(self):
        return int(self._query('SLISt:SIZE?'))

    def _listSequences(self):
        size = self._getSequenceListSize()
        return [self._getSequenceName(i+1) for i in range(size)]

    def _isSequenceExists(self, name):
        return self._listSequences().__contains__(name)

    def _getMaxSequenceSteps(self):
        return int(self._query('SLIST:SEQUENCE:STEP:MAX?')[0:-1])

    def _setSequenceItemWaitMode(self, name, step, mode):
        # mode can be OFF|ATRIGGER|BTRIGGER|ITRIGGER
        self._write_raw('SLISt:SEQuence:STEP{}:WINPut "{}", {}'.format(step, name, mode))

    def _setSequenceItemJumpMode(self, name, step, mode):
        # mode can be OFF|ATRIGGER|BTRIGGER|ITRIGGER
        self._write_raw('SLISt:SEQuence:STEP{}:EJINput "{}", {}'.format(step, name, mode))

    def _setSequenceItemJumpTarget(self, name, step, target):
        # target can be NEXT|FIRST|LAST|END or a index
        self._write_raw('SLISt:SEQuence:STEP{}:EJUMp "{}", {}'.format(step, name, target))

    def _setSequenceItemGoto(self, name, step, target):
        # target can be NEXT|FIRST|LAST|END or a index
        self._write_raw('SLISt:SEQuence:STEP{}:GOTO "{}", {}'.format(step, name, target))

    def _setSequenceItemRepeat(self, name, step, count):
        # count can be INFINITE or a number
        self._write_raw('SLISt:SEQuence:STEP{}:RCOunt "{}", {}'.format(step, name, count))

    def _setSequenceItemWaveform(self, name, step, track, waveformName):
        self._write_raw(
            'SLISt:SEQuence:STEP{}:TASSet{}:WAVeform "{}", "{}"'.format(step, track, name, waveformName))

    def _setSequenceItem(self, name, step, sequenceItem):
        self._setSequenceItemWaveform(name, step, 1, sequenceItem.waveformName)

        def modeParse(mode, clazz):
            return mode.value if isinstance(mode, clazz) else mode

        self._setSequenceItemRepeat(name, step, sequenceItem.repeat)
        self._setSequenceItemWaitMode(name, step, modeParse(sequenceItem.waitMode, AWG70002.SequenceItem.TriggerMode))
        self._setSequenceItemGoto(name, step, modeParse(sequenceItem.goto, AWG70002.SequenceItem.Target))
        self._setSequenceItemJumpMode(name, step, modeParse(sequenceItem.jumpMode, AWG70002.SequenceItem.TriggerMode))
        self._setSequenceItemJumpTarget(name, step, modeParse(sequenceItem.jumpTarget, AWG70002.SequenceItem.Target))

    # Source: Assign pattern to source #
    def _assignWaveform(self, channel, waveformName):
        self._write_raw('SOURCE{}:WAVeform "{}"'.format(channel, waveformName))

    def _assignSequence(self, channel, sequenceName, track=1):
        self._write_raw('SOURCE{}:CASSet:SEQuence "{}",{}'.format(channel, sequenceName, track))

    def _setClockSource(self, source):
        # SOURCE can be "INT|EFIX|EVAR|EXT"
        self._write_raw("CLOCk:SOURce "+source)

    def _getIntClockRate(self):
        # Set internal clock rate to rate (int)
        return self._query_ascii('CLOCk:SRATe?')[0]

    def _setIntClockRate(self, rate=1E9):
        # Set internal clock rate to rate (int)
        self._write_raw('CLOCk:SRATe %E' % rate)

    def _isSeqEnd(self):
        stat = self._query("SOURce:SCSTep?")[:-1]
        return stat == 'END'

    def _isAWGready(self):
        stat = self._query("AWGControl:RSTate?")[:-1]
        return stat == '1'

    def _isAWGend(self):
       stat = self._query("AWGControl:RSTate?")[:-1]
       return stat == '2'

class AWG70002PM(AWG70002):
    ''' AWG70002A in Pulsed Mode'''
    def __init__(self):
        super(AWG70002PM, self).__init__("AWG70002A")
        self.clockRate = self._getIntClockRate()
        #self.maxSeqSteps = self._getMaxSequenceSteps()
        self._wfList = self._listWaveforms()
        self._idxseq = 1

    def _mkWaveformName(self, wfTime):
        return "wfPattern%03d" % (wfTime)

    def _mkMarkerName(self, mkTime, mkflag):
        return "mk%dPattern%03d" % (mkflag, mkTime)

    def writeWaveformPattern(self, wfHighBeg, wfHighWidth=200, wfLength=2400):#name, data):
        ''' Generate the waveform with one pulse,
        starts at wfHighBeg, with wfHighWidth width,
        The total length is wfLength (points)
        '''
        name = self._mkWaveformName(wfHighBeg)
        data = [0]*wfLength
        wfHighBeg = wfHighBeg if wfHighBeg > 0 else 0
        wfHighEnd = wfHighBeg + wfHighWidth
        wfHighEnd = wfHighEnd if wfHighEnd < wfLength else wfLength
        data[wfHighBeg:wfHighEnd] = [1]*(wfHighEnd-wfHighBeg)

        self.writeWaveform(name, data)
        return name

    def writeMarkerPattern(self, mkHighBeg, mkFlag, mkHighWidth=200, mkLength=2400):
        name = self._mkMarkerName(mkHighBeg, mkFlag)
        marker = np.zeros([mkLength,2], dtype='uint8')
        mkHighBeg = mkHighBeg if mkHighBeg>0 else 0
        mkHighEnd = mkHighBeg + mkHighWidth
        mkHighEnd = mkHighEnd if mkHighEnd<mkLength else mkLength
        marker[int(mkHighBeg): int(mkHighEnd+1), int(mkFlag-1)] = 1
        self.writeMarker(name, marker)
        return name

    def setClock(self, rate=20E9):
        super(AWG70002PM, self).setClock(rate)
        self._waitCMD()
        self.clockRate = self._getIntClockRate()
        print("Set Clock Rate to %E" % self.clockRate)

    def isSeqEnd(self):
        return self._isSeqEnd()

    def isAWGready(self):
        return self._isAWGready()

    def isAWGend(self):
        return self._isAWGend()

    def getSysParams(self):
        params = {}
        params['sysClockRate'] = self.clockRate
        params['maxSequenceLength'] = self.maxSeqSteps
        return params

    def clearAll(self):
        self._deleteAllWaveforms()
        self._deleteAllSequence()
        self._wfList=[]

    def AddWaveform(self, wfHighBeg, wfHighWidth=200, wfLength=2400):
        name = self._mkWaveformName(wfHighBeg)
        if name in self._wfList:
            return name # do not need to add
        self.writeWaveformPattern(wfHighBeg, wfHighWidth, wfLength)
        self._wfList = self._listWaveforms() # refresh list
        return name

    def AddMarker(self, mkHighBeg, mkFlag, mkHighWidth=200, mkLength=2400):
        name = self._mkMarkerName(mkHighBeg, mkFlag)
        if name in self._wfList:
            return name # do not need to add
        self.writeMarkerPattern(mkHighBeg, mkFlag, mkHighWidth, mkLength)
        self._wfList = self._listWaveforms() # refresh list
        return name

    def writePulseSequences(self, positions, waitMode=AWG70002.SequenceItem.TriggerMode.TriggerA):
        # Based on the WaveForm we generated, Here we generate the sequence
        # The input Sequences need to be in "ns??" units.
        if len(positions) > self.maxSeqSteps:
            raise Exception("Input Sequence too Large: ", positions,
                            ", Limit=", self.maxSeqSteps)
        seqlist = []
        self._wfList = self._listWaveforms()
        for wfBegPos in positions:
            wfname = self.AddWaveform(wfBegPos)
            seqlist.append(AWG70002.SequenceItem(wfname, waitMode))
        print(time.asctime()+" Seq pack OK.")
        seqname="AutoWavSeq%d" % self._idxseq
        self.writeSequence(seqname, seqlist)
        print(time.asctime()+" Seq write OK.")
        self._assignSequence(1, seqname)
        self._idxseq += 1

    def writeMarkerSequences(self, positions, mkflag, waitMode=AWG70002.SequenceItem.TriggerMode.TriggerA):
        if len(positions)>self.maxSeqSteps:
            raise Exception("Input Sequence too Large: ", positions,
                            ", Limit=", self.maxSeqSteps)
        seqlist = []
        self._wfList = self._listWaveforms()
        for mkBegPos, mkchan in zip(positions, mkflag):
            mkname = self.AddMarker(mkBegPos, mkchan)
            seqlist.append(AWG70002.SequenceItem(mkname, waitMode))
        print(time.asctime()+" Seq pack OK.")
        seqname="AutoMkSeq%d"%self._idxseq
        self.writeSequence(seqname, seqlist)
        print(time.asctime()+" Seq write OK.")
        self._assignSequence(1, seqname)
        self._idxseq += 1

    def GenerateSequenceCSV(self, positions, path='Seq.csv', waitMode=AWG70002.SequenceItem.TriggerMode.TriggerA):
        # You do not want to use this for large file, the loading is toooo slow.
        with open(path, 'w', newline='') as csvfile:
            scw = csv.writer(csvfile)
            seqname="AutoSeq%d" % self._idxseq
            scw.writerow(['AWG Sequence Definition'])
            scw.writerow(['Sequence Name', seqname])
            scw.writerow(['Sample Rate', 1E10])
            scw.writerow(['Waveform Name Base', 'wfPattern'])
            scw.writerow('')
            scw.writerow(['Track', '1'])
            # scw.writerow('Wait','Repeat','Event Input','Event Jump to','Go To', 'Flags',
            #              'Waveform Name','Frequency','Length','Marker1','Marker2','Editor','Parameters')
            scw.writerow(['Wait', 'Waveform Name', 'Frequency', 'Length'])
            for wfBegPos in positions:
                wfname = self.AddWaveform(wfBegPos)
                scw.writerow(['TrigA', wfname, 1E10, '2400'])


import math


class ModulatorConfig:
    def __init__(self, duty, delay, diff, waveformPeriodLength, waveformLength, ampMod, ampModSlope=lambda a, b: 0):
        self.duty = duty
        self.delay = delay
        self.diff = diff
        self.waveformPeriodLength = waveformPeriodLength
        self.waveformLength = waveformLength
        self.ampMod = ampMod
        self.ampModSlope = ampModSlope

    def generateWaveform(self, randomNumbers, firstPulseMode):
        waveformPositions = [i * self.waveformPeriodLength for i in range(0, len(randomNumbers))]
        waveformPositionsInt = [math.floor(i) for i in waveformPositions + [self.waveformLength]]
        waveformLengthes = [waveformPositionsInt[i + 1] - waveformPositionsInt[i] for i in range(0, len(randomNumbers))]
        waveform = []
        waveformUnits = self.generateWaveformUnits()
        for i in range(0, len(randomNumbers)):
            rn = randomNumbers[i]
            length = waveformLengthes[i]
            if firstPulseMode and i > 0:
                waveform += [0] * length
            else:
                waveform += waveformUnits[rn][:length]
            if len(waveform) >= self.waveformLength: break
        delaySample = -int(self.delay * 25)
        waveform = waveform[delaySample:] + waveform[:delaySample]
        return waveform[:self.waveformLength]

    def generateWaveformUnits(self, ):
        waveforms = []
        for i in range(0, 8):
            waveform = self._generateWaveformUnit(i)
            waveforms.append(waveform)
        return waveforms

    def _generateWaveformUnit(self, randomNumber):
        waveform = []
        for i in range(0, math.ceil(self.waveformPeriodLength)):
            position = i * 1.0 / self.waveformPeriodLength
            if (position <= self.duty):
                pulseIndex = 0
                positionInPulse = position / self.duty
            elif (position >= self.diff and position <= (self.diff + self.duty)):
                pulseIndex = 1
                positionInPulse = (position - self.diff) / self.duty
            else:
                pulseIndex = -1
                positionInPulse = -1
            amp = self.ampMod(pulseIndex, randomNumber)
            slope = self.ampModSlope(pulseIndex, randomNumber)
            if slope >= 0:
                amp *= (1 - slope * (1 - positionInPulse))
            else:
                amp *= (1 + slope * positionInPulse)
            waveform.append(amp)
        return waveform


class AWGEncoder:
    def __init__(self):
        self.sampleRate = 25e9
        self.waveformLength = 10 * 10 * 25
        self.randomNumbers = [0] * 10
        self.firstPulseMode = False
        self.specifiedRandomNumber = -1
        self.ampDecoyZ = 1
        self.ampDecoyX = 0.8
        self.ampDecoyY = 0.4
        self.ampDecoyZSlope = 0
        self.ampDecoyXSlope = 0
        self.ampDecoyYSlope = 0
        self.ampDecoyO = 0
        self.ampPhase = 0.7
        self.pulseWidthDecoy = 2
        self.pulseWidthLaser = 3
        self.pulseWidthTime0 = 2
        self.pulseWidthTime1 = 2
        self.pulseWidthPhase = 2
        self.pulseWidthSync = 2
        self.interferometerDiff = 3
        self.delayDecoy = 0
        self.delayTime1 = 0
        self.delayTime2 = 0
        self.delayPhase = 0
        self.delayLaser = 0
        self.delaySync = 0
        self.syncPeriod = 1000
        self.betterAMTimeA = False
        self.betterAMTimeAmpPulse0 = 1
        self.betterAMTimeAmpPulse1 = 1
        self.advancedPM = True

    def _ampModDecoy(self, pulseIndex, randomNumber):
        if pulseIndex == 0:
            return [self.ampDecoyO, self.ampDecoyX, self.ampDecoyY, self.ampDecoyZ][int(randomNumber / 2)]
        else:
            return 0

    def _ampModDecoySlope(self, pulseIndex, randomNumber):
        if pulseIndex == 0:
            return [0, self.ampDecoyXSlope, self.ampDecoyYSlope, self.ampDecoyZSlope][int(randomNumber / 2)]
        else:
            return 0

    # def _ampModTime(self, pulseIndex, randomNumber):    #decoy=0->vacuum->high level->pass
    #     if pulseIndex == -1: return 0
    #     decoy = int(randomNumber / 2)
    #     if decoy == 0:
    #         return 0
    #     elif decoy == 1 or decoy == 2:
    #         return 1
    #     else:
    #         return (pulseIndex == randomNumber % 2) * (
    #             [self.betterAMTimeAmpPulse0, self.betterAMTimeAmpPulse1][pulseIndex])

    def _ampModTime(self, pulseIndex, randomNumber):
        if pulseIndex == -1: return 0
        decoy = int(randomNumber / 2)
        if decoy == 0:
            return 1
        elif decoy == 1 or decoy == 2:
            return 0
        else:
            return (pulseIndex != randomNumber % 2) * (
                [self.betterAMTimeAmpPulse0, self.betterAMTimeAmpPulse1][pulseIndex])

    def _ampModPhase(self, pulseIndex, randomNumber):
        if pulseIndex == -1:
            return 0
        else:
            if self.advancedPM:
                return (pulseIndex == randomNumber % 2) * self.ampPhase
            else:
                return ((pulseIndex == 0) and (randomNumber % 2 == 1)) * self.ampPhase

    def _ampModLaser(self, pulseIndex, randomNumber):
        if pulseIndex != 0: return 0
        if self.specifiedRandomNumber == -1 or (self.specifiedRandomNumber == randomNumber):
            return 1
        else:
            return 0

    def _ampModSinglePulse(self, pulseIndex, randomNumber):
        return pulseIndex == 0

    # Defination of Random Number:
    # parameter ``randomNumbers'' should be a list of RN
    # RN is an integer.
    # RN/2 can be one of {0, 1, 2, 3}, stands for O, X, Y ,Z
    # RN%2 represent for encoding (0, 1)
    def generateWaveforms(self):
        waveformPeriodLength = self.waveformLength / len(self.randomNumbers)
        waveformSyncPeriodLength = self.syncPeriod * self.sampleRate * 1e-9
        waveformPeriod = waveformPeriodLength * 1e9 / self.sampleRate
        waveformSyncPeriod = waveformSyncPeriodLength * 1e9 / self.sampleRate

        syncCount = self.waveformLength / waveformSyncPeriodLength
        if math.fabs(int(syncCount + 0.5) - syncCount) > 0.001:
            raise RuntimeError('Sync period does not match the length of waveform.')
        if waveformSyncPeriodLength < waveformPeriodLength:
            raise RuntimeError('Sync can not be faster then other signals.')

        modulatorConfigs = {
            'AMDecoy': ModulatorConfig(self.pulseWidthDecoy / waveformPeriod, self.delayDecoy,
                                       self.interferometerDiff / waveformPeriod, waveformPeriodLength,
                                       self.waveformLength, self._ampModDecoy, self._ampModDecoySlope),
            'AMTime1': ModulatorConfig(self.pulseWidthTime0 / waveformPeriod, self.delayTime1,
                                       self.interferometerDiff / waveformPeriod, waveformPeriodLength,
                                       self.waveformLength, self._ampModTime),
            'AMTime2': ModulatorConfig(self.pulseWidthTime1 / waveformPeriod, self.delayTime2,
                                       self.interferometerDiff / waveformPeriod, waveformPeriodLength,
                                       self.waveformLength, self._ampModTime),
            'PM': ModulatorConfig(self.pulseWidthPhase / waveformPeriod, self.delayPhase,
                                  self.interferometerDiff / waveformPeriod, waveformPeriodLength, self.waveformLength,
                                  self._ampModPhase),
            'AMLaser': ModulatorConfig(self.pulseWidthLaser / waveformPeriod, self.delayLaser,
                                       self.interferometerDiff / waveformPeriod, waveformPeriodLength,
                                       self.waveformLength, self._ampModLaser),
            'AMSync': ModulatorConfig(self.pulseWidthSync / waveformSyncPeriod, self.delaySync,
                                      self.interferometerDiff / waveformSyncPeriod, waveformSyncPeriodLength,
                                      self.waveformLength, self._ampModSinglePulse)
        }

        waveforms = {}
        for waveformName in modulatorConfigs.keys():
            config = modulatorConfigs.get(waveformName)
            waveform = config.generateWaveform(self.randomNumbers,
                                               'AMLaser'.__eq__(waveformName) and self.firstPulseMode)
            waveforms[waveformName] = waveform

        return waveforms


class AWGDev:
    def __init__(self):
        self.dev = AWG70002PM()
        self.dev._stop()
        self.encoder = AWGEncoder()
        self.encoder.randomNumbers = [0, 1, 2, 3, 4, 5, 6, 7]
        self.generatingWaveform = False

    def setRandomNumbers(self, rns):
        self.encoder.randomNumbers = rns

    def configure(self, key, value):
        if 'delayDecoy'.__eq__(key):
            self.encoder.delayDecoy = value
        elif 'delaySync'.__eq__(key):
            self.encoder.delaySync = value
        elif 'delayLaser'.__eq__(key):
            self.encoder.delayLaser = value
        elif 'delayPM'.__eq__(key):
            self.encoder.delayPhase = value
        elif 'delayTime0'.__eq__(key):
            self.encoder.delayTime1 = value
        elif 'delayTime1'.__eq__(key):
            self.encoder.delayTime2 = value

        elif 'pulseWidthDecoy'.__eq__(key):
            self.encoder.pulseWidthDecoy = value
        elif 'pulseWidthTime0'.__eq__(key):
            self.encoder.pulseWidthTime0 = value
        elif 'pulseWidthTime1'.__eq__(key):
            self.encoder.pulseWidthTime1 = value
        elif 'pulseWidthPhase'.__eq__(key):
            self.encoder.pulseWidthPhase = value
        elif 'pulseWidthLaser'.__eq__(key):
            self.encoder.pulseWidthLaser = value
        elif 'pulseWidthSync'.__eq__(key):
            self.encoder.pulseWidthSync = value
        elif 'syncPeriod'.__eq__(key):
            self.encoder.syncPeriod = value
        elif 'interferometerDiff'.__eq__(key):
            self.encoder.interferometerDiff = value

        elif 'ampDecoyZ'.__eq__(key):
            self.encoder.ampDecoyZ = value
        elif 'ampDecoyX'.__eq__(key):
            self.encoder.ampDecoyX = value
        elif 'ampDecoyY'.__eq__(key):
            self.encoder.ampDecoyY = value
        elif 'ampPhase'.__eq__(key):
            self.encoder.ampPhase = value
        elif 'ampDecoyZSlope'.__eq__(key):
            self.encoder.ampDecoyZSlope = value
        elif 'ampDecoyXSlope'.__eq__(key):
            self.encoder.ampDecoyXSlope = value
        elif 'ampDecoyYSlope'.__eq__(key):
            self.encoder.ampDecoyYSlope = value

        elif 'firstLaserPulseMode'.__eq__(key):
            self.encoder.firstPulseMode = value
        elif 'specifiedRandomNumber'.__eq__(key):
            self.encoder.specifiedRandomNumber = value
        elif 'waveformLength'.__eq__(key):
            self.encoder.waveformLength = value

        elif 'betterAMTimeA'.__eq__(key):
            self.encoder.betterAMTimeA = value
        elif 'betterAMTimeAmpPulse0'.__eq__(key):
            self.encoder.betterAMTimeAmpPulse0 = value
        elif 'betterAMTimeAmpPulse1'.__eq__(key):
            self.encoder.betterAMTimeAmpPulse1 = value
        elif 'advancedPM'.__eq__(key):
            self.encoder.advancedPM = value

        else:
            raise RuntimeError('Bad configuration')

    def generateNewWaveformLater(self):
        import threading
        def g():
            self.generateNewWaveform()
        threading.Thread(target=g).start()

    def generatingNewWaveform(self):
        return self.generatingWaveform

    def generateNewWaveform(self):
        if self.generatingWaveform:
            print('Error: generating.')
            return
        self.generatingWaveform = True
        t1 = time.time()

        waveforms = self.encoder.generateWaveforms()
        waveform1 = waveforms['AMDecoy']
        marker11 = waveforms['AMLaser']
        marker12 = waveforms['AMSync']
        waveform2 = waveforms['AMTime1']
        marker21 = waveforms['PM']
        marker22 = waveforms['AMTime2']

        if not self.encoder.betterAMTimeA:
            wft = waveform2
            waveform2 = marker21
            marker21 = wft

        waveform1 = [(w * 2 - 1) for w in waveform1]
        waveform2 = [(w * 2 - 1) for w in waveform2]
        marker11 = [0 if w == 0 else 1 for w in marker11]
        marker12 = [0 if w == 0 else 1 for w in marker12]
        marker21 = [0 if w == 0 else 1 for w in marker21]
        marker22 = [0 if w == 0 else 1 for w in marker22]

        marker1 = [[marker11[i], marker12[i]] for i in range(0, len(waveform1))]
        marker2 = [[marker21[i], marker22[i]] for i in range(0, len(waveform2))]

        t2 = time.time()

        self.dev.writeWaveform("Waveform1", waveform1)
        self.dev.addMarker('Waveform1', marker1)
        self.dev.writeWaveform("Waveform2", waveform2)
        self.dev.addMarker('Waveform2', marker2)
        self.dev.assignOutput(1, "Waveform1")
        self.dev.assignOutput(2, "Waveform2")
        self.dev._setOutput(1, True)
        self.dev._setOutput(2, True)

        t3 = time.time()

        print('Waveform generated in {} s, and applied in {} s'.format(t2-t1, t3-t2))
        self.generatingWaveform = False

    def startPlay(self):
        self.dev._start()

    def stopPlay(self):
        self.dev._stop()

    def stop(self):
        self.dev.close()



if __name__ == "__main__":
    # encoder = AWGEncoder()
    # encoder.randomNumbers = [0,1,2,3,4,5,6,7]
    # encoder.waveformLength = len(encoder.randomNumbers) * 250
    # encoder.firstPulseMode = True
    # startTime = time.time()
    # waveforms = encoder.generateWaveforms()
    # stopTime = time.time()
    # print('finished in {} s'.format(stopTime - startTime))
    #
    # import matplotlib.pyplot as plt
    # waveform = waveforms['AMTime1']
    # # waveform = waveforms['AMSync']
    # # waveform = waveforms['AMLaser']
    # plt.plot([i for i in range(0, len(waveform))], waveform)
    # plt.show()

    import Pydra
    dev = AWGDev()
    # session = Pydra.Session.newSession(('192.168.25.27',20102), dev, 'AWG-MDI-Alice')
    randomNumbersAlice = [0 for i in range(4000)]
    waveformLength = len(randomNumbersAlice) * 250
    dev.configure("waveformLength", waveformLength)
    dev.configure("syncPeriod", 250)
    dev.configure("firstLaserPulseMode", False)
    dev.setRandomNumbers(randomNumbersAlice)
    dev.generateNewWaveform()
