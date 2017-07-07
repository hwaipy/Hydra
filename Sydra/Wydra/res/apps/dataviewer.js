$(document).ready(function () {
    $("#file-table").treetable({expandable: true, clickableNodeNames: true, onNodeExpand: nodeExpand})
    $("#file-table").on("mousedown", "tr", function () {
        if ($(this)[0].id != 'file-table-head') {
            // $(this).addClass("selected")
            selectInFileTable($(this)[0].getAttribute('data-tt-id'))
        }
    })
    $("#file-table").on("mousedown", "th", function () {
        tableSortingColumn = $(this)[0].getAttribute('column')
        tableSortingOrder = parseInt($(this)[0].getAttribute('order')) * (-1)
        $(this)[0].setAttribute('order', tableSortingOrder)
        updateSorting()
    })

    if (document.addEventListener) {
        document.addEventListener("keydown", keydown, false)
    } else if (document.attachEvent) {
        document.attachEvent("onkeydown", keydown)
    } else {
        document.onkeydown = keydown
    }

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
    var showPath = $.getUrlVars().path

    setInterval("refreshTrigger()", 1000)
    updatePathInformation("", function () {
        if (showPath != undefined) {
            var rs = routes(showPath)
            if (rs.length == 0) {
                selectInFileTable(showPath)
                // nodeLoadListeners.delete(listener)
                return
            }
            var listener = function (path) {
                if (rs.includes(path)) {
                    $("#file-table").treetable("expandNode", rs[rs.indexOf(path)])
                } else if (showPath == path) {
                    selectInFileTable(showPath)
                    // nodeLoadListeners.delete(listener)
                }
            }
            nodeLoadListeners.push(listener)
            $("#file-table").treetable("expandNode", rs[0])
        }
    })
    updateSorting()

});

function keydown(e) {
    if (e.which >= 37 && e.which <= 40 && $(".selected")[0] != undefined) {
        var code = e.code
        actionOnBranch($(".selected")[0], code)
    }
}

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
    var expandedPaths = getExpandedFolderPaths(document.getElementById('file-table').getElementsByTagName('tbody')[0])
    for (var i = 0; i < expandedPaths.length; i++) {
        updatePathInformation(expandedPaths[i])
    }
    updateSorting()
}

var tableSortingColumn = 0
var tableSortingOrder = 1
function sortFun(a, b) {
    var valA = document.getElementById('path:' + a.id).children[tableSortingColumn].getAttribute('value')
    var valB = document.getElementById('path:' + b.id).children[tableSortingColumn].getAttribute('value')
    if (tableSortingColumn > 0) {
        valA = parseInt(valA)
        valB = parseInt(valB)
        if (valA < valB) return -1 * tableSortingOrder
        if (valA > valB) return 1 * tableSortingOrder
    }
    valA = document.getElementById('path:' + a.id).children[0].getAttribute('value')
    valB = document.getElementById('path:' + b.id).children[0].getAttribute('value')
    if (valA < valB) return -1 * tableSortingOrder
    if (valA > valB) return 1 * tableSortingOrder
    return 0
}
function sortBeforeLoad(items) {
    var fun = function (a, b) {
        var valA = [a.creationTime, a.lastModifiedTime, a.size][tableSortingColumn]
        var valB = [b.creationTime, b.lastModifiedTime, b.size][tableSortingColumn]
        if (tableSortingColumn > 0) {
            valA = parseInt(valA)
            valB = parseInt(valB)
            if (valA < valB) return -1 * tableSortingOrder
            if (valA > valB) return 1 * tableSortingOrder
        }
        valA = a.path
        valB = b.path
        if (valA < valB) return -1 * tableSortingOrder
        if (valA > valB) return 1 * tableSortingOrder
        return 0
    }
    items.sort(fun)
}

function updateSorting() {
    var existTrs = document.getElementById('file-table').getElementsByTagName('tbody')[0].getElementsByTagName('tr')
    // $("#file-table").treetable("sortBranch", null, 2)
    for (var i = 0; i < existTrs.length; i++) {
        var path = existTrs[i].getAttribute('data-tt-id')
        var node = $("#file-table").treetable("node", path)
        $("#file-table").treetable("sortBranch", node, sortFun)
    }
}

function updatePathInformation(path, onFinish) {
    requestMessage({"Request": ["listElements", "", path, true], "To": "StorageService"}, function (msg) {
            msg = msg[0]
            if (typeof msg === "string") {
                console.warn(msg)
                return
            }
            var parentNode = (path == '' || path == '/') ? null : $("#file-table").treetable("node", path)
            var items = []
            for (var i = 0; i < msg.length; i++) {
                var fi = new FileItem(msg[i])
                items.push(fi)
            }
            sortBeforeLoad(items)
            var newTablePaths = []
            for (var i = 0; i < items.length; i++) {
                var fi = items[i]
                newTablePaths.push(fi.path)
            }
            var tableBody = document.getElementById('file-table').getElementsByTagName('tbody')[0]
            var existTablePaths = getExistChildPaths(tableBody, path)
            for (var i = 0; i < existTablePaths.length; i++) {
                existTablePath = existTablePaths[i]
                var keep = $.inArray(existTablePath, newTablePaths) >= 0
                if (!keep) {
                    $("#file-table").treetable("removeNode", existTablePath)
                }
            }
            for (var i = 0; i < newTablePaths.length; i++) {
                var newTablePath = newTablePaths[i]
                var index = $.inArray(newTablePath, existTablePaths)
                var item = items[i]
                if (index == -1) {
                    createTableRow('file-table', parentNode, item)
                } else {
                    var tr = document.getElementById(item.pathID)
                    updateTableRow(tr, item)
                }
            }
            if (onFinish != undefined) {
                onFinish()
            }
        }
    )
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

// function onClickTableRow(name) {
//     console.log('oc: ' + name)
//     summaryClient = name
//     doUpdateSummaryInformation()
// }
//
// var summaryClient = ""
// function doUpdateSummaryInformation() {
//     if (summaryClient != "") {
//         requestMessage({"Request": ["getSummary"], "To": summaryClient}, function (msg) {
//             msg = msg [0]
//             var converter = new showdown.Converter()
//             var md2html = converter.makeHtml(msg)
//             document.getElementById('summary').innerHTML = md2html
//         })
//     }
// }
//
function FileItem(msg) {
    this.name = msg.Name
    this.path = msg.Path
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
    this.itemType = 'Unknown'
    if (this.name.endsWith('.hip') && this.type == 'Collection') this.itemType = 'hip'
    else if (this.type == 'Collection') this.itemType = 'folder'
    else if (this.type == 'Content') this.itemType = 'file'

    this.creationTimeFormatted = formatDateTime(this.creationTime)
    this.lastModifiedTimeFormatted = formatDateTime(this.lastModifiedTime)
    this.lastAccessTimeFormatted = formatDateTime(this.lastAccessTime)
    this.sizeFormatted = formatSize(this.size)

    this.editAttributionTds = editAttributionTds
    function editAttributionTds(tds) {
        tds[0].setAttribute('value', this.creationTime)
        tds[0].innerHTML = this.creationTimeFormatted
        tds[1].setAttribute('value', this.lastModifiedTime)
        tds[1].innerHTML = this.lastModifiedTimeFormatted
        tds[2].setAttribute('value', this.size)
        tds[2].innerHTML = this.sizeFormatted
        return tds
    }

    function formatSize(size) {
        if (size < 0)return '--'
        if (size < 1000)return size + ' B'
        var mS = size
        var me = 0
        while (mS >= 10000) {
            mS /= 10
            me++
        }
        var validValue = parseInt(mS + 0.5)
        var vv = validValue * Math.pow(10, me)
        var unitPre = 0
        while (vv >= 1000) {
            vv /= 1000
            unitPre++
        }
        var unitPres = ['B', 'K', 'M', 'G', 'T', 'P', 'E', 'Z', 'Y']
        return vv + ' ' + unitPres[unitPre]
    }

    function formatDateTime(time) {
        var date = new Date(time)
        var fullDate = getFullDate(date)
        var fullTime = getFullTime(date)
        var currentDate = getFullDate(new Date())
        return (currentDate == fullDate ? '' : (fullDate + ' ')) + fullTime
    }

    function fillToTwoDigits(num) {
        var ns = '' + num
        if (ns.length >= 2)return ns
        else return '0' + ns
    }

    function getFullDate(date) {
        return date.getFullYear() + '-' + fillToTwoDigits(date.getMonth() + 1) + '-' + fillToTwoDigits(date.getDate())
    }

    function getFullTime(date) {
        return fillToTwoDigits(date.getHours()) + ':' + fillToTwoDigits(date.getMinutes()) + ':' + fillToTwoDigits(date.getSeconds())
    }
}

function createTableRow(tableID, parentNode, item) {
    var itemType = item.itemType
    var itemName = item.name
    var tr = document.createElement('tr')
    tr.setAttribute('id', item.pathID)
    tr.setAttribute('data-tt-id', item.path)
    tr.setAttribute('before-fresh-added', '1')
    tr.setAttribute('type', itemType)
    if (item.parentPath.length > 0) {
        tr.setAttribute('data-tt-parent-id', item.parentPath)
    }
    var td = document.createElement('td')
    var span = document.createElement('span')
    span.appendChild(document.createTextNode(itemName))
    span.setAttribute('class', itemType)
    td.appendChild(span)
    td.setAttribute('value', item.path)
    tr.appendChild(td)
    var tds = [document.createElement('td'), document.createElement('td'), document.createElement('td')]
    item.editAttributionTds(tds)
    for (var j = 0; j < tds.length; j++) {
        tr.appendChild(tds[j])
    }
    $("#" + tableID).treetable("loadBranch", parentNode, tr)

    if (itemType == 'folder') {
        trF = document.createElement('tr')
        trF.setAttribute('id', 'LazyContent:' + item.pathID)
        trF.setAttribute('data-tt-id', 'LazyContent:' + item.path)
        trF.setAttribute('data-tt-parent-id', item.path)
        var td = document.createElement('td')
        var span = document.createElement('span')
        span.appendChild(document.createTextNode('Loading'))
        span.setAttribute('class', 'load')
        td.appendChild(span)
        trF.appendChild(td)
        for (var j = 0; j < tds.length; j++) {
            var td = document.createElement('td')
            var text = document.createTextNode('')
            td.appendChild(text)
            trF.appendChild(td)
        }
        $("#" + tableID).treetable("loadBranch", $("#file-table").treetable("node", item.path), trF)
        $("#" + tableID).treetable("collapseNode", item.path)
        tr.removeAttribute('before-fresh-added')
    }
    onNodeLoad(item.path)
}

function updateTableRow(tableRow, item) {
    var tds = Array.from(tableRow.getElementsByTagName('td')).slice(1, 4)
    // var attributions = item.attributions()
    // for (var j = 1; j < tds.length; j++) {
    //     tds[j].innerHTML = attributions[j - 1]
    // }
    item.editAttributionTds(tds)
}

function nodeExpand(a) {
    var expendedNodeId = $(this.row).data("tt-id")
    var tr = $(this.row)[0]
    if (tr.getAttribute('before-fresh-added') != 1) {
        updatePathInformation(tr.getAttribute('data-tt-id'))
    }
    updateSorting()
}

function getExistChildPaths(tableBody, parentPath) {
    var existTrs = tableBody.getElementsByTagName('tr')
    var existTablePaths = []
    for (var i = 0; i < existTrs.length; i++) {
        var parentTtId = existTrs[i].getAttribute('data-tt-parent-id')
        if (((parentPath == '' || parentPath == '/') && parentTtId == null) || (parentTtId == parentPath)) {
            existTablePaths.push(existTrs[i].getAttribute('data-tt-id'))
        }
    }
    return existTablePaths
}

function getExistValidPaths(tableBody) {
    var existTrs = tableBody.getElementsByTagName('tr')
    var existTablePaths = []
    for (var i = 0; i < existTrs.length; i++) {
        var parentTtId = existTrs[i].getAttribute('data-tt-parent-id')
        if (!existTrs[i].getAttribute('id').startsWith('LazyContent:')) {
            existTablePaths.push(existTrs[i].getAttribute('data-tt-id'))
        }
    }
    return existTablePaths
}

function getExpandedFolderPaths(tableBody) {
    var existTrs = tableBody.getElementsByTagName('tr')
    var expendedPaths = []
    for (var i = 0; i < existTrs.length; i++) {
        var tr = existTrs[i]
        var clazz = tr.getAttribute('class')
        if (clazz.includes('expanded')) {
            expendedPaths.push(tr.getAttribute('data-tt-id'))
        }
    }
    return expendedPaths
}

function actionOnBranch(tr, cmd) {
    var expandable = tr.getAttribute('type') == 'folder'
    var path = tr.getAttribute('data-tt-id')
    var parentPath = tr.getAttribute('data-tt-parent-id')
    var expanded = tr.getAttribute('class').includes('expanded')
    if (cmd == 'ArrowRight') {
        if (expandable) {
            $("#file-table").treetable("expandNode", path)
        }
    } else if (cmd == 'ArrowLeft') {
        if (expanded) {
            $("#file-table").treetable("collapseNode", path)
        } else {
            $("#file-table").treetable("collapseNode", parentPath)
            selectInFileTable(parentPath)
        }
    } else if (cmd == 'ArrowUp') {
        selectNextVisibleRow(path, true)
    } else if (cmd == 'ArrowDown') {
        selectNextVisibleRow(path, false)
    }
}

function selectNextVisibleRow(currentPath, reverse) {
    var tableBody = document.getElementById('file-table').getElementsByTagName('tbody')[0]
    var validPaths = getExistValidPaths(tableBody)
    var visiblePaths = []
    for (var i = 0; i < validPaths.length; i++) {
        var p = validPaths[i]
        var node = $("#file-table").treetable("node", p)
        if (isVisible(node)) {
            visiblePaths.push(p)
        }
    }
    var preIndex = visiblePaths.indexOf(currentPath)
    var newIndex = preIndex + (reverse ? -1 : 1)
    if (newIndex < 0) newIndex = 0
    if (newIndex >= visiblePaths.length) newIndex = visiblePaths.length - 1
    console.log('new idnex is ' + newIndex)
    if (newIndex == preIndex) return
    selectInFileTable(visiblePaths[newIndex])
}

function isVisible(node) {
    while (true) {
        node = node.parentNode()
        if (node == null)return true
        if (node.collapsed())return false
    }
}

function routes(path) {
    function parents(p, stack) {
        var index = p.lastIndexOf('/')
        var pp = p.substr(0, index)
        if (pp.length == 0)return
        else {
            stack.push(pp)
            parents(pp, stack)
        }
    }

    var rs = []
    parents(path, rs)
    rs.reverse()
    return rs
}

var nodeLoadListeners = []
function onNodeLoad(path) {
    for (var i = 0; i < nodeLoadListeners.length; i++) {
        nodeLoadListeners[i](path)
    }
}

function selectInFileTable(path) {
    $(".selected").removeClass("selected");
    var tr = document.getElementById("path:" + path)
    tr.setAttribute('class', tr.getAttribute('class') + ' selected')
    updateHipTableEntryList(path)
}

function updateHipTableEntryList(selectedPath) {
    var tr = document.getElementById('path:' + selectedPath)
    if (tr.getAttribute('type') == 'hip') {
        console.log('Need a native mathod to get hip informations.')
        doUpdateHipTableEntryList(selectedPath)
    }
}

function doUpdateHipTableEntryList(path, onFinish) {
    requestMessage({"Request": ["listElements", "", path, true], "To": "StorageService"}, function (msg) {
            msg = msg[0]
            if (typeof msg === "string") {
                console.warn(msg)
                return
            }
            var items = []
            for (var i = 0; i < msg.length; i++) {
                var fi = new HipEntry(msg[i])
                items.push(fi)
            }
            // console.log(items)
            // sortBeforeLoad(items)
            // var newTablePaths = []
            // for (var i = 0; i < items.length; i++) {
            //     var fi = items[i]
            //     newTablePaths.push(fi.path)
            // }
            // var tableBody = document.getElementById('file-table').getElementsByTagName('tbody')[0]
            // var existTablePaths = getExistChildPaths(tableBody, path)
            // for (var i = 0; i < existTablePaths.length; i++) {
            //     existTablePath = existTablePaths[i]
            //     var keep = $.inArray(existTablePath, newTablePaths) >= 0
            //     if (!keep) {
            //         $("#file-table").treetable("removeNode", existTablePath)
            //     }
            // }
            // for (var i = 0; i < newTablePaths.length; i++) {
            //     var newTablePath = newTablePaths[i]
            //     var index = $.inArray(newTablePath, existTablePaths)
            //     var item = items[i]
            //     if (index == -1) {
            //         createTableRow('file-table', parentNode, item)
            //     } else {
            //         var tr = document.getElementById(item.pathID)
            //         updateTableRow(tr, item)
            //     }
            // }
            // if (onFinish != undefined) {
            //     onFinish()
            // }
        }
    )
}

function selectInHipTable() {
}

function HipEntry(msg) {
    console.log(msg)
    this.name = msg.Name
    this.path = msg.Path
    this.parentPath = this.path.substring(0, this.path.lastIndexOf('/'))
    this.creationTime = msg.CreationTime
    this.lastAccessTime = msg.LastAccessTime
    this.lastModifiedTime = msg.LastModifiedTime
    // this.pathID = 'path:' + msg.Path
    this.type = msg.Type
    // if (this.type == 'Content') {
    //     this.size = msg.Size
    // } else {
    //     this.size = -1
    // }
    // this.itemType = 'Unknown'
    // if (this.name.endsWith('.hip') && this.type == 'Collection') this.itemType = 'hip'
    // else if (this.type == 'Collection') this.itemType = 'folder'
    // else if (this.type == 'Content') this.itemType = 'file'
    //
    // this.creationTimeFormatted = formatDateTime(this.creationTime)
    // this.lastModifiedTimeFormatted = formatDateTime(this.lastModifiedTime)
    // this.lastAccessTimeFormatted = formatDateTime(this.lastAccessTime)
    // this.sizeFormatted = formatSize(this.size)
    //
    // this.editAttributionTds = editAttributionTds
    // function editAttributionTds(tds) {
    //     tds[0].setAttribute('value', this.creationTime)
    //     tds[0].innerHTML = this.creationTimeFormatted
    //     tds[1].setAttribute('value', this.lastModifiedTime)
    //     tds[1].innerHTML = this.lastModifiedTimeFormatted
    //     tds[2].setAttribute('value', this.size)
    //     tds[2].innerHTML = this.sizeFormatted
    //     return tds
    // }
    //
    // function formatSize(size) {
    //     if (size < 0)return '--'
    //     if (size < 1000)return size + ' B'
    //     var mS = size
    //     var me = 0
    //     while (mS >= 10000) {
    //         mS /= 10
    //         me++
    //     }
    //     var validValue = parseInt(mS + 0.5)
    //     var vv = validValue * Math.pow(10, me)
    //     var unitPre = 0
    //     while (vv >= 1000) {
    //         vv /= 1000
    //         unitPre++
    //     }
    //     var unitPres = ['B', 'K', 'M', 'G', 'T', 'P', 'E', 'Z', 'Y']
    //     return vv + ' ' + unitPres[unitPre]
    // }
    //
    // function formatDateTime(time) {
    //     var date = new Date(time)
    //     var fullDate = getFullDate(date)
    //     var fullTime = getFullTime(date)
    //     var currentDate = getFullDate(new Date())
    //     return (currentDate == fullDate ? '' : (fullDate + ' ')) + fullTime
    // }
    //
    // function fillToTwoDigits(num) {
    //     var ns = '' + num
    //     if (ns.length >= 2)return ns
    //     else return '0' + ns
    // }
    //
    // function getFullDate(date) {
    //     return date.getFullYear() + '-' + fillToTwoDigits(date.getMonth() + 1) + '-' + fillToTwoDigits(date.getDate())
    // }
    //
    // function getFullTime(date) {
    //     return fillToTwoDigits(date.getHours()) + ':' + fillToTwoDigits(date.getMinutes()) + ':' + fillToTwoDigits(date.getSeconds())
    // }
}