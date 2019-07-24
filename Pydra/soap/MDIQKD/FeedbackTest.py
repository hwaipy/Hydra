import Pydra
import time

class FeedbackTest:
    def __init__(self):
        self.session = Pydra.Session.newSession(('192.168.25.27', 20102))

    def zTest(self):
        # self.zFeedback()
        self.phaseLock()

    def zFeedback(self):
        dc = self.session.blockingInvoker('DC-MDI-Alice-Time1')
        apd = self.session.blockingInvoker('APD-MDI-Alice-ZFB')
        # channels, delays = [0,1], [5.3, 8.3, 15.3, 18.3]
        timeAM = {0:"TimeAMII", 1:"TimeAMI"}

        print('start scan')
        for i in range(30,130):
            apd.setDelay(i/10)
            time.sleep(0.5)
            print('{}\t{}'.format(apd.getDelay(), apd.detectorFrequency(1)))

        # for chnl in channels:
        #     while True:
        #         zerates, init_vltg= [], dc_aliceAM.getVoltageSetPoint(chnl)
        #         print("通道{}初始电压值：{:.2f}".format(chnl, init_vltg))
        #         voltages = {chnl:np.linspace(init_vltg-0.5, init_vltg+0.5,11)}
        #         assert  max(voltages[chnl]) <= 13.5
        #         for vltg in voltages[chnl]:
        #             dc_aliceAM.setVoltage(chnl,vltg)
        #             print("设置与实际电压值：{:.2f}\t{:.2f}".format(vltg, dc_aliceAM.getVoltageSetPoint(chnl)))
        #             time.sleep(0.5)
        #             zerates.append(float("{:.5f}".format(ec.getAPDZERate(delays))))
        #
        #         print("{}偏压值与误码率:".format(timeAM[chnl]))
        #         print(voltages[chnl])
        #         print(zerates)
        #
        #         bestVltg_inx = zerates.index(min(zerates))
        #         dc_aliceAM.setVoltage(chnl,voltages[chnl][bestVltg_inx])
        #         print("{}优化后的电压值与误码率：{:.2f}\t\t{:.3f}%".format(timeAM[chnl], dc_aliceAM.getVoltageSetPoint(chnl),100*ec.getAPDZERate(delays)))
        #         if bestVltg_inx not in [0, -1]: break
        # while True:
        #     print("实时误码率为：{:.4f}%".format(float(100*self.getAPDZERate(delays))))
        # print("Z基矢本地反馈结束！")

    def phaseLock(self):
        import numpy as np
        dc = self.session.blockingInvoker('DC-MDI-Alice-PL')
        apd = self.session.blockingInvoker('APD-MDI-Alice-PL')

        scan_cnts = []
        scan_vltgs = list(np.arange(0,28.1,0.2))
        for vltg in scan_vltgs:
            dc.setVoltage(0,vltg)
            time.sleep(0.2)
            cnt = apd.detectorFrequency(1)
            scan_cnts.append(cnt)
            print("{:.2f}\t{:.1f}".format(vltg,cnt))
    #     opml_vltg = scan_vltgs[scan_cnts.index(min(scan_cnts))]
    #     ratio = self.PL_APD.detectorFrequency(1)/min(scan_cnts)
    #     print("扫描得到最佳电压与曲线消光比：{:.2f}\t{:.2f}".format(opml_vltg, ratio))
    #     plt.plot(scan_vltgs, scan_cnts)
    #     plt.title("Scan Ratio:{:.2f}".format(ratio))
    #     plt.savefig("phaseLockScan{}.png".format(time.strftime("%Y%m%d%H%M%S",time.localtime())))
    #     plt.ion()
    #     plt.pause(5)
    #     plt.close()
    #     while True:
    #         fdbk_cnts = []
    #         if ratio < 50:
    #             print("消光比小于50，需要锁相反馈，采用5点定标法锁相反馈：")
    #             fdbk_vltgs = np.arange(opml_vltg-0.8,opml_vltg+0.9,0.3)
    #             for vltg in fdbk_vltgs:
    #                 dc_alicePL.setVoltage(0,vltg)
    #                 time.sleep(1)
    #                 fdbk_cnts.append(self.PL_APD.detectorFrequency(1))
    #             ratio = max(scan_cnts)/min(fdbk_cnts)
    #             opml_vltg = fdbk_vltgs[fdbk_cnts.index(min(fdbk_cnts))]
    #             dc_alicePL.setVoltage(0,opml_vltg)
    #             print("最优电压与消光比为：{:.2f}\t{:.2f}\t{:.2f}".format(opml_vltg,dc_alicePL.getVoltageSetPoint(0),ratio))
    #         else:
    #             ratio = max(scan_cnts)/self.PL_APD.detectorFrequency(1)
    #             print("实时消光比为：{:.2f}".format(ratio))
    #             with open("phaseLockfdbk.txt","a") as file:
    #                 file.write("{:.2f}\t{:.2f}\n".format(time.time(), ratio))


if __name__ == '__main__':
    test = FeedbackTest()
    test.zTest()
