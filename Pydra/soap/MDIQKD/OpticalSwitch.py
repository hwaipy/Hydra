import serial


class OpticalSwitch:
    def __init__(self):
        self.serial = serial.Serial('COM6')
        print(self.serial.readline())

    # Channel: A for 2,3, B for 4,5, C for 6,7, D for 8.9, E for 10, 11, F for 12, 13
    # def switch(self, channel, direction):
    def setSwitchStatuses(self, A=True, B=True, C=True, D=True, E=True, F=True):
        self.serial.write(b'0\r\n')
        value = (2 if F else 1) << 12
        value += (2 if E else 1) << 10
        value += (2 if D else 1) << 8
        value += (2 if C else 1) << 6
        value += (2 if B else 1) << 4
        value += (2 if A else 1) << 2
        vS = str.encode('{}\r\n'.format(value))
        self.serial.write(b'0\r\n')

    def set(self):
        self.ser.write(b'1\r\n')
        self.ser.flush()
        while True:
            print('reading...')
            print(        self.ser.readline())


if __name__ == '__main__':
    import Pydra
    import sys

    os = OpticalSwitch()
    os.setSwitchStatuses(False)
    # session = Pydra.Session.newSession(('172.16.60.199', 20102), OpticalSwitch(), 'OpticalSwitch_Charlie')
    # for line in sys.stdin:
    #     if line == 'q\n':
    #         break
    # session.stop()
