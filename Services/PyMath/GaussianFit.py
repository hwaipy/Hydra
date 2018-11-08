import sys
from scipy.optimize import curve_fit
from scipy import asarray as ar, exp
import numpy as np

def singlePeakGaussianFit(xs, ys):
    dataX = [float(s) for s in xs]
    dataY = [float(s) for s in ys]

    def gaus(x, a, x0, sigma):
        return a * exp(-(x - x0) ** 2 / (2 * sigma ** 2))

    x = ar(dataX)
    y = ar(dataY)
    mean = sum(x * y) / sum(y)
    sigma = np.sqrt(sum(y * (x - mean) ** 2) / sum(y))
    popt, pcov = curve_fit(gaus, x, y, p0=[1, mean, sigma])
    result = [i for i in popt]
    return result