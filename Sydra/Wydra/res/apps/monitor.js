$(document).ready(function () {
    $(".tablesorter").tablesorter({
        theme: 'blue',
        widthFixed: true,
    })
    $(".debug-trigger").click(function () {
        console.log('mo')
    })
    $('#refresh-frequency').val(5)
    $('#refresh-frequency').change(function () {
        var a = $("#refresh-frequency").val();
        refreshPeriod = a
        refreshCount = 0
    })
    setInterval("refreshTrigger()", 1000)
    updateClientInformation()
});

var refreshCount = 0
var refreshPeriod = 5
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
        console.log(msg)
        var informations = [], newTableIDs = []
        for (var i = 0; i < msg.length; i++) {
            var ci = new ClientInformation(msg[i])
            informations.push(ci)
            newTableIDs.push(ci.tableID)
        }
        var table = document.getElementById('client-table').getElementsByTagName('tbody')[0]
        var trs = table.getElementsByTagName('tr')
        var existTableIDs = []
        var existTableRows = []
        for (var i = 0; i < trs.length; i++) {
            existTableIDs.push(trs[i].getAttribute('id'))
            existTableRows.push(trs[i])
        }
        for (var i = 0; i < existTableIDs.length; i++) {
            existTableID = existTableIDs[i]
            var keep = $.inArray(existTableID, newTableIDs) >= 0
            if (!keep) {
                table.removeChild(existTableRows[i])
            }
        }
        for (var i = 0; i < newTableIDs.length; i++) {
            var newTableID = newTableIDs[i]
            var index = $.inArray(newTableID, existTableIDs)
            var information = informations[i]
            var contents = information.contents()
            if (index == -1) {
                var tr = document.createElement('tr')
                tr.setAttribute('id', information.tableID)
                table.appendChild(tr)
                for (var j = 0; j < contents.length; j++) {
                    var td = document.createElement('td')
                    var text = document.createTextNode(contents[j])
                    td.appendChild(text)
                    tr.appendChild(td)
                }
                var onClockUtilFunction = function (name) {
                    return function () {
                        onClickTableRow(name)
                    }
                }
                $('#' + information.tableID).click(onClockUtilFunction(information.name))
            } else {
                var tr = document.getElementById(information.tableID)
                var tds = tr.getElementsByTagName('td')
                for (var j = 0; j < tds.length; j++) {
                    tds[j].innerHTML = contents[j]
                }
            }
        }
        $(".tablesorter").trigger("update")
    })
}

function requestMessage(message, onResponse) {
    var buffer = msgpack.encode(message)
    var bytesArray = new Uint8Array(toArray(buffer))
    var xhr = new XMLHttpRequest()
    xhr.open("POST", "/wydra/request/abc", true)
    xhr.responseType = "arraybuffer"
    xhr.setRequestHeader('Content-Type', 'application/octet-stream')
    xhr.onload = function () {
        if (this.status == 200) {
            var data = this.response
            var msg = msgpack.decode(new Uint8Array(data))
            onResponse(msg)
        }
    }
    xhr.send(bytesArray)
}

function toArray(buffer) {
    return Array.prototype.slice.call(buffer);
}

function onClickTableRow(name) {
    console.log('oc: ' + name)
    summaryClient = name
    doUpdateSummaryInformation()
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