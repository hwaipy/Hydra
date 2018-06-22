$(document).ready(function () {
    protocolName = decodeURI(window.location.hash.replace("#", ""))
    loadProtocolDetail(protocolName)
});

function loadProtocolDetail(protocolName) {
    var xhr = new XMLHttpRequest()
    console.log("/protocols/" + protocolName)
    xhr.open("GET", "/protocols/" + protocolName + ".md", true)
    xhr.onload = function () {
        if (this.status == 200) {
            document.title = "Protocal: " + protocolName
            var md = this.response
            md = applyExtendsRefs(md)
            var converter = new showdown.Converter()
            var md2html = converter.makeHtml(md)
            document.getElementById('protocol').innerHTML = md2html
        } else {
            document.title = "Protocal: " + protocolName + " (Not Found)"
            document.getElementById('protocol').innerHTML = "Protocol " + protocolName + " does not exist."
        }
    }
    xhr.send()
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