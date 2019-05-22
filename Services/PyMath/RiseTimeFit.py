import sys
from scipy.optimize import curve_fit
from scipy import asarray as ar, exp
import numpy as np
import matplotlib.pyplot as plt


# global riseRef
# global ysBuffer
# global ysBufferCount

# riseRef = -1
# ysBuffer = []
# ysBufferCount = 0

def riseTimeFit(xs, ys):
    # global ysBuffer, ysBufferCount
    # if len(ysBuffer) is 0:
    #     ysBuffer = [0] * len(ys)
    #
    # for i in range(0, len(ys)):
    #     ys[i] += ysBuffer[i]
    #

    # in case there is a large background
    bgs = ys[int(len(ys) * 0.8):-1]
    bg = sum(bgs) / len(bgs)
    ys = [y - bg for y in ys]

    SPD = [ys[0]]
    for y in ys[1:]:
        SPD.append(SPD[-1] + y)
    roughRise = 0
    for i in range(0, len(xs)):
        if SPD[i] > 0.04 * SPD[-1]:
            roughRise = xs[i]
            break

    fitXs = []
    fitYs = []
    for i in range(0, len(xs)):
        if xs[i] >= roughRise and xs[i] <= roughRise + 1.7:
            fitXs.append(xs[i])
            fitYs.append(SPD[i])

    # ysBufferCount += 1
    # if ysBufferCount <= 4:
    #     for i in range(0, len(ys)):
    #         ysBuffer[i] = ys[i]
    #     return 1e10


    def linear(x, a, b):
        return a * x + b

    expectA = (fitYs[0] - fitYs[-1]) / (fitXs[0] - fitXs[-1])
    expectB = fitYs[0] - expectA * fitXs[0]
    popt, pcov = curve_fit(linear, fitXs, fitYs, p0=[expectA, expectB])

    # plt.plot(fitXs, fitYs)
    # plt.plot(fitXs, [linear(x, popt[0], popt[1]) for x in fitXs])
    # plt.show()
    # global riseRef
    rise = -popt[1] / popt[0]
    # if riseRef is -1:
    #     riseRef = rise
    # print((rise-riseRef)*1000)
    ysBuffer = []
    ysBufferCount = 0
    return rise
