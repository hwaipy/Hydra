import socket
import sys
import Utils
from Pydra import Session
import os
import threading
import time


class PXIFrequencyDivider:
    def __init__(self, adapter, port, address, serviceName):
        self.adapterServer = AdapterServer(port)
        if adapter is None:
            self.adapter = SimulatedAdapter(port)
        else:
            self.adapter = ExecutableAdapter(adapter, port)
        self.adapterServer.accept()

        class PXIFrequencyDividerService:
            def __init__(self, handler):
                self.handler = handler

            def getDivide(self):
                raise RuntimeError('')

            def setDivide(self, divide):
                self.handler.adapterServer.sendCommand(self.__createFrequancyCommand(divide))

            def setDelay(self, channel, course, fine):
                self.handler.adapterServer.sendCommand(self.__createDelayCommand(channel, course, fine))

            def __createFrequancyCommand(self, divide):
                if divide < 1 or divide > 255:
                    raise RuntimeError('Divide out of range.')
                addr = b'0x28c\0' + b'0x'
                data = format(divide, '2X').replace(' ', '0').encode('utf-8')
                cmd = addr + data + b'\0\0\0\0\0\0\0'
                return cmd

            def __createDelayCommand(self, channel, courseDelay, fineDelay):
                if channel < 0 or channel >= 40:
                    raise RuntimeError('Channel out of range.')
                if courseDelay < 0 or courseDelay >= 60000:
                    raise RuntimeError('Course Delay out of range.')
                if fineDelay < 0 or fineDelay >= 256:
                    raise RuntimeError('Fine Delay out of range.')
                addr = b'0x288\0' + b'0x'
                cn = format(channel, '2X').replace(' ', '0').encode('utf-8')
                cD = format(courseDelay, '4X').replace(' ', '0').encode('utf-8')
                fD = format(fineDelay, '2X').replace(' ', '0').encode('utf-8')
                cmd = addr + cn + cD + fD + b'\0'
                return cmd

        time.sleep(1)
        self.service = PXIFrequencyDividerService(self)
        self.session = Session.newSession(address, self.service, serviceName)

        self.service.setDivide(99)
        for c in range(0, 40):
            self.service.setDelay(c, 0, 0)


class AdapterServer:
    def __init__(self, port):
        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            self.server.bind(('', port))
        except socket.error as msg:
            print('Bind failed. Error Code : ' + str(msg[0]) + ' Message ' + msg[1])
            sys.exit()
        self.server.listen(10)

    def sendCommand(self, command):
        self.communicator.sendLater(command)

    def accept(self):
        def __dataFetcher(socket):
            data = socket.recv(100)

        def __dataSender(socket, message):
            s = socket.send(message)

        conn, addr = self.server.accept()
        self.communicator = Utils.BlockingCommunicator(conn, __dataFetcher, __dataSender)
        self.communicator.start()


class SimulatedAdapter:
    def __init__(self, port):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect(('localhost', port))
        self.communicator = Utils.BlockingCommunicator(self.socket, self.__dataFetcher, self.__dataSender)
        self.communicator.start()

    def __dataFetcher(self, socket):
        data = socket.recv(100)
        if len(data) == 0:
            raise RuntimeError('Connection closed.')
        print(data)

    def __dataSender(self, socket, message):
        pass


class ExecutableAdapter:
    def __init__(self, adapter, port):
        threading.Thread(target=self.__start, name='ExecutableAdapter_Thread').start()

    def __start(self):
        os.system('Services\WaveformGenerator\PXIFrequencyDivider\TDCTest.bat')
