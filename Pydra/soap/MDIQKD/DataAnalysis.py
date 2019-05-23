import os
import sys
import matplotlib.pyplot as plt


def parseFile(file):
    lines = open(file).readlines()
    dataMayDu = [[float(i.strip()) for i in line.split('\t')[1:]] for line in lines]
    data = [[0, 0, 0]]
    for d in dataMayDu:
        if data[-1][-1] != d[-1]:
            data.append(d)
    data = data[1:]

    def draw():
        plt.plot([i for i in range(0, len(data))], [d[0] for d in data])
        plt.plot([i for i in range(0, len(data))], [d[1] for d in data])
        print(f'{file}.png')
        plt.savefig(f'{file}.png', dpi=300)
        plt.close()

    # draw()

    def average(filtedData):
        qbers = [fd[2] for fd in filtedData]
        if len(qbers) == 0:
            return None
        return sum(qbers) / len(qbers)

    print(f'unfiltered: {average(data)}')

    def filter(unfilteredData, bound):
        max1 = max([d[0] for d in unfilteredData])
        max2 = max([d[1] for d in unfilteredData])
        filteredData = []
        for d in unfilteredData:
            if d[0] >= max1 * bound and d[1] >= max2 * bound:
                filteredData.append(d)

        filterEfficiency = len(filteredData) / len(unfilteredData)
        return [filterEfficiency, average(filteredData)]

    filterEfficiency, qber = filter(data, 0.5)
    # print(filterEfficiency)
    # print(qber)

    bounds = []
    filterEfficiencies = []
    qbers = []
    for i in range(0, 100):
        bound = i / 100
        bounds.append(bound)
        filterEfficiency, qber = filter(data, bound)
        filterEfficiencies.append(filterEfficiency)
        qbers.append(qber)

    fig, ax1 = plt.subplots()
    ax1.plot(bounds, filterEfficiencies)
    ax2 = ax1.twinx()
    ax2.plot(bounds, qbers, 'red')
    plt.savefig(f'{file}-filtered.png', dpi=300)
    plt.close()


dir = 'C:\\Users\\Administrator\\Desktop\\MDI1207\\'
files = [file for file in os.listdir(dir) if file.endswith('.txt')]
# files = ['PhaseFdbk_eigth.txt']
for file in files:
    parseFile(f'{dir}{file}')

    # sys.exit()
