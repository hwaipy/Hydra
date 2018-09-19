# -*- coding: utf-8 -*-
"""
Created on Dec 05 2016

@author: Hwaipy Li, hwaipy@gmail.com
@author: Yang Liu, lfliuy@gmail.com
"""

# import pyvisa as visa
# import numpy as np
# import enum
# import time
# import csv
# import os
from Instruments import DeviceException, VISAInstrument


class TekAWG70k(VISAInstrument):
    manufacturer = 'Keysight Technologies'
    model = '34470A'

    def __init__(self, resourceID):
        super().__init__(resourceID)

    def writeWaveform(self, name, data):
        print(self._isWaveformExists(name))
        # if self._isWaveformExists(name):
        #     self._deleteWaveform(name)
        # self._createWaveform(name, len(data))
        # self._writeWaveformData(name, data)

    #     def _markerForm(self, marker1, marker2):
    #         return ((marker1&0x1) << 6) | ((marker2&0x1) << 7)
    #
    #     def writeMarker(self, name, marker):
    #         # Marker is a N*1 or N*2 np.array of 0/1, describing the marker data.
    #         # e.g., marker[:,0]=np.array([0,0,0,1,1]), marker[:,1]=np.array([0,1,1,1,0]),
    #         # then we set both markers accordingly.
    #         # if marker has only one column, we set both marker the same.
    #         # by translating it to marker_raw = [0, 0x8, 0x8, 0xC, 0x4]
    #         if self._isWaveformExists(name):
    #             self._deleteWaveform(name)
    #
    #         np_marker = np.array(marker)
    #         self._createWaveform(name, np_marker.shape[0])
    #         if len(np_marker.shape) == 2:
    #             raw_marker = self._markerForm(np_marker[:, 0], np_marker[:, 1])
    #         else:
    #             raw_marker = self._markerForm(np_marker, np_marker)
    #
    #         self._writeMarkerData(name, raw_marker)
    #
    #     def addMarker(self, name, marker):
    #         np_marker = np.array(marker)
    #         if not self._isWaveformExists(name):
    #             self._createWaveform(name, np_marker.shape[0])
    #
    #         if len(np_marker.shape) == 2:
    #             raw_marker = self._markerForm(np_marker[:, 0], np_marker[:, 1])
    #         else:
    #             raw_marker = self._markerForm(np_marker, np_marker)
    #
    #         self._writeMarkerData(name, raw_marker)
    #
    #
    #     def writeSequence(self, name, sequenceItems):
    #         if self._isSequenceExists(name):
    #             self._deleteSequence(name)
    #         self._createSequence(name, len(sequenceItems), 1)
    #         for i in range(len(sequenceItems)):
    #             self._setSequenceItem(name, i+1, sequenceItems[i])
    #             if i%10==0:
    #                 print(i)
    #             #print("Send %d SeqItem to AWG"%i)
    #
    #     def assignOutputSeq(self, channel, sequence):
    #         self._assignSequence(channel, sequence, 1)
    #
    #     def assignOutput(self, channel, waveform):
    #         self._assignWaveform(channel, waveform)
    #
    #     def assignOutputs(self, waveforms):
    #         for channel in range(len(waveforms)):
    #             self._assignWaveform(channel + 1, waveforms[channel])
    #
    #     def setClock(self, rate=1E9):
    #         self._setClockSource("INT")
    #         self._setIntClockRate(rate)
    #
    #     def start(self, channels=[1, 2]):
    #         if not isinstance(channels, list):
    #             channels = [channels]
    #         for channel in channels:
    #             print("Start Channel", channel)
    #             self._setOutput(channel, True)
    #         self._start()
    #
    #     def stop(self):
    #         self._setOutput(1, False)
    #         self._setOutput(2, False)
    #         print("Stop Channels 1&2")
    #         self._stop()
    #
    #     class SequenceItem():
    #         def __init__(self, waveformName, waitMode, repeat=1, goto='NEXT', jumpMode='OFF', jumpTarget='NEXT'):
    #             self.waveformName = waveformName
    #             self.repeat = repeat
    #             self.waitMode = waitMode
    #             self.goto = goto
    #             self.jumpMode = jumpMode
    #             self.jumpTarget = jumpTarget
    #
    #         class TriggerMode(enum.Enum):
    #             OFF = 'OFF'
    #             TriggerA = 'ATR'
    #             TrigegrB = 'BTR'
    #             Integer = 'ITR'
    #
    #         class Target(enum.Enum):
    #             NEXT = 'NEXT'
    #             FIRST = 'FIRST'
    #             LAST = 'LAST'
    #             END = 'END'
    #
    #     # Normal operation/inquiry #
    #     def _waitCMD(self, waitTime=10):
    #         wt = 0
    #         while not bool(self._query_ascii('*OPC?')):
    #             time.sleep(0.1)
    #             wt += 0.1
    #             if wt >= waitTime:
    #                 break
    #         if wt >= waitTime:
    #             return False
    #         else:
    #             return True
    #
    #     def _identity(self):
    #         return self.scpi._IDN.query()
    #
    #     def _isOperationCompleted(self):
    #         return bool(self._query("*OPC?")[0])
    #
    #     def _getVersion(self):
    #         return self.scpi.SYSTem.VERSion.query()
    #
    #     # Waveform operations #
    #     def _createWaveform(self, name, length):
    #         assert isinstance(name, str)
    #         assert isinstance(length, int)
    #         self.scpi.WLISt.WAVeform.NEW.write('"{}"'.format(name), length)
    #
    def _getWaveformListSize(self):
        print(self.scpi.WLISt.SIZE.createCommand(True))
        return self.scpi.WLISt.SIZE.query()

    #     def _getWaveformLength(self, name):
    #         return self.scpi.WLISt.WAVeform.LENGth.queryInt('"{}"'.format(name))

    def _getWaveformName(self, index):
        assert isinstance(index, int)
        return self.scpi.WLISt.NAME.queryString(index)

    def _listWaveforms(self):
        print('lsiting waveforms')
        size = self._getWaveformListSize()
        print(size)
        return [self._getWaveformName(i + 1) for i in range(size)]

    def _isWaveformExists(self, name):
        assert isinstance(name, str)
        return self._listWaveforms().__contains__(name)


#     def _deleteWaveform(self, name):
#         assert isinstance(name, str)
#         self.scpi.WLISt.WAVeform.DELete.write('"{}"'.format(name))
#         #self.instr_handle.write('WLISt:WAVeform:DELete "{}"'.format(name))
#
#     def _deleteAllWaveforms(self):
#         self.scpi.WLISt.WAVeform.DELete.write('ALL')
#
#     def _getWaveformType(self, name):
#         return self.scpi.WLISt.WAVeform.TYPE.query('"{}"'.format(name))
#
#     def _writeWaveformData(self, name, data, start=0):
#         self._write_binary('WLISt:WAVeform:DATA "{}",{},{},'.format(name, start, len(data)), data,
#                                               datatype='f', is_big_endian=False)
#
#     def _writeMarkerData(self, name, data, start=0):
#         print(data)
#         self._write_binary('WLISt:WAVeform:MARKer:DATA "{}",{},{},'.format(name, start, len(data)), data,
#                                               datatype='B', is_big_endian=False)
#
#     def _setOutput(self, channel, status):
#         #self.scpi.OUTPUT.write('{} {}'.format(channel, 1 if status else 0))
#         self._write_raw('OUTPUT{}:{}'.format(channel, 1 if status else 0))
#
#     def _start(self):
#         self.scpi.AWGControl.RUN.IMMediate.write()
#         #self.instr_handle.write('AWGControl:RUN')
#
#     def _stop(self):
#         self.scpi.AWGControl.STOP.IMMediate.write()
#         #self.instr_handle.write('AWGControl:STOP')
#
#     # Sequence Operations #
#     def _getSequenceName(self, index):
#         return self._query('SLISt:NAME? {}'.format(index))[1:-2]
#
#     def _getSequenceLength(self, name):
#         return int(self._query('SLISt:SEQuence:LENGth? "'+name+'"')[0:-1])
#
#     def _deleteSequence(self, name):
#         self._write_raw('SLISt:SEQuence:DELete "{}"'.format(name))
#
#     def _deleteAllSequence(self):
#         self._write_raw('SLISt:SEQuence:DELete ALL')
#
#     def _createSequence(self, name, step, track):
#         self._write_raw('SLISt:SEQuence:NEW "{}",{},{}'.format(name, step, track))
#
#     def _getSequenceListSize(self):
#         return int(self._query('SLISt:SIZE?'))
#
#     def _listSequences(self):
#         size = self._getSequenceListSize()
#         return [self._getSequenceName(i+1) for i in range(size)]
#
#     def _isSequenceExists(self, name):
#         return self._listSequences().__contains__(name)
#
#     def _getMaxSequenceSteps(self):
#         return int(self._query('SLIST:SEQUENCE:STEP:MAX?')[0:-1])
#
#     def _setSequenceItemWaitMode(self, name, step, mode):
#         # mode can be OFF|ATRIGGER|BTRIGGER|ITRIGGER
#         self._write_raw('SLISt:SEQuence:STEP{}:WINPut "{}", {}'.format(step, name, mode))
#
#     def _setSequenceItemJumpMode(self, name, step, mode):
#         # mode can be OFF|ATRIGGER|BTRIGGER|ITRIGGER
#         self._write_raw('SLISt:SEQuence:STEP{}:EJINput "{}", {}'.format(step, name, mode))
#
#     def _setSequenceItemJumpTarget(self, name, step, target):
#         # target can be NEXT|FIRST|LAST|END or a index
#         self._write_raw('SLISt:SEQuence:STEP{}:EJUMp "{}", {}'.format(step, name, target))
#
#     def _setSequenceItemGoto(self, name, step, target):
#         # target can be NEXT|FIRST|LAST|END or a index
#         self._write_raw('SLISt:SEQuence:STEP{}:GOTO "{}", {}'.format(step, name, target))
#
#     def _setSequenceItemRepeat(self, name, step, count):
#         # count can be INFINITE or a number
#         self._write_raw('SLISt:SEQuence:STEP{}:RCOunt "{}", {}'.format(step, name, count))
#
#     def _setSequenceItemWaveform(self, name, step, track, waveformName):
#         self._write_raw(
#             'SLISt:SEQuence:STEP{}:TASSet{}:WAVeform "{}", "{}"'.format(step, track, name, waveformName))
#
#     def _setSequenceItem(self, name, step, sequenceItem):
#         self._setSequenceItemWaveform(name, step, 1, sequenceItem.waveformName)
#
#         def modeParse(mode, clazz):
#             return mode.value if isinstance(mode, clazz) else mode
#
#         self._setSequenceItemRepeat(name, step, sequenceItem.repeat)
#         self._setSequenceItemWaitMode(name, step, modeParse(sequenceItem.waitMode, AWG70002.SequenceItem.TriggerMode))
#         self._setSequenceItemGoto(name, step, modeParse(sequenceItem.goto, AWG70002.SequenceItem.Target))
#         self._setSequenceItemJumpMode(name, step, modeParse(sequenceItem.jumpMode, AWG70002.SequenceItem.TriggerMode))
#         self._setSequenceItemJumpTarget(name, step, modeParse(sequenceItem.jumpTarget, AWG70002.SequenceItem.Target))
#
#     # Source: Assign pattern to source #
#     def _assignWaveform(self, channel, waveformName):
#         self._write_raw('SOURCE{}:WAVeform "{}"'.format(channel, waveformName))
#
#     def _assignSequence(self, channel, sequenceName, track=1):
#         self._write_raw('SOURCE{}:CASSet:SEQuence "{}",{}'.format(channel, sequenceName, track))
#
#     def _setClockSource(self, source):
#         # SOURCE can be "INT|EFIX|EVAR|EXT"
#         self._write_raw("CLOCk:SOURce "+source)
#
#     def _getIntClockRate(self):
#         # Set internal clock rate to rate (int)
#         return self._query_ascii('CLOCk:SRATe?')[0]
#
#     def _setIntClockRate(self, rate=1E9):
#         # Set internal clock rate to rate (int)
#         self._write_raw('CLOCk:SRATe %E' % rate)
#
#     def _isSeqEnd(self):
#         stat = self._query("SOURce:SCSTep?")[:-1]
#         return stat == 'END'
#
#     def _isAWGready(self):
#         stat = self._query("AWGControl:RSTate?")[:-1]
#         return stat == '1'
#
#     def _isAWGend(self):
#        stat = self._query("AWGControl:RSTate?")[:-1]
#        return stat == '2'
#


# class Instrument:
#     visa_resources = {'AWG70002A': 'GPIB8::1::INSTR'}
#
#     def __init__(self, instr_name=""):
#         self.instr_name = instr_name
#         self.rm = visa.ResourceManager()
#         self._inited = False
#         if not instr_name in self.visa_resources:
#             print("Open resource: No Resource Named:'", instr_name, "' Found")
#             return
#         self.instr_handle = self.rm.open_resource(self.visa_resources[instr_name])
#         print("Open resource: Resource opened: ", self.instr_handle.query("*IDN?"))
#         self._inited = True
#
#     def __enter__(self):
#         # we can initialize here, in #with
#         return self
#
#     def __exit__(self, exc_type, exc_val, exc_tb):
#         self.close()
#         #if exc_tb is None:
#         #    print '[Exit %s]: Exited without exception.' % self.tag
#         #else:
#         #    print '[Exit %s]: Exited with exception raised.' % self.tag
#         #    return False   # 可以省略，缺省的None也是被看做是False
#
#     def list_resource(self):
#         resources = self.rm.list_resources()
#         print("PyVISA have found the following resources:")
#         for res in resources:
#             print(res)
#
#     def _is_valid(self):
#         return self._inited
#
#     def _query(self, msg):
#         ans = ""
#         if self._is_valid():
#             try:
#                 ans = self.instr_handle.query(msg)
#             except:
#                 ans = ""
#         return ans
#
#     def _query_ascii(self, msg):
#         ''' Return List values queried'''
#         if self._is_valid():
#             return self.instr_handle.query_ascii_values(msg)
#             # if we want numpy return, and hex format, with separater '$'
#             #return self.instr_handle.query_ascii_values(msg, container=numpy.array, converter='x', separator='$')
#
#     def _query_binary(self, msg, datatype='B'):
#         ''' Return List values queried in binary'''
#         if self._is_valid():
#             return self.instr_handle.query_binary_values(msg, datatype)
#             # if we have double 'd' in big endian:
#             #return self.instr_handle.query_binary_values(msg, datatype='d', is_big_endian=True)
#
#     def _write_ascii(self, msg, values):
#         ''' write values (a list)'''
#         if self._is_valid():
#             self.instr_handle.write_ascii_values(msg, values)
#             # if we want to convert to hex, and separate with '$'
#             # default converter = 'f', separator = ','
#             #self.instr_handle.write_ascii_values(msg, values, converter='x', separator='$')
#
#     def _write_binary(self, msg, values, datatype='f', is_big_endian=False):
#         ''' write values (a list)'''
#         if self._is_valid():
#             self.instr_handle.write_binary_values(msg, values, datatype, is_big_endian)
#             # if we have double 'd' in big endian:
#             # default converter = 'f', separator = ','
#             #self.instr_handle.write_ascii_values(msg, values, datatype='d', is_big_endian=True)
#
#     def _write_raw(self, msg):
#         if self._is_valid():
#             self.instr_handle.write(msg)
#
#     def _read_raw(self):
#         if self._is_valid():
#             return self.instr_handle.read_raw()
#
#     def close(self):
#         if self._is_valid():
#             print('Close Resource: ', self.instr_name)
#             self.instr_handle.close()
#         else:
#             print('Close Resource: ', self.instr_name, '[Does not exist]')
#
#
# class SCPI:
#     def __init__(self, instrument):
#         self.instrument = instrument
#         self.query = self.instrument._query#instr_handle.query
#         self.write = self.instrument._write_raw#instr_handle.write
#         self.writeValue = self.instrument._write_binary#instr_handle.write_binary_values
#         self.queryValue = self.instrument._query_binary#instr_handle.query_binary_values
#
#     def __getattr__(self, item):
#         return SCPI.Command(self, item)
#
#     class Command:
#         def __init__(self, scpi, cmd, parent=None):
#             self.scpi = scpi
#             self.parent = parent
#             self.cmd = cmd
#             #print(cmd, parent)
#             if self.cmd[0] == '_':
#                 self.cmd = '*' + self.cmd[1:]
#             if parent:
#                 self.fullCmd = parent.fullCmd + ":" + self.cmd
#             else:
#                 self.fullCmd = self.cmd
#
#         def query(self, *args):
#             re = self.scpi.query(self.createCommand(True, [arg for arg in args]))
#             if re is not None:
#                 if (len(re) > 0) and (re[-1] == '\n'):
#                     re = re[:-1]
#             return re
#
#         def queryInt(self, *args):
#             return int(self.query(*args))
#
#         def queryString(self, *args):
#             return self.query(*args)[1:-1]
#
#         def write(self, *args):
#             self.scpi.write(self.createCommand(False, [arg for arg in args]))
#
#         def createCommand(self, isQuery, args=[]):
#             cmd = self.fullCmd
#             if isQuery:
#                 cmd += '?'
#             if len(args) > 0:
#                 cmd += ' {}'.format(args[0])
#                 for i in range(1, len(args)):
#                     cmd += ',{}'.format(args[i])
#             return cmd
#
#         def __getattr__(self, item):
#             return SCPI.Command(self.scpi, item, self)
#
#         def __str__(self):
#             return '[SCPI]' + self.fullCmd
#
#

# class AWG70002PM(AWG70002):
#     ''' AWG70002A in Pulsed Mode'''
#     def __init__(self):
#         super(AWG70002PM, self).__init__("AWG70002A")
#         self.clockRate = self._getIntClockRate()
#         self.maxSeqSteps = self._getMaxSequenceSteps()
#         self._wfList = self._listWaveforms()
#         self._idxseq = 1
#
#     def _mkWaveformName(self, wfTime):
#         return "wfPattern%03d" % (wfTime)
#
#     def _mkMarkerName(self, mkTime, mkflag):
#         return "mk%dPattern%03d" % (mkflag, mkTime)
#
#     def writeWaveformPattern(self, wfHighBeg, wfHighWidth=200, wfLength=2400):#name, data):
#         ''' Generate the waveform with one pulse,
#         starts at wfHighBeg, with wfHighWidth width,
#         The total length is wfLength (points)
#         '''
#         name = self._mkWaveformName(wfHighBeg)
#         data = [0]*wfLength
#         wfHighBeg = wfHighBeg if wfHighBeg > 0 else 0
#         wfHighEnd = wfHighBeg + wfHighWidth
#         wfHighEnd = wfHighEnd if wfHighEnd < wfLength else wfLength
#         data[wfHighBeg:wfHighEnd] = [1]*(wfHighEnd-wfHighBeg)
#
#         self.writeWaveform(name, data)
#         return name
#
#     def writeMarkerPattern(self, mkHighBeg, mkFlag, mkHighWidth=200, mkLength=2400):
#         name = self._mkMarkerName(mkHighBeg, mkFlag)
#         marker = np.zeros([mkLength,2], dtype='uint8')
#         mkHighBeg = mkHighBeg if mkHighBeg>0 else 0
#         mkHighEnd = mkHighBeg + mkHighWidth
#         mkHighEnd = mkHighEnd if mkHighEnd<mkLength else mkLength
#         marker[int(mkHighBeg): int(mkHighEnd+1), int(mkFlag-1)] = 1
#         self.writeMarker(name, marker)
#         return name
#
#     def setClock(self, rate=20E9):
#         super(AWG70002PM, self).setClock(rate)
#         self._waitCMD()
#         self.clockRate = self._getIntClockRate()
#         print("Set Clock Rate to %E" % self.clockRate)
#
#     def isSeqEnd(self):
#         return self._isSeqEnd()
#
#     def isAWGready(self):
#         return self._isAWGready()
#
#     def isAWGend(self):
#         return self._isAWGend()
#
#     def getSysParams(self):
#         params = {}
#         params['sysClockRate'] = self.clockRate
#         params['maxSequenceLength'] = self.maxSeqSteps
#         return params
#
#     def clearAll(self):
#         self._deleteAllWaveforms()
#         self._deleteAllSequence()
#         self._wfList=[]
#
#     def AddWaveform(self, wfHighBeg, wfHighWidth=200, wfLength=2400):
#         name = self._mkWaveformName(wfHighBeg)
#         if name in self._wfList:
#             return name # do not need to add
#         self.writeWaveformPattern(wfHighBeg, wfHighWidth, wfLength)
#         self._wfList = self._listWaveforms() # refresh list
#         return name
#
#     def AddMarker(self, mkHighBeg, mkFlag, mkHighWidth=200, mkLength=2400):
#         name = self._mkMarkerName(mkHighBeg, mkFlag)
#         if name in self._wfList:
#             return name # do not need to add
#         self.writeMarkerPattern(mkHighBeg, mkFlag, mkHighWidth, mkLength)
#         self._wfList = self._listWaveforms() # refresh list
#         return name
#
#     def writePulseSequences(self, positions, waitMode=AWG70002.SequenceItem.TriggerMode.TriggerA):
#         # Based on the WaveForm we generated, Here we generate the sequence
#         # The input Sequences need to be in "ns??" units.
#         if len(positions) > self.maxSeqSteps:
#             raise Exception("Input Sequence too Large: ", positions,
#                             ", Limit=", self.maxSeqSteps)
#         seqlist = []
#         self._wfList = self._listWaveforms()
#         for wfBegPos in positions:
#             wfname = self.AddWaveform(wfBegPos)
#             seqlist.append(AWG70002.SequenceItem(wfname, waitMode))
#         print(time.asctime()+" Seq pack OK.")
#         seqname="AutoWavSeq%d" % self._idxseq
#         self.writeSequence(seqname, seqlist)
#         print(time.asctime()+" Seq write OK.")
#         self._assignSequence(1, seqname)
#         self._idxseq += 1
#
#     def writeMarkerSequences(self, positions, mkflag, waitMode=AWG70002.SequenceItem.TriggerMode.TriggerA):
#         if len(positions)>self.maxSeqSteps:
#             raise Exception("Input Sequence too Large: ", positions,
#                             ", Limit=", self.maxSeqSteps)
#         seqlist = []
#         self._wfList = self._listWaveforms()
#         for mkBegPos, mkchan in zip(positions, mkflag):
#             mkname = self.AddMarker(mkBegPos, mkchan)
#             seqlist.append(AWG70002.SequenceItem(mkname, waitMode))
#         print(time.asctime()+" Seq pack OK.")
#         seqname="AutoMkSeq%d"%self._idxseq
#         self.writeSequence(seqname, seqlist)
#         print(time.asctime()+" Seq write OK.")
#         self._assignSequence(1, seqname)
#         self._idxseq += 1
#
#     def GenerateSequenceCSV(self, positions, path='Seq.csv', waitMode=AWG70002.SequenceItem.TriggerMode.TriggerA):
#         # You do not want to use this for large file, the loading is toooo slow.
#         with open(path, 'w', newline='') as csvfile:
#             scw = csv.writer(csvfile)
#             seqname="AutoSeq%d" % self._idxseq
#             scw.writerow(['AWG Sequence Definition'])
#             scw.writerow(['Sequence Name', seqname])
#             scw.writerow(['Sample Rate', 1E10])
#             scw.writerow(['Waveform Name Base', 'wfPattern'])
#             scw.writerow('')
#             scw.writerow(['Track', '1'])
#             # scw.writerow('Wait','Repeat','Event Input','Event Jump to','Go To', 'Flags',
#             #              'Waveform Name','Frequency','Length','Marker1','Marker2','Editor','Parameters')
#             scw.writerow(['Wait', 'Waveform Name', 'Frequency', 'Length'])
#             for wfBegPos in positions:
#                 wfname = self.AddWaveform(wfBegPos)
#                 scw.writerow(['TrigA', wfname, 1E10, '2400'])
#
# def testFun(delay, count):
#     waveform = [-0] * delay
#     signal = 1
#     for i in range(0, count):
#         if i%2 is 0: signal = 1
#         if i%2 is 1: signal = -1
#         #if i%4 is 0: signal = 1
#         #if i%4 is 1: signal = 0
#         #if i%4 is 2: signal = -1
#         #if i%4 is 3: signal = 0
#         waveform += [signal]*50
#         signal *= -1
#
#
#     return waveform
#
#
# def testFun1(delay, count):
#     waveform = [-0] * delay
#     signal = 1
#     for i in range(0, count):
#         if i%2 is 0: signal = -1
#         if i%2 is 1: signal = 1
#         #if i%4 is 0: signal = 0
#         #if i%4 is 1: signal = 1
#         #if i%4 is 2: signal = 0
#         #if i%4 is 3: signal = -1
#         waveform += [signal]*50
#         signal *= -1
#
#
#     return waveform
# if __name__ == "__main__":
#     import sys
#     if(len(sys.argv) > 2):
#         delay1 = (int) (sys.argv[1])
#         delay2 = (int) (sys.argv[2])
#     else:
#         delay1 = 0
#         delay2 = 10000
#     with AWG70002PM() as dev:
#         dev._stop()
#         dev.clearAll()
#         dev.setClock(25E9)
#         waveformCH1 = testFun1(delay1, 16384)
#         waveform1 = ([1]*2500000+[0]*50)*100
#         dev.writeWaveform("kuaisu", waveform1)
#         dev.addMarker('kuaisu', ([1]*5000+[0]*45000))
#         #dev.writeWaveform("kuaisu", testFun(0,50))
#         #dev.writeWaveform("kuaisu", [1]*250000+[-1]*250000)
#         #dev.writeWaveform("mansu",[1]*1250+[-1]*1250)
#         # waveformCH2 = testFun(delay2, 16384)
#         delay = 9
#         # wf2 = wf[delay:] + wf[:delay]
#         dev.writeWaveform("mansu", ([1]*50+[0]*450)*100)
#         dev.addMarker('mansu', ([1]*50+[0]*450)*100)
#    # dev.addMarker('mansu', [255]*1000+[0]*(len(waveformCH2)-1000))
#         #dev.writeWaveform("mansu", testFun(0,2))
#         #dev.writeWaveform("mansu", testFun(0,16384))
#         #dev.writeWaveform("mansu", testFun(0,32768))
#
#         # dev.addMarker("ManualPattern", [[1,0],[0,1],[1,1],[0,0],[0,1],[1,0],[0,0],[1,1],[1,1],[0,0]])
#         # dev.writeMarker("ManualMarkerPattern1D",[1,1,0,1,0,1])
#         # dev.writeMarker("ManualMarkerPattern2D", [[1,0],[0,1],[1,1],[0,0]])
#         dev.assignOutput(1,"kuaisu")
#         dev.assignOutput(2,"mansu")
#         dev._setOutput(1, True)
#         dev._setOutput(2, True)
#         dev._start()
#         seqlist1 = []
#         sys.exit(0)
# # for i in range(20):
#         #     wfname = dev.writeWaveformPattern(i*20, 200)
#         #     seqlist1.append(
#         #             AWG70002.SequenceItem(wfname, waitMode=AWG70002.SequenceItem.TriggerMode.TriggerA))
#         # dev.writeSequence("AutoSeq1", seqlist1)
#         # dev.assignOutputSeq(2, "AutoSeq1")
#
#         # seqlist2 = []
#         # markerflag = [1,2,2,2,1,1,2,1,1,2,2,1,2,1,1]
#         # for j in range(15):
#         #     mkname = dev.writeMarkerPattern(j*20, markerflag[j], 200)
#         #     seqlist2.append(
#         #             AWG70002.SequenceItem(mkname, waitMode=AWG70002.SequenceItem.TriggerMode.TriggerA))
#         # dev.writeSequence("AutoSeq2", seqlist2)
#         # dev.assignOutputSeq(2, "AutoSeq2")
#
#         # use patch command, need to wait for trigger
#         params = dev.getSysParams()
#         wfposs = [100]*20#(params['maxSequenceLength']-1)
#         print("Write Pulse Sequence...")
#         dev.writePulseSequences(wfposs, waitMode=AWG70002.SequenceItem.TriggerMode.OFF)
#         print("Write Pulse Sequence OK")
#
#         # writeMarkerSequence
#         mkposs = [200]*30
#         seqmkflag = [1,2,2,1,1,2,1,2,2,1,1,1,2,2,1,1,2,1,2,2,1,1,2,1,2,2,2,2,1,1]
#         print("Write Marker Sequence...")
#         dev.writeMarkerSequences(mkposs, seqmkflag, waitMode=AWG70002.SequenceItem.TriggerMode.OFF)
#         print("Write Marker Sequence OK")
#
#         dev.start([1])
#         ts = time.clock()
#         # TODO: When are we ready? Prepared for external trigger?
#         while not dev.isAWGready():
#             time.sleep(0.5)
#             print((time.clock()-ts))
#         dev.stop()

if __name__ == '__main__':
    awg = TekAWG70k('TCPIP0::192.168.25.52::inst0::INSTR')
    print(awg.getIdentity())
    awg.writeWaveform("TestWaveform", [1, 2, 3, 4, 5, 6, 7, 8])
