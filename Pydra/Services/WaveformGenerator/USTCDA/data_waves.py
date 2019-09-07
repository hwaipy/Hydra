import csv
import time
import os
from itertools import repeat, chain
import numpy as np
import matplotlib.pyplot as plt

import os
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.pyplot import savefig
import matplotlib as mpl
# mpl.rcParams['font.family'] = 'sans-serif'
# mpl.rcParams['font.sans-serif'] = 'NSimSun,Times New Roman' #中文除外的设置成New Roman，中文设置成宋体
# mpl.rcParams['axes.unicode_minus']=False

mode = (u'直接输出', u'一级循环开始', u'一级循环结束', u'二级循环开始', u'二级循环结束', u'三级循环开始',u'三级循环结束', u'四级循环开始', u'四级循环结束','触发输出',u'计数输出',u'态判断输出',u'')
def print_csv(x_list, unit, listv, listv1, listv2, dirname,note, filename, outdir):
    global mode
    y_pos = np.arange(len(mode))
    x_pos = np.arange(len(x_list))
    plt.rc('figure', figsize=(9, 4))

    fig, host = plt.subplots()

    par1 = host.twinx()
    par1.grid()
    host.plot(range(0, len(listv2)), listv2, 'g*', label=u'码值')
    p1, = host.plot(range(0, len(listv)), listv, 'b-', label=u'码值')
    # p3, = host.plot(range(0, len(listv)), listv, 'g-', label=u'物理值2')
    p2, = par1.plot(range(0, len(listv1)), listv1, 'r.', label=u'序列类型')

    host.set_xlim(0, len(listv))
    if (filename.find('温度') > -1):
        if (filename.find('APD') > -1):
            host.set_ylim(-17, (max(listv) + 0.1) * 1.1)
        else:
            host.set_ylim(5, (max(listv) + 0.1) * 1.1)
    else:
        host.set_ylim((min(listv) - 0.1) * 0.9, (max(listv) + 0.1) * 1.1)

    plt.yticks(y_pos, mode, rotation=0, fontsize='small')

    nn = x_pos[::int(len(x_list) / 10)]
    xx = []
    for ii in nn:
        xx.append(str(x_list[ii])+unit)
    tt = list(nn)
    if (x_pos[-1]-nn[-1]) / nn[1] > 0.49:##最后一个标识不能太近
        xx.append(str(x_list[-1])+unit)
        tt.append(x_pos[-1])
    # else:
    #     print(x_pos[-1]-nn[-1], )
    # tt=np.append(nn,[x_pos[-1]])
    # print(tt)
    plt.xticks(tt, xx, rotation=60)

    ss = filename
    if ss.find('-') > -1:
        host.set_xlabel(ss[:ss.find('-')])
    else:
        host.set_xlabel(ss)
    host.set_ylabel(u'码值')
    par1.set_ylabel(u'序列类型')

    host.yaxis.label.set_color(p1.get_color())
    # host.yaxis.label.set_color(p3.get_color())
    par1.yaxis.label.set_color(p2.get_color())

    tkw = dict(size=4, width=1.5)
    host.tick_params(axis='y', colors=p1.get_color(), **tkw)
    # host.tick_params(axis='y', colors=p3.get_color(), **tkw)
    par1.tick_params(axis='y', colors=p2.get_color(), **tkw)
    host.tick_params(axis='x', **tkw)

    lines = [p1, p2]
    #host.legend(lines, [l.get_label() for l in lines])
    #ax1.legend(loc=1, ncol=3, shadow=True)
    ss = filename##.decode('cp936')

    plt.title(note)
    if (filename.find('/') > 0):
        tt = filename.split('/')  # [filename.index('/')]='_'
        filename = tt[0] + tt[1]
    filename = filename.strip() + '.png'
    # print filename
    pathname = os.path.join(outdir, filename)

    savefig(pathname)
    plt.show()
    # plt.clf()
    # plt.close()

class waveform:
    def __init__(self):
        self.amplitude = 65535
        self.defaultvolt = 20000
        self.frequency = 2e10
        self.seq = None
        self.wave = None
        self.generate_sin()

    def generate_seq(self, start_addr=0, length=40):
        ## 生成连续波形序列
        unit = [start_addr,length,0,0]
        self.seq = unit*4096

    def generate_count_seq(self, start_addr=0, length=5000, wait_cnt=30000):
        unit = [0,1250,wait_cnt,0x4<<11]*3
        unit = unit+[1250,10000,2,0x4<<11]
        self.seq = unit*(4096>>1)

    def generate_trig_seq(self, loopcnt=16, repeat=2000, start_addr=0, length=25):
        unit = [start_addr,length,repeat,0x8<<11]
        last = [start_addr,length,repeat,1<<15|0x8<<11]
        # if repeat < 2:
        #     self.seq = last*loopcnt
        # else:

        self.seq = unit*(loopcnt-1)+last+[0,0,0,0]*8
        # print('序列长度：',len(self.seq))

    def generate_squr(self, repeat=0, lowtime1=0, hightime=100, lowtime2=100, low=0, high=65535, pad=1):
        unit=[low]*lowtime1+[high]*hightime+[low]*lowtime2
        self.pad_unit(repeat, unit, pad)

    def generate_sin(self, repeat=8, cycle_count=16, low=0, high=32767, offset=32768, pad=1):
        unit = np.sin(np.arange(0, 2*np.pi, (2*np.pi)/cycle_count))*high+offset
        unit = unit.astype(np.int32)
        unit = list(unit)
        self.pad_unit(repeat, unit, pad)

    def generate_cos(self, repeat=8, cycle_count=16, low=0, high=32767, offset=32768, pad=1):
        unit = np.cos(np.arange(0, 2*np.pi, (2*np.pi)/cycle_count))*high+offset
        unit = unit.astype(np.int32)
        unit = list(unit)
        self.pad_unit(repeat, unit, pad)

    def generate_dc(self, dc_code=32767, length=16, pad=1):
        unit = [dc_code]*length
        self.pad_unit(1, unit, pad)

    def generate_pulse(self, dc_code=0, pad=1):
        #生成脉冲波形
        step_cnt = 32767
        temp = [i*int(32767/step_cnt) for i in range(1,step_cnt)]
        temp = [32768 for i in range(1,step_cnt)]
        unit = [32768]*10000+[dc_code]*20000+temp+[32768]*(30000-0)
        self.pad_unit(1, unit, pad)

    def generate_saw(self, repeat=0, cycle_count=20, low=0, high=65535, pad=1):
        #由低到高的三角
        # repeat：波形个数
        #cycle_count 三角波周期所占采样点个数
        #high；最大码值
        #low：最小码值
        unit = np.arange(low, high, (high-low)/cycle_count)
        unit = unit.astype(np.int32)
        unit = list(unit)
        self.pad_unit(repeat, unit, pad)

    def generate_inv_saw(self, repeat=0, cycle_count=20, low=0, high=65535, pad=1):
        #由高到低的三角波
        # repeat：波形个数
        #cycle_count 三角波周期所占采样点个数
        #high；最大码值
        #low：最小码值
        unit = np.arange(high, low, (low-high)/cycle_count)
        unit = unit.astype(np.int32)
        unit = list(unit)
        self.pad_unit(repeat, unit, pad)

    def pad_unit(self, repeat, unit, pad=1):
        if pad == 1:
            if repeat > 0:
                unit *= repeat
                seq_len = (len(unit)+7) >> 3
                pad_len = (32-(len(unit)&0x1F))&0x1F
                unit += [self.defaultvolt]*pad_len#32个采样点对齐
                self.generate_trig_seq(length=seq_len)
                self.wave = unit
            else:
                # rep_map = [1,8,4,8,2,8,4,7]
                # rep_len = rep_map[len(unit) & 0x7]
                # unit = unit*(rep_len)
                seq_len = (len(unit)+7) >> 3
                # print(seq_len)
                # print("seq_len")
                pad_len = (32-(len(unit)&0x1F))&0x1F
                unit += [self.defaultvolt]*pad_len#32个采样点对齐
                self.generate_seq(length=seq_len)
                self.wave = unit
        else:
            self.wave = unit
        # plt.figure()
        # plt.plot(self.wave)
        # plt.show()
        # print(self.wave)
        # print('波形长度：',len(self.wave))

    def gen_step_wave(self, steps=16, step_length=16):
        '''产生台阶波形， 每个台阶16个采样点，台阶间隔为65536/steps'''
        step_size = int(65536/steps)
        step_da = 2
        seq = []
        wave = []
        for i in range(steps):
            seq = seq + [i*step_da,step_da,0,0x8<<11]
            wave = wave + [step_size*i]*16
        seq = seq + [step_da*steps,step_da,0,1<<15|0x8<<11]
        wave = wave + [0]*16
        self.wave = wave+[0]*32
        # self.wave[0:16] = wave[-16:]
        self.seq = seq+[0]*32

    def get_loop_mode(self):
        loop_mode = ''
        func_dic = {0:'S', 1:'L', 2:'J', 8:'T', 12:'C', 4:'D'}
        loop_dic = {0:'(', 1:'[', 2:'{', 3:'<'}
        jump_dic = {0:')', 1:']', 2:'}', 3:'>'}
        start_addr = [[],[],[],[]]
        for seq_idx in range(len(self.seq) >> 2):
            idx = seq_idx << 2
            func = (self.seq[idx+3] >> 11) & 0x000F
            # level = self.seq[idx] #老版本
            level = (self.seq[idx+3] >> 8) & 0x03
            count = self.seq[idx + 2]
            stop = (self.seq[idx + 3] >> 15) & 0x0001

            label = func_dic[func]
            if label=='L':
                label = str(count)+loop_dic[level]
                start_addr[level].append(seq_idx)
                print('{}层循环起始：地址：{}'.format(level,seq_idx))
            if label=='J':
                label = jump_dic[level]
                if len(start_addr[level])==0:
                    print('错误：循环起始与结束不匹配:地址{}'.format(idx))
                self.seq[idx+2] = start_addr[level][-1]
                start_addr[level].pop()
                print('{}层循环结束：跳转：{}'.format(level, self.seq[idx+2]))
            loop_mode += label
            if stop==1:
                break
        return loop_mode

    def get_seq_level_order(self, seq):
        #获取 seq中的循环等级嵌套关系
        max_cnt_dic = {0:0,1:0,2:0,3:0}
        loop_level = 0
        for seq_idx in range(len(seq) >> 2):
            idx = seq_idx << 2
            stop = (seq[idx+3] >> 15) & 0x00001
            func = (seq[idx+3] >> 11) & 0x000F
            level = (seq[idx+3] >> 8) & 0x03

            if func == 1:#loop start
                loop_level += 1
                max_cnt_dic[level] = max( max_cnt_dic[level],loop_level)
            if func == 2:#loop end or jump
                loop_level -= 1
            if stop == 1:
                break
        list1= sorted(max_cnt_dic.items(),key=lambda x:x[1], reverse=True)
        return [item[0] for item in list1]

    def seq_extend(self, seq, pro_level):
        extended_seq = []
        loop_start_addr = 0
        loop_end_addr = 0
        loop_cnt = 0
        loop_level = 0
        find_start = 0
        has_level = 0
        # print('input seq')
        # print(seq)
        for seq_idx in range(len(seq) >> 2):
            idx = seq_idx << 2
            stop = (seq[idx+3] >> 15) & 0x00001
            func = (seq[idx+3] >> 11) & 0x000F
            # level = seq[idx] #老版本
            level = (seq[idx+3] >> 8) & 0x03
            count = seq[idx+2]
            jump_addr = seq[idx+2] << 2

            if func == 1:#loop start
                has_loops = 1
                if find_start == 0 and pro_level == level:
                    has_level = 1
                    loop_start_addr = idx
                    loop_cnt = count
                    loop_level = level
                    # print('循环起始：{}, 循环次数:{}, 循环级别：{}'.format(idx, count, loop_level))
                    find_start = 1
                    extended_seq += seq[loop_end_addr:idx]
                # else:
                    # print('循环级别{}'.format(level))

            if func == 2:#loop end or jump

                # print('find start {}, pro_level {}, level{}'.format(find_start, pro_level, level))
                if pro_level == level:
                    find_start = 0
                    if level != pro_level:
                        print('警告：结束级别:{}与开始级别:{}不同'.format(level, loop_level))
                    else:
                        loop_end_addr = idx+4
                        # print('当前地址：{}，跳转地址：{}, 跳转级别：{}, seq_idx {}'.format(loop_end_addr,loop_start_addr, loop_level, seq_idx))
                        # if jump_addr != loop_start_addr:
                        #     print('警告：循环结束跳转地址:{0}与循环起始地址:{1}不同, 循环级别:{2}'.format(jump_addr, loop_start_addr, loop_level))
                        # else:
                        # print(seq[loop_start_addr:loop_end_addr])
                        # print(extended_seq)
                        extended_seq += seq[loop_start_addr:loop_end_addr]*loop_cnt
                        # print(extended_seq)
            if stop == 1:
                break
        if find_start > 0:
            print('警告：未找到匹配的结束级别，开始级别:{}，已记录级别:{}'.format(loop_level, find_start))

        extended_seq += seq[loop_end_addr:]
        # rt_seq = extended_seq.copy()
        return extended_seq, has_level
    def get_wave_trig_pos(self, trig_delay, wave_length, count):
        ## 返回触发序列输出的触发的位置
        if trig_delay < 0 or trig_delay > 255:
            print('延时值越界:{}'.format(0))
            os.error('延时值越界:{}'.format(0))
        trig_pos = (count + trig_delay - 45) * 8
        return wave_length + trig_pos
    def wave_preview(self, test_note=' '):
        '''预览序列对象生成的波形'''
        wave = []
        trig_pos_list = []
        seq_mode = []
        default_volt = 32768
        pro_level = 3
        temp_seq = self.seq
        extended_seq = None

        ## 将序列的4级循环展开
        ### 获取循环嵌套关系
        loop_order = self.get_seq_level_order(temp_seq)
        # while pro_level >= 0 :
        print('循环嵌套关系：{}'.format(loop_order))
        for pro_level in loop_order:
            extended_seq, has_level = self.seq_extend(temp_seq, pro_level)
            # pro_level -= 1
            # print(self.get_loop_mode())
            temp_seq = extended_seq
        # 对展开后的序列生成对应的波形，模式，触发序列
        for seq_idx in range(len(extended_seq) >> 2):
            idx = seq_idx << 2 # 序列索引
            stop = extended_seq[idx+3] >> 15 #序列停止标识
            func = (extended_seq[idx+3] >> 11) & 0x000F # 序列类型
            start_addr = extended_seq[idx] << 3 #波形区起始地址
            wave_len = extended_seq[idx+1] << 3## 波形采样点数
            end_addr = start_addr+wave_len #波形区结束地址
            count = extended_seq[idx+2] #多功能计数区
            istrig_seq = (extended_seq[idx+3] >> 10) & 0x0001 #序列触发输出标识
            trig_delay = (extended_seq[idx+3]) & 0x00FF #触发序列的延时计数
            level = (extended_seq[idx+3] >> 8) & 0x03 #循环级别
            # mode = (u'直接输出', u'一级循环开始', u'二级循环开始', u'三级循环开始', u'四级循环开始', u'一级循环结束', u'二级循环结束',u'三级循环结束', u'四级循环结束','触发输出',u'计数输出',u'态判断输出' )
            level_label_loop = [u'一级循环开始', u'二级循环开始', u'三级循环开始', u'四级循环开始']
            level_label_jump = [u'一级循环结束', u'二级循环结束',u'三级循环结束', u'四级循环结束']
            func_dic = {0:['直接输出'], 8:['触发输出'],4:['计数输出'],12:['态判断输出'],1:level_label_loop,2:level_label_jump}
            if func_dic[func][0] ==  '态判断输出':
                start_addr = (count & 0x00FF) << 9 #低5位地址为0 #0态的地址 ## 取0态的地址
                end_addr = start_addr+wave_len #波形区结束地址

            ## 如果是序列有触发标识，添加触发信号的位置
            if istrig_seq == 1 or func_dic[func][0] ==  '触发输出':
                pos_cnt = 0
                if func_dic[func][0] ==  '计数输出':
                    pos_cnt = count
                trig_pos_list.append(self.get_wave_trig_pos(trig_delay, len(wave), pos_cnt))
            unit = []
            if func_dic[func][0] ==  '计数输出' or func_dic[func][0] ==  '触发输出':#计数类型延时
                unit = [default_volt]*((count) << 3)#延时
            unit += self.wave[start_addr:end_addr]
            wave += unit
            seq_mode += [mode.index(func_dic[func][level])]*len(unit)

            if end_addr > len(self.wave):
                print('end addr:{0}, wave length:{1}'.format(end_addr,len(self.wave)))
                print('波形有越界，实际输出可能有未知波形数据:{}'.format(extended_seq[idx:idx+4]))

            if wave_len < 4 :
                os.error('错误：波形长度小于4，不支持')

            if stop == 1:
                break
            if len(wave) > 1000000:
                print('生成wave过长')
                break
        ## 根据触发位置list生成触发序列
        wave_trig = [0]*len(wave)
        trig_time = []
        note = '连续波形输出'
        if len(trig_pos_list) > 0:
            pre_time = trig_pos_list[0]
            for idx in trig_pos_list:
                if idx < 0:
                    print('警告，触发位置超波形边界:{}ns'.format(idx*0.5))
                    idx = 0
                elif idx > len(wave):
                    print('警告，触发位置超波形边界:{}ns'.format(idx*0.5))
                    idx = -1
                print('触发时刻:{}ns'.format(idx*0.5))
                tmp_pre = idx
                if idx - pre_time < -41:
                    print('注意：有前一个序列触发时间大于当前触发序列时间现象')
                    tmp = trig_time[-1]
                    trig_time[-1] = idx*0.5
                    trig_time.append(tmp)
                    tmp_pre = tmp
                elif idx - pre_time < 0:
                    trig_time[-1] = idx * 0.5
                    tmp_pre = idx * 0.5
                    print('注意：有前一个序列触发时间大于当前触发序列时间现象, 且产生重叠')
                elif idx - pre_time < 41 and idx != pre_time:
                    ## 触发脉冲宽度为20ns，当两个触发位置间隔小于41时，存在重叠现象
                    print('注意：有触发重叠现象')
                else:
                    trig_time.append(idx*0.5)
                pre_time = tmp_pre
                wave_trig[idx] = 70000
            note = self.get_loop_mode()
        # else:
        #     seq_mode = '连续波形'
        xlist = np.arange(0,len(wave) >> 1)
        xlist = xlist.repeat(2)
        # xlist.sort()
        dir = os.getcwd()+'/测试数据'
        filename = '序列数据生成波形预览'+test_note#+'-'+time.strftime('%Y_%m_%d_%H_%M_%S', time.localtime(time.time()))

        # print(len(xlist), len(wave))
        print_csv(xlist,'ns',wave,seq_mode,wave_trig, dir,note,filename,dir)
        return len(wave), wave, trig_time

    def gen_comp_wave(self, counter=10, length=(512>>3)):
        '''产生复杂波形， 第一区间是正弦长度512，第二区间是方波，长度512，第三区间是三角波，长度512
        第一个序列输出第一个区间，触发输出，第二个序列输出第二个区间，延时20个计数器输出'''
        self.generate_sin(repeat=32)
        comp_wave = self.wave[:512]
        self.generate_squr(repeat=10,hightime=32,lowtime1=16,lowtime2=16)
        comp_wave.extend(self.wave[:512])
        self.wave = comp_wave
        unit = [0,length,0,0x8<<11]*3
        last = [512>>3,length,counter,1<<15|0x4<<11]
        self.seq = unit+last+[0,0,0,0]*6

    def load_wave_file(self):
        filename = os.path.join(os.getcwd(),'wave_data.csv')
        self.wave=[]
        with open(filename, 'r') as csvfile:
            reader = csv.reader(csvfile)
            for row in reader:
                self.wave += [int(num)+32768 for num in row]
        # print(os.getcwd())
        # print(self.wave)
    def gen_reset_control_wave_seq(self, wait_state_cnt=144, gate_seq_cnt=99):
        '''产生复杂波形， 第一区间是正弦长度512，第二区间是方波，长度512，第三区间是三角波，长度512
        第一个序列输出第一个区间，触发输出，第二个序列输出第二个区间，延时20个计数器输出'''
        unit1 = [0,2,0,0x8<<11]
        unit2 = [0,2,144,0x4<<11]
        unit3 = [772,25,258,0xC<<11]
        unit4 = [320,63,99,0x4<<11]
        unit5 = [0,2,0,0x18<<11]
        self.seq = unit1+unit2+unit3+unit4+unit5+[0,0,0,0]*3
# aa = waveform()
#
# import matplotlib.pyplot as plt
# plt.plot(aa.wave)
# plt.show()

# wa = waveform()
# wa.seq = [0, 16, 0, 17453, 257, 4, 257, 24576, 0, 4, 3, 2304, 257, 4, 257, 25645, 0, 4, 2, 2560, 0, 4, 3, 4608, 0, 4, 3, 2816, 16, 4, 20, 9264, 0, 16, 0, 8192, 0, 4, 0, 1072, 16, 16, 0, 0, 0, 4, 8, 4864, 257, 4, 257, 24576, 257, 4, 257, 25645, 0, 4, 3, 2816, 0, 4, 2, 4352, 0, 4, 9, 4864, 0, 16, 0, 32768, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
# print()
# wa.wave_preview()

## 0[1{}<2345>678]9