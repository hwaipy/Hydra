$(document).ready(function () {
    $.extend({
        getUrlVars: function () {
            var vars = [], hash;
            var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&')
            for (var i = 0; i < hashes.length; i++) {
                hash = hashes[i].split('=')
                vars.push(hash[0])
                vars[hash[0]] = hash[1]
            }
            return vars
        },
        getUrlVar: function (name) {
            return $.getUrlVars()[name]
        }
    })
    var showPath = decodeURI($.getUrlVars().path)
    var rs = showPath == undefined ? [] : routes(showPath)

    fileTable = new TreeTable("file-table", {
        expandable: true,
        clickableNodeNames: true
    }, updatePathInformation, onNodeSelect, [null, formatDateTime, formatDateTime, formatSize], rs)

    setInterval("refreshTrigger()", 1000)
})

var refreshCount = 0
var refreshPeriod = 3

function refreshTrigger() {
    refreshCount++
    if (refreshCount >= refreshPeriod) {
        refreshCount = 0
        updateFilesInformation()
        // updateHipInformation()
        trySyncNote()
    }
}

function updateFilesInformation() {
    updatePathInformation("")
    var expandedTrs = fileTable.getExpandedExpandableTrs()
    for (var i = 0; i < expandedTrs.length; i++) {
        updatePathInformation(expandedTrs[i].getAttribute('data-tt-id'))
    }
}

function updatePathInformation(path, onFinish) {
    requestMessage({"Request": ["listElements", "", path, true], "To": "StorageService"}, function (msg) {
        msg = msg[0]
        if (typeof msg === "string") {
            console.warn(msg)
            return
        }
        var newRows = []
        for (var i = 0; i < msg.length; i++) {
            var fi = new FileItem(msg[i])
            newRows.push(fi)
        }
        fileTable.updatePathInformation(path, newRows)
        if (onFinish != undefined) {
            onFinish()
        }
    })
}

function FileItem(msg) {
    this.name = msg.Name
    this.path = msg.Path
    this.id = this.path
    this.pathID = 'path:' + msg.Path
    this.type = msg.Type
    this.creationTime = msg.CreationTime
    this.lastAccessTime = msg.LastAccessTime
    this.lastModifiedTime = msg.LastModifiedTime
    if (this.type == 'Content') {
        this.size = msg.Size
    } else {
        this.size = -1
    }
    this.parentPath = this.path.substring(0, this.path.lastIndexOf('/'))
    this.parentId = this.parentPath
    this.itemType = 'Unknown'
    if (this.name.endsWith('.hip') && this.type == 'Collection') this.itemType = 'hip'
    else if (this.type == 'Collection') this.itemType = 'folder'
    else if (this.type == 'Content') this.itemType = 'file'
    this.expandable = this.type == 'Collection'
    this.values = [this.name, this.creationTime, this.lastModifiedTime, this.size]
}

function routes(path) {
    function parents(p, stack) {
        var index = p.lastIndexOf('/')
        var pp = p.substr(0, index)
        if (pp.length == 0) return
        else {
            stack.push(pp)
            parents(pp, stack)
        }
    }

    var rs = [path]
    parents(path, rs)
    rs.reverse()
    return rs
}

function onNodeSelect(currentId, previousId) {
    var currentHip = getContainerHip(currentId)
    var previousHip = getContainerHip(previousId)
    if (currentHip != previousHip) {
        if (previousHip != null) onHipUnselected(previousHip)
        if (currentHip != null) onHipSelected(currentHip)
    }
    var currentFile = currentId
    onFileSelection(currentFile)
}

function onHipSelected(hip) {
    currentSelectedHip = hip
    updateHipInformation()
}

function onHipUnselected(hip) {
    currentSelectedHip = null
    updateHipInformation()
}

var currentSelectedHip = null

function getContainerHip(path) {
    if (path == null || path == undefined) return null
    var rs = routes(path)
    for (var i = 0; i < rs.length; i++) {
        if (rs[i].toLowerCase().endsWith('.hip')) {
            return rs[i]
        }
    }
    return null
}

function updateHipInformation() {
    if (currentSelectedHip != null) {
        doUpdateHipNote(currentSelectedHip)
    } else {
        document.getElementById('HipEntryNote').innerHTML = ''
    }
}

var lastSyncedNote = ""

function doUpdateHipNote(path, onFinish) {
    requestMessage({"Request": ["readNote", "", path], "To": "StorageService"}, function (msg) {
            msg = msg[0]
            if (typeof msg === "string") {
                console.warn(msg)
                return
            }
            if (currentSelectedHip != path) {
                console.debug('Note of path ' + path + ' expired.')
                return
            }
            var note = msg.Note
            lastSyncedNote = note
            document.getElementById('HipEntryNote').innerHTML = '<form>Note:<br>\n<textarea id="HipEntryNoteTextArea" name="message" rows="10" cols="30">' + note + '</textarea></form>\n'
        }
    )
}

function trySyncNote() {
    currentNote = $('#HipEntryNoteTextArea')
    if (currentNote.length > 0) {
        currentNote = currentNote.val()
        if (currentNote != lastSyncedNote) {
            console.log("now updating note")
            requestMessage({
                    "Request": ["writeNote", "", currentSelectedHip, currentNote],
                    "To": "StorageService"
                }, function (msg) {
                    console.log(msg)
                    msg = msg[0]
                    console.log(msg)
                    // if (typeof msg === "string") {
                    //     console.warn(msg)
                    //     return
                    // }
                }
            )
        }
    }
}

var currentSelectedID = null
var currentViewer = null
var viewers = {'HBT': new HBTViewer()}

function onFileSelection(selectedID) {
    if (selectedID == currentSelectedID) return
    currentSelectedID = selectedID
    if (currentViewer != null) currentViewer.stop()
    fileViewerDiv = $('#FileViewer')[0]
    fileViewerDiv.innerHTML = '<div id="FileViewerContent"></div>'
    content = $('#FileViewerContent')[0]
    fileType = selectedID.split('.').reverse()[0]
    currentViewer = viewers[fileType.toUpperCase()]
    if (currentViewer != null && currentViewer != undefined) currentViewer.show(content, currentSelectedID)
}

function HBTViewer() {

    this.show = show

    function show(div, selectedID) {
        console.log('show')
        div.innerHTML = selectedID


        // requestMessage({"Request": ["readNote", "", path], "To": "StorageService"}, function (msg) {
        //         msg = msg[0]
        //         if (typeof msg === "string") {
        //             console.warn(msg)
        //             return
        //         }
        //         if (currentSelectedHip != path) {
        //             console.debug('Note of path ' + path + ' expired.')
        //             return
        //         }
        //         var note = msg.Note
        //         lastSyncedNote = note
        //         document.getElementById('HipEntryNote').innerHTML = '<form>Note:<br>\n<textarea id="HipEntryNoteTextArea" name="message" rows="10" cols="30">' + note + '</textarea></form>\n'
        //     }
        // )
    }

    this.stop = stop

    function stop() {
        console.log('stop')
    }
}

//
// function selectInHipTable() {
// }
//
// function HipEntry(msg) {
//     this.name = msg.Name
//     this.path = msg.Path
//     this.parentPath = this.path.substring(0, this.path.lastIndexOf('/'))
//     this.creationTime = msg.CreationTime
//     this.lastAccessTime = msg.LastAccessTime
//     this.lastModifiedTime = msg.LastModifiedTime
//     // this.pathID = 'path:' + msg.Path
//     this.type = msg.Type
//     this.entryID = 'hip-id-' + this.name
//     if (this.type == 'Content') {
//         this.size = msg.Size
//     } else {
//         this.size = -1
//     }
//     this.lastModifiedTimeFormatted = formatDateTime(this.lastModifiedTime)
//     this.sizeFormatted = formatSize(this.size)
//
//     function formatSize(size) {
//         if (size < 0) return '--'
//         if (size < 1000) return size + ' B'
//         var mS = size
//         var me = 0
//         while (mS >= 10000) {
//             mS /= 10
//             me++
//         }
//         var validValue = parseInt(mS + 0.5)
//         var vv = validValue * Math.pow(10, me)
//         var unitPre = 0
//         while (vv >= 1000) {
//             vv /= 1000
//             unitPre++
//         }
//         var unitPres = ['B', 'K', 'M', 'G', 'T', 'P', 'E', 'Z', 'Y']
//         return vv + ' ' + unitPres[unitPre]
//     }
//
//     function formatDateTime(time) {
//         var date = new Date(time)
//         var fullDate = getFullDate(date)
//         var fullTime = getFullTime(date)
//         var currentDate = getFullDate(new Date())
//         return (currentDate == fullDate ? '' : (fullDate + ' ')) + fullTime
//     }
//
//     function fillToTwoDigits(num) {
//         var ns = '' + num
//         if (ns.length >= 2) return ns
//         else return '0' + ns
//     }
//
//     function getFullDate(date) {
//         return date.getFullYear() + '-' + fillToTwoDigits(date.getMonth() + 1) + '-' + fillToTwoDigits(date.getDate())
//     }
//
//     function getFullTime(date) {
//         return fillToTwoDigits(date.getHours()) + ':' + fillToTwoDigits(date.getMinutes()) + ':' + fillToTwoDigits(date.getSeconds())
//     }
//
//     this.contents = contents
//
//     function contents() {
//         var dotIndex = this.name.lastIndexOf(".")
//         var entryName = this.name.substring(0, dotIndex)
//         var type = this.name.substring(dotIndex + 1, this.name.length)
//         if (dotIndex == -1) {
//             entryName = this.name
//             type = "-"
//         }
//         return [entryName, type, this.lastModifiedTimeFormatted, this.sizeFormatted]
//     }
// }