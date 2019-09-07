# 	FileName:DAboard.py
# 	Author:LinJin
# 	E-mail:18685029093@163.com
# 	All right reserved @ LinJin.
# 	Modified: 2018.2.18
#   Description:The class of DAC

import socket
import time
import sys

from types import coroutine
from DAboard_defines import *
import struct
import math
from itertools import repeat
from collections import Counter
import asyncio

class DABoard(object):
    """
        DA 板对象

        实现与DA硬件的连接，

        """

    def __init__(self):
        self.board_def  = DABoard_Defines()
        self.port       = 80
        self.zeros      = list(repeat(0,1024))
        self.channel_amount = 4
        # Initialize core parameters
        self.daTrigDelayOffset  = 0
        self.offsetCorr       = [0,0,0,0,0,0,0,0]
        self.da_chip          = 0 # 0 AD 9136  1 for LTC2000A
        # Create a TCP/IP socket
        self.sockfd = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sockfd.settimeout(5.0)
        self.soft_version = None

    def connect(self, addr):
        """Connect to Server"""
        #host = '149.199.131.176'
        host = addr
        print ('Host name is: {}'.format(host))
        try:
            self.sockfd.connect((host, self.port))
            print ('connected')
            # rcv_data = self.Read_RAM(0x80000000, 1024)
            # self.soft_version = [int(rcv_data[718]),int(rcv_data[717]),int(rcv_data[716])]
            return 1
        except socket.error as msg:
            self.sockfd.close()
            self.sockfd = None
        if self.sockfd is None:
            print ('ERROR:Could not open socket')
            return -1


    def disconnect(self):
        """Close the connection to the server."""
        if self.sockfd is not None:
            print ('Closing socket')
            self.sockfd.close()

    def Write_Reg(self, bank, addr, data):
        """Write to register command."""
        cmd = self.board_def.CMD_WRITE_REG
        #I need to pack bank into 4 bytes and then only use the 3
        packedBank = struct.pack("l", bank)
        unpackedBank = struct.unpack('4b', packedBank)

        packet = struct.pack("4bLL", cmd, unpackedBank[0], unpackedBank[1], unpackedBank[2], addr, data)
    #     print ('this is my packet: {}'.format(repr(packet)))
        #Next I need to send the command
        try:
            self.send_data(packet)
        except socket.timeout:
            print ("Timeout raised and caught")
        #next read from the socket
        try:
            stat, data = self.receive_data()
        except socket.timeout:
            print ("Timeout raised and caught")
        if stat != 0x0:
            print ('Issue with Write Command stat: {}'.format(stat))
            return self.board_def.STAT_ERROR

        return self.board_def.STAT_SUCCESS

    def Read_Reg(self, bank, addr, data=0):
        """Read from register command."""
        # data is used for spi write
        cmd = self.board_def.CMD_READ_REG
        # data = 0xFAFAFAFA

        #I need to pack bank into 4 bytes and then only use the 3
        packedBank = struct.pack("l", bank)
        unpackedBank = struct.unpack('4b', packedBank)

        packet = struct.pack("4bLi", cmd, unpackedBank[0], unpackedBank[1], unpackedBank[2], addr, data)
        #Next I need to send the command
        try:
            self.send_data(packet)
        except socket.timeout:
            print ("Timeout raised and caught")
        #next read from the socket
        try:
            self.recv_stat, self.recv_data = self.receive_data()
        except socket.timeout:
            print ("Timeout raised and caught")

        if self.recv_stat != 0x0:
            print ('Issue with Reading Register stat={}!!!'.format(self.recv_stat) )
            return self.board_def.STAT_ERROR
        return self.recv_data

    def Read_RAM(self, addr, length):
        """Read from RAM command."""
        cmd = self.board_def.CMD_READ_MEM
        pad = 0xFAFAFA

        #I need to pack bank into 4 bytes and then only use the 3
        packedPad = struct.pack("l", pad)
        unpackedPad = struct.unpack('4b', packedPad)

        packet = struct.pack("4bLL", cmd, unpackedPad[0], unpackedPad[1], unpackedPad[2], addr, length)
        #Next I need to send the command
        self.send_data(packet)
        #next read from the socket
        recv_stat, recv_data = self.receive_data()
        if recv_stat != 0x0:
            print ('Issue with Reading RAM stat: {}'.format(recv_stat))
            return self.board_def.STAT_ERROR
        ram_data = self.receive_RAM(int(length))
        return ram_data

    def Write_RAM(self, start_addr, wave):
        """Write to RAM command."""
        cmd = self.board_def.CMD_WRITE_MEM
        pad = 0xFFFFFF
        #I need to pack bank into 4 bytes and then only use the 3
        packedPad = struct.pack("L", pad)
        unpackedPad = struct.unpack('4b', packedPad)
        length = len(wave) << 1 #short 2 byte
        packet = struct.pack("4bLL", cmd, unpackedPad[0], unpackedPad[1], unpackedPad[2], start_addr, length)
        #Next I need to send the command
        self.send_data(packet)
        #next read from the socket
        recv_stat, recv_data = self.receive_data()
        if recv_stat != 0x0:
            print ('Ram Write cmd Error stat={}!!!'.format(recv_stat))
            return self.board_def.STAT_ERROR
        #method 1
        # format = str(len(wave))+'H'
        # format = "{0}d".format(len(wave))
        format = "{0:d}H".format(len(wave))
        if self.da_chip == 1:
            format = ">{0:d}h".format(len(wave))
        packet = struct.pack(format, *wave)
        # print(packet[0:64])
        # format = '{:02x}{:02x}-'*32
        # print(format.format(*(packet[0:64])))
        #method 2
        # packet = struct.pack('H'*len(wave), *wave)
        #method 3
        # packet = b''.join(struct.pack('H', elem) for elem in wave)

        self.send_data(packet)
        #Use a while loop to send all the data in the data array
        # while sent_data < (length/4):
        # #    print ('this is my RAM data: {}'.format(hex(data[sent_data])))
        #     ram_packet = struct.pack("L", data[sent_data])
        #     self.send_data(ram_packet)
        #     sent_data = sent_data + 1

        #next read from the socket to ensure no errors occur
        self.sockfd.settimeout(20)
        stat, data = self.receive_data()
        self.sockfd.settimeout(5)
        # print(packet)
        if stat != 0x0:
            print ('Ram Write Error stat={}!!!'.format(stat))
            return self.board_def.STAT_ERROR

    def Run_Command(self, ctrl, data0, data1):
        """Run command."""

        cmd = self.board_def.CMD_CTRL_CMD
        packedCtrl = struct.pack("l", ctrl)
        unpackedCtrl = struct.unpack('4b', packedCtrl)
        packet = struct.pack("4bLL", cmd, unpackedCtrl[0], unpackedCtrl[1], unpackedCtrl[2], data0, data1)
    #    print ('this is my cmd packet: {}'.format(repr(packet)))
    #     print(ctrl, data0, data1)
        self.send_data(packet)
        stat, data = self.receive_data()
        if stat != 0x0:
            print ('Dump RAM Error stat={}!'.format(stat))
        return data

    def send_data(self, msg):
        """Send data over the socket."""
        totalsent = 0
        # tt= struct.unpack('c'*len(msg), msg)
        # print(tt)
        while totalsent < len(msg):
            sent = self.sockfd.send(msg)
            if sent == 0:
                raise RuntimeError("Socket connection broken")
            totalsent = totalsent + sent

    def receive_data(self):
        """Read received data from the socket."""
        chunks = []
        bytes_recd = 0
        while bytes_recd < 8:
            #I'm reading my data in byte chunks
            chunk = self.sockfd.recv(min(8 - bytes_recd, 4))
            if chunk == '':
               raise RuntimeError("Socket connection broken")
            chunks.append(chunk)
            bytes_recd = bytes_recd + len(chunk)
        stat_tuple = struct.unpack('L', chunks[0])
        data_tuple = struct.unpack('L', chunks[1])
        stat = stat_tuple[0]
        data = data_tuple[0]
        return stat, chunks[1]

    def receive_RAM(self, length):
        """Read received data from the socket after a read RAM command."""
        chunks = []
        ram_data = b''
        bytes_recd = 0
        self.sockfd.settimeout(5)
        while bytes_recd < length:
            #I'm reading my data in byte chunks
            chunk = self.sockfd.recv(min(length - bytes_recd, 1024))
            #Unpack the received data
            # data = struct.unpack("L", chunk)
            # print(type(chunk))
            ram_data += chunk
            if chunk == '':
               raise RuntimeError("Socket connection broken")
            # chunks.append(chunk)
            bytes_recd = bytes_recd + len(chunk)
        # print(bytes_recd)
        #  print ('Print something I can understand: {}'.format(repr(chunks)))
        #  print ram_data
        return ram_data
    def Init(self):
        """Read received data from the socket after a read RAM command."""
        # dac address 1136 to 1139 should all 255
        #只需要最低字节
        rd_data = (struct.unpack('4c',self.Run_Command(self.board_def.CTRL_RD_DAC1,addr,0))[0] for addr in range(1136,1140))
        rd_data = (struct.unpack('4c',self.Run_Command(self.board_def.CTRL_RD_DAC2,addr,0))[0] for addr in range(1136,1140))

        for data in rd_data:
            print(data)
        #  print ('Print something I can understand: {}'.format(repr(chunks)))
        #  print ram_data
        return rd_data

    def SetTrigDelay(self,point):
        self.SetTrigStart((self.daTrigDelayOffset+point)/8+1)
        self.SetTrigStop((self.daTrigDelayOffset+point)/8+10)
    def DA_reset(self):
        """Run command."""
        print('da reset please wait 20 seconds')
        cmd = self.board_def.CMD_CTRL_CMD
        ctrl = self.board_def.CTRL_REINIT
        data0 = 1
        data1 = 0
        packedCtrl = struct.pack("l", ctrl)
        unpackedCtrl = struct.unpack('4b', packedCtrl)
        packet = struct.pack("4bLL", cmd, unpackedCtrl[0], unpackedCtrl[1], unpackedCtrl[2], data0, data1)
    #    print ('this is my cmd packet: {}'.format(repr(packet)))
        self.send_data(packet)
        time.sleep(5)
        print('da reset done')
        return 0
    def DA_reprog(self):
        """Run command."""
        print('da reprog please wait 20 seconds')
        cmd = self.board_def.CMD_CTRL_CMD
        ctrl = self.board_def.CTRL_REINIT
        data0 = 2
        data1 = 0
        packedCtrl = struct.pack("l", ctrl)
        unpackedCtrl = struct.unpack('4b', packedCtrl)
        packet = struct.pack("4bLL", cmd, unpackedCtrl[0], unpackedCtrl[1], unpackedCtrl[2], data0, data1)
    #    print ('this is my cmd packet: {}'.format(repr(packet)))
        self.send_data(packet)
        time.sleep(10)
        print('da reprog done')
        return 0

    def SpiReadWrite(self,R_W=1, spi_dev=2, spi_sel=3, reg_addr=0, reg_data=0):
        # R_W 1 read 0 write
        # spi_dev = 2 读LTC2000A的SPI， 1 读9516,9136的SPI，0读配置FLASH的SPI（一般禁止）
        # spi_sel 在spi_dev = 1时  0表示9516， 1表示9136-1 2表示9136-2
        # reg_data 写入的数据，仅低8位有效
        addr = (spi_dev << 24) | (spi_sel << 16) | reg_addr | (R_W << 7)
        tmp = self.Read_Reg(self.board_def.BANK_SPI, addr, reg_data)
        return struct.unpack('i', tmp)[0]
        # return tmp

    def SetRamRead(self,flag):
        # 1 read lower data
        self.Run_Command(self.board_def.CTRL_READ_FLAG,flag,0)
    def Set_watchdog_timeout(self, reprog_timeout, reset_timeout ):
        '''
        设置DA板底层逻辑复位和重配置的超时时间，
        reprog_timeout 重配置超时计数，单位10ms每计数
        reset_timeout 重复位超时计数，单位10ms每计数
        该命令目前有以下作用
        1. 禁止喂狗
        2. 禁止底层计数第10位为1时的自动复位
        3. 超时时间到达时一定会发生复位或重配置（取决于谁的的计数小）
        小心使用，目前只打算使用在配置过程中，实现可靠重配置
        '''
        self.Run_Command(self.board_def.CTRL_SET_WDTO, reprog_timeout, reset_timeout)
    def SetTotalCount(self,count):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,1,count << 16)
    def SetDACStart(self,count):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,2,count << 16)
    def SetDACStop(self,count):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,3,count << 16)
    def SetTrigStart(self,count):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,4,count << 16)
    def SetTrigStop(self,count):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,5,count << 16)
    def SetIsMaster(self,ismaster):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,6,ismaster << 16)
    def SetTrigSel(self,sel):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,7,sel << 16)
    def SendIntTrig(self):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,8,1 << 16)
    def SetTrigInterval(self,T):
        # self.Run_Command(self.board_def.CTRL_SYNC_CTRL,9,math.floor(T/4e-9) << 12)
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,9,T << 12)
    def SetTrigCount(self,count):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,10,count << 12)
    def ClearTrigCount(self):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,11,1 << 16)
    def EnableDASync(self):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,12,1 << 16)
    def SetTrigIntervalL2(self,T):
        # self.Run_Command(self.board_def.CTRL_SYNC_CTRL,9,math.floor(T/4e-9) << 12)
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,15,T << 12)
    def SetTrigCountL2(self,count):
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,16,count << 12)
    def setDAADSyncDelay(self, cnt):
        # da sync方式为idelay
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,17,0x80000000) #EN_VT SET
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,17,0x00000000) #EN_VT SET
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,17,cnt << 16)  #set delay value, disable EN_VT
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,17,cnt << 16 | 0x8000) #enable delay load, self cleared delay load
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,17,0x80000000) #EN_VT
    def setOutputSwitch(self,data):#0:switch off 1:switch on
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,18,data<<16)
    def MultiBoardMode(self,data):#1:single board mode 0: multi board mode
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,19,data<<16)
    def SwitchTrigMode(self,ch):
        ## 设置到AD板的触发源
        ## 0 触发模块
        ## 1 序列通道1
        ## 2 序列通道2
        ## 3 序列通道3
        ## 4 序列通道4
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,20,ch << 16)
    def setDAADPLLDelay(self, cnt):
        # da sync方式为移动pll相位
        # cnt = 0 增加相位
        # cnt = 1 减少相位
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,17,cnt << 31 | 0x00008000) #EN_VT SET

    def setDAfeedbackDelay(self, tdc_step):
        # da sync方式为tdc延时链，最大有96个step
        self.Run_Command(self.board_def.CTRL_SYNC_CTRL,17,(tdc_step << 16) | 0x00008000) #EN_VT SET
    def SetLoop(self,loop1,loop2,loop3,loop4):
        self.Run_Command(self.board_def.CTRL_SET_LOOP,(loop1 << 16) | loop2,(loop3 << 16) | loop4)
    def StartStop(self,index):
        self.Run_Command(self.board_def.CTRL_START_PLAY,index,0)
    def PowerOnDAC(self,chip, onoff):
        self.Run_Command(self.board_def.CTRL_DAC_POWER,chip, onoff)
    def InitBoard(self):
        self.Run_Command(self.board_def.CTRL_INIT,0, 0)
    def AdpCompt(self):
        # adp_compt = (self.Run_Command(self.board_def.CTRL_JESD_DATA_IF_SY_STAT,0,0)) & 0x00800000
        adp_compt = (self.Run_Command(self.board_def.CTRL_JESD_DATA_IF_SY_STAT,0,0))
        return adp_compt
    def SetGain(self,channel, data):
        map_ch = [2,3,0,1]
        channel = map_ch[channel]
        self.Write_Reg(7, channel, data)
        # self.Write_Reg(self.board_def.CTRL_SET_GAIN,channel, data)
    def SetOffset(self,channel, data):
        map_ch = [6,7,4,5]
        channel = map_ch[channel]
        self.Write_Reg(7, channel, data)
        # self.Run_Command(self.board_def.CTRL_SET_OFFSET,channel, data)
    def SetDefaultVolt(self,channel, volt):

        volt = volt + self.offsetCorr[channel-1]
        volt = 65535 if volt > 65535 else volt  # 范围限制
        volt = 0 if volt < 0 else volt  # 范围限制
        volt = 65535 - volt         # 由于负通道接示波器，数据反相方便观察
        #print('default volt{0}'.format(volt))
        self.Run_Command(self.board_def.CTRL_DAC_DEFAULT,channel-1, volt)
    def SetBoardcast(self,isBoardcast, period):
        self.Run_Command(self.board_def.CTRL_MONITOR,isBoardcast, period)

    def WriteSeq(self,ch,seq):
        if(ch < 1 or ch > self.channel_amount):
            print('Wrong channel!')        #检查通道编号
        startaddr = (ch*2-1)<<18             #序列的内存起始地址，单位是字节。
        self.Write_RAM(startaddr, seq)

    def WriteSeq(self,ch,seq):
        if(ch < 1 or ch > self.channel_amount):
            print('Wrong channel!')        #检查通道编号
        startaddr = (ch*2-1)<<18             #序列的内存起始地址，单位是字节。
        self.Write_RAM(startaddr, seq)
        # print(startaddr,seq)

    def WriteWave(self,ch,wave):
        if(ch < 1 or ch > self.channel_amount):
            print('Wrong channel!')        #检查通道编号
        startaddr = (ch-1)<<19             #波形数据的内存起始地址，单位是字节。
        self.Write_RAM(startaddr, wave)

    def update_DelayIP_reg(self):
        ## 高精度延时模块寄存器寄存器值刷入状态包中
        self.Run_Command(self.board_def.CTRL_UPDATE_DELAYIP_REG, 0, 0)

    def DelayIP_write(self,addr, data):
        ## 高精度延时模块寄存器写入
        ## 地址范围0-15，每个地址4字节数据
        data0 = addr | (1<<8)
        self.Run_Command(self.board_def.CTRL_UPDATE_DELAYIP_REG, data0, data)

    def DelayIP_read(self,addr):
        ## 高精度延时模块寄存器读取
        ## 地址范围0-15，每个地址4字节数据
        data0 = addr | (2<<8)
        reg_data = self.Run_Command(self.board_def.CTRL_UPDATE_DELAYIP_REG, data0, 0)
        return reg_data

    def SetDacOffset(self, channel, offset_code):
        """

        :param self: AWG对象
        :param channel: 通道值（1，2，3，4）
        :param offset_code: 对应的DA通道的offset值，精度到1个LSB
        :return: None，网络通信失败表示命令设置失败
        """

        self._channel_check(channel)
        # self.user_setted_offset[channel - 1] = offset_code

        ch_map = [3, 4, 1, 2]
        ch = ch_map[channel - 1]
        dac_sel = (((ch - 1) >> 1) + 1) << 24
        page = ((ch - 1) & 0x01) + 1
        temp1 = (offset_code + self._calibrated_offset[channel - 1] >> 0) & 0xFF
        temp2 = (offset_code + self._calibrated_offset[channel - 1] >> 8) & 0xFF
        self.Run_Command(self.board_def.CTRL_DAC_WRITE, data1=(dac_sel | 0x008), data0=page)
        self.Run_Command(self.board_def.CTRL_DAC_WRITE, data1=(dac_sel | 0x135), data0=1)  # 使能offset
        self.Run_Command(self.board_def.CTRL_DAC_WRITE, data1=(dac_sel | 0x136), data0=temp1)  # LSB [7:0]
        self.Run_Command(self.board_def.CTRL_DAC_WRITE, data1=(dac_sel | 0x137), data0=temp2)  # LSB [15:8]
        self.Run_Command(self.board_def.CTRL_DAC_WRITE, data1=(dac_sel | 0x13A), data0=0)  # SIXTEEN [4:0]

    def WriteFLASH_old(self, data):
        """Write to data to flash old version."""
        print('program flash start')
        start_addr = 9 << 18
        cmd = self.board_def.CMD_WRITE_MEM
        pad = 0xFFFFFF
        #I need to pack bank into 4 bytes and then only use the 3
        packedPad = struct.pack("L", pad)
        unpackedPad = struct.unpack('4b', packedPad)
        length = len(data)
        packet = struct.pack("4bLL", cmd, unpackedPad[0], unpackedPad[1], unpackedPad[2], start_addr, length)
        #Next I need to send the command
        self.send_data(packet)
        #next read from the socket
        recv_stat, recv_data = self.receive_data()
        if recv_stat != 0x0:
            print ('Ram Write cmd Error stat={}!!!'.format(recv_stat))
            return self.board_def.STAT_ERROR

        self.send_data(data)
        #next read from the socket to ensure no errors occur
        self.sockfd.settimeout(1000);
        stat, data = self.receive_data()
        self.sockfd.settimeout(5)
        # print(packet)

        print('program flash end')
        if stat != 0x0:
            print ('Ram Write Error stat={}!!!'.format(stat))
            return self.board_def.STAT_ERROR
    def WriteGoldenFLASH_old(self, data):
        """Write to RAM command."""
        print('program flash start')
        start_addr = 10 << 18
        cmd = self.board_def.CMD_WRITE_MEM
        pad = 0xFFFFFF
        #I need to pack bank into 4 bytes and then only use the 3
        packedPad = struct.pack("L", pad)
        unpackedPad = struct.unpack('4b', packedPad)
        length = len(data)
        packet = struct.pack("4bLL", cmd, unpackedPad[0], unpackedPad[1], unpackedPad[2], start_addr, length)
        #Next I need to send the command
        self.send_data(packet)
        #next read from the socket
        recv_stat, recv_data = self.receive_data()
        if recv_stat != 0x0:
            print ('Ram Write cmd Error stat={}!!!'.format(recv_stat))
            return self.board_def.STAT_ERROR

        self.send_data(data)
        #next read from the socket to ensure no errors occur
        self.sockfd.settimeout(1000);
        stat, data = self.receive_data()
        self.sockfd.settimeout(5)
        # print(packet)

        print('program flash end')
        if stat != 0x0:
            print ('Ram Write Error stat={}!!!'.format(stat))
            return self.board_def.STAT_ERROR

    def WriteFLASH(self, data, config_addr, is_first_page):
        """Write to FLASH command.
            data 待写入数据
            config_addr 写入起始地址
            is_first_page 是否写入第一页
        """
        print('program flash start')
        # 地址最高位为1表示 写入FLASH操作
        # 地址低两位为1 表示常规写入， 为2表示写入第一页
        start_addr = 0x80000000 | (config_addr & 0x00FFFF00) | (is_first_page+1)
        cmd = self.board_def.CMD_WRITE_MEM
        pad = 0xFFFFFF
        #I need to pack bank into 4 bytes and then only use the 3
        packedPad = struct.pack("L", pad)
        unpackedPad = struct.unpack('4b', packedPad)
        length = len(data)
        packet = struct.pack("4bLL", cmd, unpackedPad[0], unpackedPad[1], unpackedPad[2], start_addr, length)
        #Next I need to send the command
        self.send_data(packet)
        #next read from the socket
        recv_stat, recv_data = self.receive_data()
        if recv_stat != 0x0:
            print ('Ram Write cmd Error stat={}!!!'.format(recv_stat))
            return self.board_def.STAT_ERROR

        self.send_data(data)
        #next read from the socket to ensure no errors occur
        self.sockfd.settimeout(400);
        stat, data = self.receive_data()
        self.sockfd.settimeout(5)
        # print(packet)

        print('program flash end')
        if stat != 0x0:
            print ('Ram Write Error stat={}!!!'.format(stat))
            return self.board_def.STAT_ERROR

    def EraseFlashSector(self,addr, sectors):
        print('sector erase start')
        # self.sockfd.settimeout(300)
        self.Run_Command(self.board_def.CTRL_ERASE_PART,addr, sectors)
        # self.sockfd.settimeout(5)
        # time.sleep(20)
        # print('sector erase stop')

    def EraseFlashEntire(self):
        print('entire erase start')
        self.sockfd.settimeout(1000)
        self.Run_Command(self.board_def.CTRL_ERASE_ALL,0, 0)
        self.sockfd.settimeout(5)
        time.sleep(130)
        print('entire erase end')

    def StartTrigAdapt(self):
        print('entire erase start')
        self.Run_Command(self.board_def.CTRL_CMD_ADPT,0, 0)
        print('trig adapt started')

    def GetFlashType(self):
        status = self.Read_RAM(0x80000000, 1024)
        # print(status)
        return status[937]

    def GetEraseStatus(self):
        self.sockfd.settimeout(60)
        status = self.Read_RAM(0x80000000, 1024)
        self.sockfd.settimeout(5)
        return status[916]

    def GetDAADSyncErrCnt(self):
        status = self.Read_RAM(0x80000000, 1024)
        return status[732]+(status[733]<<8)+(status[734]<<16)+(status[735]<<24)