import sys
from scipy.optimize import curve_fit
from scipy import asarray as ar, exp, sin
import numpy as np
import math

def sinFit(xs, ys, paraW=None):
    dataX = [float(s) for s in xs]
    dataY = [float(s) for s in ys]

    def sinFunction(x, a, w, p, b):
        return a * sin(w * x + p) + b

    x = ar(dataX)
    y = ar(dataY)

    def stdev(data):
        ave = sum(data)/len(data)
        sumI = 0
        for d in data:
            sumI += (d-ave)**2
        return math.sqrt(sumI/(len(data)-1))
    average = sum(y) / len(y)
    stv = stdev(y)
    w = paraW if (paraW is not None) else 2*math.pi/(x[-1]-x[0])
    preP=[stv * math.sqrt(2), w, 0, average]
    print(preP)
    popt, pcov = curve_fit(sinFunction, x, y, p0=preP)
    result = [i for i in popt]
    print(result)
    print()
    return result