$(document).ready(function () {
    clientTable = new SortedTable("client-table", {
        theme: 'blue',
        widthFixed: true,
    }, onClickTableData)
    $('#refresh-frequency').val(1)
    $('#refresh-frequency').change(function () {
        var a = $("#refresh-frequency").val();
        refreshPeriod = a
        refreshCount = 0
    })
    setInterval("refreshTrigger()", 1000)
    updateClientInformation()
});

var refreshCount = 0
var refreshPeriod = 1

function refreshTrigger() {
    refreshCount++
    if (refreshCount >= refreshPeriod) {
        refreshCount = 0
        updateClientInformation()
        doUpdateSummaryInformation()
    }
}

function updateClientInformation() {
    requestMessage({"Request": ["sessionsInformation"]}, function (msg) {
        msg = msg[0]
        if (typeof msg === "string") {
            console.warn(msg)
            return
        }
        //console.log(msg)
        var newTrs = []
        for (var i = 0; i < msg.length; i++) {
            var ci = new ClientInformation(msg[i])
            newTrs.push(ci.tr())
        }
        clientTable.updateTrs(newTrs)
    })
}

function onClickTableData(e) {
    var tr = e.target.parentNode
    var trID = tr.getAttribute('id')
    summaryClient = tr.childNodes[1].innerHTML
    doUpdateSummaryInformation()
    // doUpdateProtocols()
    doUpdateDocument()
}

var summaryClient = ""

function doUpdateSummaryInformation() {
    if (summaryClient != "") {
        requestMessage({"Request": ["getSummary"], "To": summaryClient}, function (msg) {
            msg = msg [0]
            var converter = new showdown.Converter()
            var md2html = converter.makeHtml(msg)
            document.getElementById('summary').innerHTML = md2html
        })
    }
}

function doUpdateDocument() {
    if (summaryClient != "") {
        requestMessage({"Request": ["getDocument"], "To": summaryClient}, function (msg) {
            msg = msg [0]
            msg = applyExtendsRefs(msg)
            var converter = new showdown.Converter()
            var md2html = converter.makeHtml(msg)
            document.getElementById('doc').innerHTML = md2html
        })
    }
}

function applyExtendsRefs(md) {
    var regex1 = RegExp('@protocol\\((.+?)\\)', 'g');
    var array1;
    var protocols = new Array()
    while ((array1 = regex1.exec(md)) !== null) {
        protocols.push(array1)
    }
    protocols.forEach(function (e) {
        md = md.replace(e[0], "[" + e[1] + "](/apps/protocols.html#" + e[1] + ")")
    })
    return md
}

function doUpdateProtocols() {
    if (summaryClient != "") {
        requestMessage({"Request": ["getProtocols"], "To": summaryClient}, function (msg) {
            msg = msg [0]
            if ((typeof msg) == "string") document.getElementById('protocols').innerHTML = "<b>Protocols</b>: None"
            else {
                ps = "<b>Protocols</b>: "
                for (var i = 0; i < msg.length; i++) {
                    p = msg[i]
                    ps += "<a href=\"/apps/protocols.html#" + p + "\">" + p + "</a>"
                    if (i != msg.length - 1) ps += ", "
                }
                document.getElementById('protocols').innerHTML = ps
            }
        })
    }
}

function ClientInformation(msg) {
    this.id = msg[0]
    this.tableID = 'client-id-' + this.id
    this.name = msg[1]
    this.connectedTime = msg[2]
    this.messageSend = msg[3]
    this.messageReceived = msg[4]
    this.bytesSend = msg[5]
    this.bytesReceived = msg[6]

    this.contents = contents

    function contents() {
        return [this.id, this.name, formatTimePeriod(new Date().getTime() - this.connectedTime), this.messageSend, this.messageReceived, this.bytesSend, this.bytesReceived]
    }

    this.tr = tr

    function tr() {
        var TR = document.createElement('tr')
        TR.setAttribute('id', this.tableID)
        c = this.contents()
        for (var j = 0; j < c.length; j++) {
            var td = document.createElement('td')
            var text = document.createTextNode(c[j])
            td.appendChild(text)
            TR.appendChild(td)
        }
        return TR
    }

    function formatTimePeriod(ms) {
        function checkTime(i, l) {
            var t = '' + i
            while (t.length < l) {
                t = "0" + t
            }
            return t
        }

        var d = Math.floor(ms / 3600000 / 24)
        var h = Math.floor(ms / 3600000) % 24
        var m = Math.floor(ms / 60000) % 60
        var s = Math.floor(ms / 1000) % 60
        var ms = ms % 1000
        var ft = checkTime(h, 2) + ":" + checkTime(m, 2) + ":" + checkTime(s, 2) + "." + checkTime(ms, 3)
        if (d <= 0) {
        } else if (d == 1) {
            ft = d + ' day  ' + ft
        } else {
            ft = d + ' days  ' + ft
        }
        return ft
    }
}