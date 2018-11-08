function requestMessage(message, onResponse) {
    var buffer = msgpack.encode(message)
    var bytesArray = new Uint8Array(toArray(buffer))
    var xhr = new XMLHttpRequest()
    xhr.open("POST", "/wydra/request", true)
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
