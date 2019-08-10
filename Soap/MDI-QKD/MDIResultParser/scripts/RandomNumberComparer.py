from datetime import datetime
import math
import matplotlib.pyplot as plt
import numpy as np
import sys
import os
import random

lengthOfRandomNumbers = 1000
pAlice = [0.382779, 0.086313, 0.497856]  # PX, PY, PZ
pBob = [0.380657, 0.085882, 0.504446]  # PX, PY, PZ
randomBasis = ['O', 'X', 'Y', 'Z']

if __name__ == '__main__':
    root = '/Users/hwaipy/Dropbox/Labwork/Projects/_2017-10-26 MDI-QKD/2019-08-07 正式采数/1.randomNumbers/20190802'
    rAFile = '{}/{}'.format(root, [f for f in os.listdir(root) if f.__contains__('Alice')][0])
    rBFile = '{}/{}'.format(root, [f for f in os.listdir(root) if f.__contains__('Bob')][0])
    rAs = np.load(rAFile)
    rBs = np.load(rBFile)
    rPairs = [z for z in zip(rAs, rBs)]

    if sum(pAlice) > 1: raise RuntimeError('sum of pAlice can not exceed 1.')
    if sum(pBob) > 1: raise RuntimeError('sum of pBob can not exceed 1.')
    pAs = [1 - sum(pAlice)] + pAlice
    pBs = [1 - sum(pBob)] + pBob

    if not len(rAs) == len(rBs): raise RuntimeError(
        'length of {} not equals to length of {}'.format(randomNumberFileAlice, randomNumberFileBob))
    if not lengthOfRandomNumbers == len(rAs): raise RuntimeError(
        'LengthOfRandomNumber not equal to length of RandomNumberFile.')

    for iA in range(len(randomBasis)):
        for iB in range(len(randomBasis)):
            validRandomPairs = [rP for rP in rPairs if (int(rP[0] / 2) == iA) and (int(rP[1] / 2) == iB)]
            corrects = [rP for rP in validRandomPairs if sum(rP) % 2 == 1]
            wrongs = [rP for rP in validRandomPairs if sum(rP) % 2 == 0]
            print('{}{}: expect {:.1f}, actual {}. correct:wrong = {}:{}'.format(randomBasis[iA], randomBasis[iB],
                                                                                 lengthOfRandomNumbers * pAs[iA] * pBs[
                                                                                     iB], len(validRandomPairs),
                                                                                 len(corrects), len(wrongs)))
