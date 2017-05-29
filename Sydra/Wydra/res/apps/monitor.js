console.log("1234")
// x=document.getElementById("intro");
// document.write('<p>id="intro" 的段落中的文本是：' + x.innerHTML + '</p>');
$(document).ready(function () {
    // setInterval("refresh()", 1000)
    $(".tablesorter").tablesorter();
    $(".debug-trigger").click(function () {
        refresh()
    })
});

var refreshCount = 0
var refreshPeriod = 1
function refresh() {
    refreshCount++
    if (refreshCount >= refreshPeriod) {
        refreshCount = 0
        // var buffer = msgpack.encode({"Request": ["sessionsInformation"]})
        // var bytesArray = new Uint8Array(toArray(buffer))
        // var xhr = new XMLHttpRequest()
        // xhr.open("POST", "/wydra/request/abc", true)
        // xhr.responseType = "arraybuffer"
        // xhr.setRequestHeader('Content-Type', 'application/octet-stream')
        // xhr.onload = function () {
        //     if (this.status == 200) {
        //         var data = this.response
        //         console.log(data)
        //         console.log(new Uint8Array(data))
        //         console.log(toArray(data))
        //         var msg = msgpack.decode(new Uint8Array(data))
        //         console.log(msg)
        //         //         var img = document.createElement("img");
        //         //         img.onload = function(e) {
        //         //             window.URL.revokeObjectURL(img.src);
        //         //         };
        //         //         img.src = window.URL.createObjectURL(blob);
        //         //         $("#imgcontainer").html(img);
        //     }
        // }
        // xhr.send(bytesArray)
        requestMessage({"Request": ["sessionsInformation"]}, function (msg) {
            msg = msg[0]
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
        })
    }
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

function ClientInformation(msg) {
    this.id = msg[0]
    this.tableID = 'client-id-' + this.id
}