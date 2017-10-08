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
    }, updatePathInformation, [null, formatDateTime, formatDateTime, formatSize], rs)

    setInterval("refreshTrigger()", 1000)
})

var refreshCount = 0
var refreshPeriod = 3

function refreshTrigger() {
    refreshCount++
    if (refreshCount >= refreshPeriod) {
        refreshCount = 0
        updateFilesInformation()
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
//             var parentNode = (path == '' || path == '/') ? null : $("#file-table").treetable("node", path)
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

//
// function updateHipTableEntryList(selectedPath) {
//     var tr = document.getElementById('path:' + selectedPath)
//     if (tr.getAttribute('type') == 'hip') {
//         doUpdateHipTableEntryList(selectedPath)
//     } else {
//         var table = document.getElementById('hip-table').getElementsByTagName('tbody')[0]
//         var trs = table.getElementsByTagName('tr')
//         var trsLength = trs.length
//         for (var i = 0; i < trsLength; i++) {
//             table.removeChild(trs[0])
//         }
//         document.getElementById('HipEntryNote').innerHTML = ''
//     }
// }
//
// function doUpdateHipTableEntryList(path, onFinish) {
//     requestMessage({"Request": ["getHipInformation", "", path, true], "To": "StorageService"}, function (msg) {
//             msg = msg[0]
//             if (typeof msg === "string") {
//                 console.warn(msg)
//                 return
//             }
//             var items = msg.items
//             var hipEntries = [], newTableIDs = []
//             var note = msg.note
//             for (var i = 0; i < items.length; i++) {
//                 var fi = new HipEntry(items[i])
//                 if (fi.type == 'Content') {
//                     hipEntries.push(fi)
//                     newTableIDs.push(fi.entryID)
//                 }
//             }
//             var table = document.getElementById('hip-table').getElementsByTagName('tbody')[0]
//             var trs = table.getElementsByTagName('tr')
//             var existTableIDs = []
//             var existTableRows = []
//             for (var i = 0; i < trs.length; i++) {
//                 existTableIDs.push(trs[i].getAttribute('id'))
//                 existTableRows.push(trs[i])
//             }
//             for (var i = 0; i < existTableIDs.length; i++) {
//                 existTableID = existTableIDs[i]
//                 var keep = $.inArray(existTableID, newTableIDs) >= 0
//                 if (!keep) {
//                     table.removeChild(existTableRows[i])
//                 }
//             }
//             for (var i = 0; i < newTableIDs.length; i++) {
//                 var newTableID = newTableIDs[i]
//                 var index = $.inArray(newTableID, existTableIDs)
//                 var entry = hipEntries[i]
//                 var contents = entry.contents()
//                 if (index == -1) {
//                     var tr = document.createElement('tr')
//                     tr.setAttribute('id', entry.entryID)
//                     table.appendChild(tr)
//                     for (var j = 0; j < contents.length; j++) {
//                         var td = document.createElement('td')
//                         var text = document.createTextNode(contents[j])
//                         td.appendChild(text)
//                         tr.appendChild(td)
//                     }
//                     var onClockUtilFunction = function (name) {
//                         return function () {
//                             onClickTableRow(name)
//                         }
//                     }
//                     $('#' + entry.entryID).click(onClockUtilFunction(entry.name))
//                 } else {
//                     var tr = document.getElementById(entry.entryID)
//                     var tds = tr.getElementsByTagName('td')
//                     for (var j = 0; j < tds.length; j++) {
//                         tds[j].innerHTML = contents[j]
//                     }
//                 }
//             }
//             $(".tablesorter").trigger("update")
//             document.getElementById('HipEntryNote').innerHTML = note
//         }
//     )
// }
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