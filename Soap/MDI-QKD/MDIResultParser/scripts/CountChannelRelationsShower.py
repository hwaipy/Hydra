import matplotlib.pyplot as plt
import sys
import os
import numpy as np

if __name__ == '__main__':
    # dataPath = '/Users/hwaipy/Desktop/MDIProgramTest/ResultsScala/results/20190804-234318/CountChannelRelations.csv'
    # outputPath = '/Users/hwaipy/Desktop/MDIProgramTest/ResultsScala/results/20190804-234318/CountChannelRelations.png'
    dataPath = sys.argv[1]
    outputPath = sys.argv[2]

    file = open(dataPath, 'r')
    rawData = file.readlines()[1:]
    file.close()

    data = [[float(item) for item in line.split(',')] for line in rawData]
    counts = [[d[0] for d in data], [d[1] for d in data]]
    powers = [[d[2] for d in data], [d[3] for d in data]]

    fig = plt.figure()
    ax1 = fig.add_subplot(111)
    ax2 = ax1.twinx()
    labels = ['Alice', 'Bob']
    colors = ['C0', 'C1']
    for kk in [0, 1]:
        sidePowers = powers[kk]
        sideCounts = counts[kk]
        binCount = 30
        maxPower = max(sidePowers)
        minPower = min(sidePowers)
        powerSteps = [(maxPower - minPower) / binCount * (i + 0.5) + minPower for i in range(0, binCount)]
        entryCountHistogram = [0] * binCount
        for i in range(0, len(sidePowers)):
            index = int((sidePowers[i] - minPower) / (maxPower - minPower) * binCount)
            if index == binCount: index -= 1
            entryCountHistogram[index] += 1

        validSidePowers = []
        validSideCounts = []
        for i in range(len(sidePowers)):
            if sidePowers[i] < 4.5:
                validSidePowers.append(sidePowers[i])
                validSideCounts.append(sideCounts[i])
        z = np.polyfit(validSidePowers, validSideCounts, 1)

        ax1.scatter(sidePowers, sideCounts, s=2, color=colors[kk], label='Counts', alpha=0.2)
        ax1.plot(powerSteps, [z[1] + p * z[0] for p in powerSteps], 'black')
        ax1.text(powerSteps[-1], z[1] + powerSteps[-1] * z[0], '{}: {:.2f}'.format(labels[kk], z[0]), size='large',
                 **{'horizontalalignment': 'right', 'verticalalignment': 'baseline'})
        ax2.plot(powerSteps, entryCountHistogram, colors[kk], label=labels[kk])
    ax1.set_ylabel('APD Counts')
    ax1.set_xlabel('PD Power')
    ax2.set_ylabel('Frequencies')
    plt.legend()
    plt.savefig(outputPath, dpi=300)
    plt.close()
