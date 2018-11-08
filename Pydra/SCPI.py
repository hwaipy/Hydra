__author__ = 'Hwaipy'


class SCPI:
    def __init__(self, query, write):
        self.query = query
        self.write = write

    def __getattr__(self, item):
        return SCPICommand(self, item)


class SCPICommand:
    def __init__(self, scpi, cmd, parent=None):
        self.scpi = scpi
        self.parent = parent
        self.cmd = cmd
        if self.cmd[0] == '_':
            self.cmd = '*' + self.cmd[1:]
        if parent:
            self.fullCmd = parent.fullCmd + ":" + self.cmd
        else:
            self.fullCmd = self.cmd

    def query(self, *args):
        re = self.scpi.query(self.createCommand(True, [arg for arg in args]))
        if re is not None:
            if (len(re)>0) and (re[-1]=='\n'):
                re = re[:-1]
        return re

    def write(self, *args):
        self.scpi.write(self.createCommand(False, [arg for arg in args]))

    def createCommand(self, isQuery, args=[]):
        cmd = self.fullCmd
        if isQuery:
            cmd += '?'
        if len(args) > 0:
            cmd += ' {}'.format(args[0])
            for i in range(1, len(args)):
                cmd += ',{}'.format(args[i])
        return cmd

    def __getattr__(self, item):
        return SCPICommand(self.scpi, item, self)

    def __str__(self):
        return '[SCPI]' + self.fullCmd


if __name__ == "__main__":
    def query(cmd):
        print('[Qeury]' + cmd)


    def write(cmd):
        print('[Write]' + cmd)


    scpi = SCPI(query, write)
    print(scpi._IDN.fff.query(['1', '2', 3]))
    print(scpi._IDN.aaaF.write(['1', '2', 3]))
