import matplotlib.pyplot as plt
import numpy as np
import numpy.fft as fft
import math
import os
import msgpack


def __loadMsgpackEntries(path):
    file = open(path, 'rb')
    data = file.read(os.path.getsize(path))
    file.close()
    unpacker = msgpack.Unpacker(raw=False)
    unpacker.feed(data)
    entries = []
    for packed in unpacker:
        entries.append(packed)
    return entries

class Channel:
    def __init__(self, sections, threshold=1):
        self.sections = sections
        self.systemTimes = []
        self.entries = []
        self.riseIndices = []
        for section in self.sections:
            self.systemTimes.append(section['SystemTime'])
            channelDatas = section['Monitor']
            for channelData in channelDatas:
                self.entries.append(ChannelEntry(channelData[:3], channelData[3]))
        self.__searchForRises(threshold)

    def __searchForRises(self, threshold):
        for i in range(0, len(self.entries) - 1):
            triggerLevelPre = self.entries[i].trigger
            triggerLevelPost = self.entries[i + 1].trigger
            if (triggerLevelPre < threshold and triggerLevelPost > threshold):
                self.riseIndices.append(i)

    def validate(self):
        for i in range(0, len(self.riseIndices) - 1):
            delta = (self.entries[self.riseIndices[i + 1]].refTime - self.entries[self.riseIndices[i]].refTime) / 1000
            if math.fabs(delta - 10) > 0.02:
                raise RuntimeError("Error in ChannelMonitorSyncs: {}".format(delta))


class ChannelEntry:
    def __init__(self, powers, refTime):
        self.power1 = powers[1]
        self.power2 = powers[2]
        self.trigger = powers[0]
        self.refTime = refTime
        self.tdcTime = -1


def parseFile(source, target, file):
    channel = Channel(__loadMsgpackEntries('{}/{}'.format(source, file)))
    channel.validate()

    print(channel.riseIndices)
    # print(len(data))

    # data1 = np.array([d[1] for d in data])
    # data2 = np.array([d[2] for d in data])
    # minValue = min(min(data1), min(data2))
    # data1 = data1 - minValue + 0.01
    # data2 = data2 - minValue + 0.01
    # axHistogram1 = plt.subplot(2, 1, 1)
    # axHistogram2 = plt.subplot(2, 1, 2)
    # ysLog1 = [math.log10(y / 50) * 10 for y in data1]
    # ysLog2 = [math.log10(y / 50) * 10 for y in data2]
    # minLog = min(min(ysLog1), min(ysLog2))
    # maxLog = max(max(ysLog1), max(ysLog2))
    # axHistogram1.hist(ysLog1, np.linspace(minLog, maxLog, 100))
    # axHistogram2.hist(ysLog2, np.linspace(minLog, maxLog, 100))
    #
    # plt.show()
    # plt.savefig('{}/compare{}.png'.format(dir, index), dpi=300)
    # plt.close()


def parse(source, target):
    files = [file for file in os.listdir(source) if file.__contains__('Channel')]
    for file in files[:1]:
        parseFile(source, target, file)


if __name__ == '__main__':
    parse('E:\\MDIQKD_Parse\\Parsing\\20190912\\run-1', 'E:\\MDIQKD_Parse\\Parsing\\20190912\\run-1-parsed')
