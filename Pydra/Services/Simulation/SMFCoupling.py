import time
import math
import numpy as np
from numba import *
from numba import cuda
import numba
import os
import matplotlib.pyplot as plt
from matplotlib import cm

os.environ['CUDA_HOME'] = r'C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v10.1'


class Wavefront:
    def __init__(self, E, size):
        self.E = E
        self.size = size
        self.grid = self.E.shape[0]

    def EArray(self):
        return np.copy(self.E)

    def xs(self):
        return np.linspace(-self.size / 2, self.size / 2, self.E.shape[1]).data

    def ys(self):
        return np.linspace(-self.size / 2, self.size / 2, self.E.shape[0]).data

    def I(self):
        return np.sum(self.E.real ** 2 + self.E.imag ** 2)

    @classmethod
    def plane(cls, size, grid):
        return Wavefront(np.ones((grid, grid), dtype=complex) / grid, size)

    @classmethod
    def gaussian(cls, sigma, size, grid):
        E = np.zeros((grid, grid), dtype=complex)
        for yi in range(0, grid):
            for xi in range(0, grid):
                x = (xi / float(grid) - 1.0 / 2) * size
                y = (yi / float(grid) - 1.0 / 2) * size
                E[yi][xi] = complex(math.exp(-(x * x + y * y) / (sigma * sigma)), 0)
        return Wavefront(E * (size / grid / sigma) / np.sqrt(np.pi / 2), size)

    def turbulencedByZernike(self, zernikeIndices):
        E = self.EArray()
        for zi in range(len(zernikeIndices)):
            weight = zernikeIndices[zi]
            zPhase = ZernikePhase.get(zi, self.size, self.grid).array
            E *= np.exp(weight * zPhase * 1j)
        return Wavefront(E, self.size)

    def turbulencedByPhaseMask(self, phases):
        E = self.EArray()
        E *= np.exp(phases * 1j)
        return Wavefront(E, self.size)


class ZernikePhase:
    def __init__(self, index, size, grid):
        self.index = index
        self.size = size
        self.grid = grid
        self.xs = np.linspace(-size / 2, size / 2, self.grid)
        self.ys = np.linspace(-size / 2, size / 2, self.grid)
        self.n = int(math.ceil((-3 + math.sqrt(9 + 8 * self.index)) / 2))
        self.m = int(self.n + (self.index - (self.n + 3) * self.n / 2) * 2)
        self.array = self.generateZernikePhase(grid) / np.power(self.size, self.n)

    instances = {}

    @classmethod
    def get(cls, index, size, grid):
        key = '{}_{}_{}'.format(size, grid, index)
        if not ZernikePhase.instances.__contains__(key):
            ZernikePhase.instances[key] = ZernikePhase(index, size, grid)
        return ZernikePhase.instances[key]

    def generateZernikePhase(self, grid):
        array = np.zeros((grid, grid), dtype=float)
        deltam = 1 if (self.m == 0) else 0
        Norm = math.sqrt(2 * (self.n + 1) / (1 + deltam))
        for s in range(0, int((self.n - math.fabs(self.m)) / 2 + 1)):
            flag = 1 if (s % 2 == 0) else -1
            prodA = self.prod(1, (self.n - s))
            prodB = self.prod(1, s)
            prodC = self.prod(1, int((self.n + math.fabs(self.m)) / 2 - s))
            prodD = self.prod(1, int((self.n - math.fabs(self.m)) / 2 - s))
            for ix in range(0, grid):
                for iy in range(0, grid):
                    x = self.xs[ix]
                    y = self.ys[iy]
                    rho = math.sqrt(x * x + y * y)
                    array[iy][ix] += flag * prodA * math.pow(rho, self.n - 2 * s) / (prodB * prodC * prodD)
        for ix in range(0, grid):
            for iy in range(0, grid):
                x = self.xs[ix]
                y = self.ys[iy]
                if x == 0:
                    theta = math.pi / 2 if (y >= 0) else -math.pi / 2
                elif y == 0:
                    theta = 0 if (x >= 0) else math.pi
                else:
                    theta = math.atan(y / x) + (0 if (x > 0) else math.pi)
                array[iy][ix] = (-Norm * math.sin(self.m * theta) if (self.m < 0) else Norm * math.cos(
                    self.m * theta)) * array[iy][ix]
        return array

    def prod(self, start, stop):
        return np.prod(np.linspace(start, stop, stop - start + 1))

    def show(self):
        fig = plt.figure()
        ax = fig.gca(projection='3d')
        X = np.linspace(-self.size / 2, self.size / 2, self.grid)
        Y = np.linspace(-self.size / 2, self.size / 2, self.grid)
        X, Y = np.meshgrid(X, Y)
        Z = self.array
        surf = ax.plot_surface(X, Y, Z, cmap=cm.coolwarm, linewidth=0, antialiased=False)
        plt.show()


@cuda.jit
def doCaptureKernel(captureXs, captureYs, wfXs, wfYs, wfPhaseLenses, wfMILEs, z, k, Es):
    startX, startY = cuda.grid(2)
    gridX = cuda.gridDim.x * cuda.blockDim.x
    gridY = cuda.gridDim.y * cuda.blockDim.y

    for captureXI in range(startX, len(captureXs), gridX):
        captureX = captureXs[captureXI]
        for captureYI in range(startY, len(captureYs), gridY):
            captureY = captureYs[captureYI]
            rr = Es[captureYI][captureXI]
            for wavefrontXI in range(len(wfXs)):
                for wavefrontYI in range(len(wfYs)):
                    wavefrontX = wfXs[wavefrontYI][wavefrontXI]
                    wavefrontY = wfYs[wavefrontYI][wavefrontXI]
                    wavefrontPhaseLens = wfPhaseLenses[wavefrontYI][wavefrontXI]
                    wavefrontMILE = wfMILEs[wavefrontYI][wavefrontXI]
                    deltaX = captureX - wavefrontX
                    deltaY = captureY - wavefrontY
                    r = math.sqrt(deltaX * deltaX + deltaY * deltaY + z * z)
                    phase = ((wavefrontPhaseLens + r) * k)
                    cosP = math.cos(phase)
                    sinP = math.sin(phase)
                    IT12 = complex(cosP, sinP)
                    IT3 = (1 + z / r) / r
                    IT = IT12 * IT3 * wavefrontMILE
                    rr += IT
            Es[captureYI][captureXI] = rr


@jit(nopython=True, parallel=True)
def doCapture(captureXs, captureYs, wfXs, wfYs, wfPhaseLenses, wfMILEs, z, k, Es):
    for captureXI in range(len(captureXs)):
        captureX = captureXs[captureXI]
        for captureYI in range(len(captureYs)):
            captureY = captureYs[captureYI]
            deltaX = captureX - wfXs
            deltaY = captureY - wfYs
            r = np.sqrt(deltaX * deltaX + deltaY * deltaY + z * z)
            phase = (wfPhaseLenses + r) * k
            cosP = np.cos(phase)
            sinP = np.sin(phase)
            IT12 = cosP + sinP * complex(0, 1)
            IT3 = (1 + z / r) / r
            IT = IT12 * IT3 * wfMILEs
            Es[captureYI][captureXI] += np.sum(IT)


class Capture:
    def __init__(self, captureDistance, captureSize, captureGrid, lensF, lensThickness, lensN, lamda, gpu=False):
        self.lensF = lensF
        self.captureDistance = captureDistance
        self.captureSize = captureSize
        self.captureGrid = captureGrid
        self.lensN = lensN
        self.lamda = lamda
        self.lensThickness = lensThickness
        self.lensR = lensF * (lensN - 1)
        self.k = 2 * np.pi / self.lamda
        self.gpu = gpu

    def capture(self, wavefront):
        roundedWavefront = self.roundWavefront(wavefront)
        return self.captureIntensity(wavefront, roundedWavefront[0], roundedWavefront[1], roundedWavefront[2],
                                     roundedWavefront[3])

    def capturePower(self, wavefront):
        intensities = self.capture(wavefront)
        r = self.captureGrid / 2
        power = 0
        for i in range(self.captureGrid):
            for j in range(self.captureGrid):
                if np.sqrt((i - r) ** 2 + (j - r) ** 2) <= r:
                    power += intensities[i, j]
        return power

    def captureAmplitudeWithRoundedWavefront(self, wavefrontXs, wavefrontYs, wavefrontPhaseLenses, wavefrontMILEs):
        Es = np.zeros((self.captureGrid, self.captureGrid), dtype=complex)
        captureXs = np.linspace(-self.captureSize / 2, self.captureSize / 2, self.captureGrid)
        captureYs = np.linspace(-self.captureSize / 2, self.captureSize / 2, self.captureGrid)
        if self.gpu:
            blockdim = (32, 8)
            griddim = (32, 16)
            dEs = cuda.to_device(Es)
            doCaptureKernel[griddim, blockdim](captureXs, captureYs, wavefrontXs, wavefrontYs, wavefrontPhaseLenses,
                                               wavefrontMILEs, self.captureDistance, self.k, dEs)
            dEs.to_host()
        else:
            doCapture(captureXs, captureYs, wavefrontXs, wavefrontYs, wavefrontPhaseLenses, wavefrontMILEs,
                      self.captureDistance, self.k, Es)
        return Es

    def captureIntensity(self, wavefront, wavefrontXs, wavefrontYs, wavefrontPhaseLenses, wavefrontMILEs):
        amplitude = self.captureAmplitudeWithRoundedWavefront(wavefrontXs, wavefrontYs, wavefrontPhaseLenses,
                                                              wavefrontMILEs)
        intensities = np.zeros((self.captureGrid, self.captureGrid), dtype=float)
        for captureXI in range(self.captureGrid):
            for captureYI in range(self.captureGrid):
                amp = amplitude[captureYI][captureXI]
                intensities[captureYI][captureXI] = (amp * amp.conjugate()).real
        return intensities / (wavefront.grid * self.captureGrid / wavefront.size / self.captureSize) ** 2

    def roundWavefront(self, wavefront):
        EArray = wavefront.EArray()
        shape = EArray.shape
        wavefrontXs = np.zeros((shape[0], shape[1]), dtype=float)
        wavefrontYs = np.zeros((shape[0], shape[1]), dtype=float)
        wavefrontPhaseLenses = np.zeros((shape[0], shape[1]), dtype=float)
        wavefrontMILEs = np.zeros((shape[0], shape[1]), dtype=complex)
        wavefrontGrid = EArray.shape[0]
        xs = wavefront.xs()
        ys = wavefront.ys()
        for xi in range(0, wavefrontGrid):
            x = xs[xi]
            for yi in range(0, wavefrontGrid):
                y = ys[yi]
                wavefrontXs[yi][xi] = x
                wavefrontYs[yi][xi] = y
                wavefrontPhaseLenses[yi][xi] = (self.lensN - 1) * (math.sqrt(
                    self.lensR * self.lensR - x * x - y * y) - self.lensR + self.lensThickness) + self.lensThickness
                if (math.sqrt(x * x + y * y) <= wavefront.size / 2):
                    wavefrontMILEs[yi][xi] = complex(0, -1) / 2 / self.lamda * EArray[yi][xi]
        return [wavefrontXs, wavefrontYs, wavefrontPhaseLenses, wavefrontMILEs]


def save2D(intensities, filename):
    fi = intensities.flatten()
    maxI = max(fi)
    minI = min(fi)
    grays = [[0 for cn in range(intensities.shape[1])] for rn in range(intensities.shape[0])]
    for y in range(intensities.shape[0]):
        for x in range(intensities.shape[1]):
            grays[y][x] = int((intensities[y][x] - minI) / (maxI - minI) * 255.9)
    import png
    png.from_array(grays, 'L').save(filename)


class SMFCModel:
    def __init__(self, wavefrontGrid, wavefrontSize, beamWaist, captureGrid, captureSize, lamda, f):
        self.wavefrontGrid = wavefrontGrid
        self.wavefrontSize = wavefrontSize
        self.beamWaist = beamWaist
        self.captureGrid = captureGrid
        self.captureSize = captureSize
        self.lamda = lamda
        self.f = f
        self.wavefront = Wavefront.gaussian(beamWaist, wavefrontSize, wavefrontGrid)
        self.turbulencedWavefront = self.wavefront.turbulencedByZernike([0])

    def turbulencedByZernike(self, turbulenceZernikeIndices):
        self.turbulencedWavefront = self.wavefront.turbulencedByZernike(turbulenceZernikeIndices)

    def turbulencedByPhaseMask(self, phases):
        self.turbulencedWavefront = self.wavefront.turbulencedByPhaseMask(phases)

    def couplingEfficiency(self):
        capture = Capture(captureDistance=self.f, captureSize=self.captureSize, captureGrid=self.captureGrid,
                          lensF=self.f, lensThickness=0, lensN=1.5, lamda=self.lamda, gpu=False)
        return capture.capturePower(self.turbulencedWavefront)


if __name__ == '__main__':
    model = SMFCModel(wavefrontGrid=10, wavefrontSize=13e-3, beamWaist=3e-3, captureGrid=50,
                      captureSize=0.172e-3, lamda=0.81e-6, f=1.0)

    t1 = time.time()
    ds = np.linspace(-5, 5, 100)
    phasesBase = np.array([1] * 50 + [-1] * 50).reshape((10, 10))
    print(phasesBase)
    for d in ds:
        model.turbulencedByPhaseMask(phasesBase * d)
        eff = model.couplingEfficiency()
        print('{}\t{}'.format(d, eff))

    t2 = time.time()
    print((t2 - t1) / len(ds))
