function TreeTable(tableID, treetableParas, onNodeExpand, onNodeSelect, dataFormatters, defaultSelectionRoute) {
    treetableParas.onNodeExpand = nodeExpand
    treetableParas.onSelect = function (e) {
        console.log(e)
    }
    $("#" + tableID).treetable(treetableParas)

    //Rows is a list of ROWs, with following functions and fields:
    //      id: each row should have an unique id
    //      parentId: for root elements, the parentId should be an empty string
    //      expandable: indicate if any sub element may exists
    //      rowType: indicate the type. Typically, a corresponding icon will be shown in the first column
    //      values: values that need to be displayed
    this.updatePathInformation = updatePathInformation

    function updatePathInformation(path, rows) {
        var newTablePaths = []
        for (var i = 0; i < rows.length; i++) {
            newTablePaths.push(rows[i].id)
        }
        var tableBody = document.getElementById(tableID).getElementsByTagName('tbody')[0]
        var oldTablePaths = getExistChildPaths(path)
        for (var i = 0; i < oldTablePaths.length; i++) {
            var oldTablePath = oldTablePaths[i]
            var keep = $.inArray(oldTablePath, newTablePaths) >= 0
            if (!keep) {
                $("#" + tableID).treetable("removeNode", oldTablePath)
            }
        }
        for (var i = 0; i < rows.length; i++) {
            var row = rows[i]
            var newTablePath = row.id
            var index = $.inArray(newTablePath, oldTablePaths)
            if (index == -1) {
                var tr = document.createElement('tr')
                tr.setAttribute('id', tableID + ' TreeTableRow ' + row.id)
                tr.setAttribute('data-tt-id', row.id)
                tr.setAttribute('data-tt-parent-id', row.parentId)
                var values = row.values

                var span = document.createElement('span')
                span.innerHTML = formatData(values[0], 0)
                span.setAttribute('class', row.itemType)
                span.setAttribute('id', tableID + ' TreeTableCellDisplay(' + 0 + ') ' + row.id)
                var td = document.createElement('td')
                td.appendChild(span)
                td.setAttribute('id', tableID + ' TreeTableCell(' + 0 + ') ' + row.id)
                td.setAttribute('data-tt-value', values[0])
                tr.appendChild(td)
                for (var j = 1; j < values.length; j++) {
                    var td = document.createElement('td')
                    td.innerHTML = formatData(values[j], j)
                    td.setAttribute('id', tableID + ' TreeTableCell(' + j + ') ' + row.id)
                    td.setAttribute('data-tt-value', values[j])
                    tr.appendChild(td)
                }
                $("#" + tableID).treetable("loadBranch", $("#" + tableID).treetable("node", row.parentId), tr)
            } else {
                tr = $("#" + tableID).treetable("node", row.id).row[0]
                var values = row.values

                var td = document.getElementById(tableID + ' TreeTableCell(' + 0 + ') ' + row.id)
                td.setAttribute('data-tt-value', values[0])
                var span = document.getElementById(tableID + ' TreeTableCellDisplay(' + 0 + ') ' + row.id)
                span.innerHTML = formatData(values[0], 0)
                for (var j = 1; j < values.length; j++) {
                    var td = document.getElementById(tableID + ' TreeTableCell(' + j + ') ' + row.id)
                    td.innerHTML = formatData(values[j], j)
                }
            }

            if (row.expandable) {
                tr.setAttribute('data-tt-expandable', 'true')
                var existNode = $("#" + tableID).treetable("node", 'LazyContent:' + row.id)
                if (existNode == undefined && getExistChildPaths(newTablePath).length == 0) {
                    trF = document.createElement('tr')
                    trF.setAttribute('id', 'LazyContent:' + row.id)
                    trF.setAttribute('data-tt-id', 'LazyContent:' + row.id)
                    trF.setAttribute('data-tt-parent-id', row.id)
                    var td = document.createElement('td')
                    var span = document.createElement('span')
                    span.appendChild(document.createTextNode('Loading...'))
                    span.setAttribute('class', 'load')
                    td.appendChild(span)
                    trF.appendChild(td)
                    var tdCount = row.numberOfColumn
                    for (var j = 0; j < tdCount - 1; j++) {
                        var td = document.createElement('td')
                        var text = document.createTextNode('')
                        td.appendChild(text)
                        trF.appendChild(td)
                    }
                    tr.setAttribute('Indicate-On-Creating-Loading-Fake-Node', 'true')
                    $("#" + tableID).treetable("loadBranch", $("#" + tableID).treetable("node", row.id), trF)
                    $("#" + tableID).treetable("collapseNode", row.id)
                    tr.removeAttribute('Indicate-On-Creating-Loading-Fake-Node')
                }
            }
        }
        updateSorting()
        if (defaultSelectionRoute != null && defaultSelectionRoute != undefined && defaultSelectionRoute.length > 0) {
            for (var k = 0; k < rows.length; k++) {
                var row = rows[k]
                var index = $.inArray(row.id, defaultSelectionRoute)
                if (index == defaultSelectionRoute.length - 1) {
                    select(row.id)
                    defaultSelectionRoute = []
                    break
                } else if (index >= 0) {
                    expand(row.id)
                }
            }
        }
    }

    function getExistChildPaths(parentPath) {
        var tableBody = document.getElementById(tableID).getElementsByTagName('tbody')[0]
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

    function nodeExpand(a) {
        var tr = $(this.row)[0]
        if (tr.getAttribute('Indicate-On-Creating-Loading-Fake-Node') != 'true') {
            onNodeExpand(tr.getAttribute('data-tt-id'))
        }
    }

    this.getNode = getNode

    function getNode(id) {
        return $('#' + tableID).treetable('node', id)
    }

    this.getExpandedExpandableTrs = getExpandedExpandableTrs

    function getExpandedExpandableTrs() {
        var tableBody = document.getElementById(tableID).getElementsByTagName('tbody')[0]
        var existTrs = tableBody.getElementsByTagName('tr')
        var expendedExpandableTrs = []
        for (var i = 0; i < existTrs.length; i++) {
            var tr = existTrs[i]
            if (tr.getAttribute('class').includes('expanded') && tr.getAttribute('data-tt-expandable') == 'true') {
                expendedExpandableTrs.push(tr)
            }
        }
        return expendedExpandableTrs
    }

    function updateSorting() {
        var tableBody = document.getElementById(tableID).getElementsByTagName('tbody')[0]
        var existTrs = tableBody.getElementsByTagName('tr')
        for (var i = 0; i < existTrs.length; i++) {
            var path = existTrs[i].getAttribute('data-tt-id')
            var node = $("#" + tableID).treetable("node", path)
            $("#" + tableID).treetable("sortBranch", node, sortFun)
        }
    }

    $("#" + tableID).on("mousedown", "th", function () {
        tableSortingColumn = $(this)[0].getAttribute('column')
        tableSortingOrder = tableSortingOrder * (-1)
        updateSorting()
    })

    var tableSortingColumn = 0
    var tableSortingOrder = 1

    function sortFun(a, b) {
        var valA = listSortingValues(a)
        var valB = listSortingValues(b)
        return sortList(valA, valB) * tableSortingOrder
    }

    function sortList(listA, listB) {
        for (var i = 0; i < listA.length; i++) {
            if (listA[i] > listB[i]) return 1
            if (listA[i] < listB[i]) return -1
        }
        return 0
    }

    function listSortingValues(node) {
        var tr = node.row[0]
        var tds = tr.getElementsByTagName('td')
        var values = [parseValue(tds[tableSortingColumn].getAttribute('data-tt-value'))]
        for (var i = 0; i < tds.length; i++) {
            values.push(parseValue(tds[i].getAttribute('data-tt-value')))
        }
        return values
    }

    function parseValue(v) {
        var number = new Number(v)
        if (number.toString() == 'NaN') return v
        return number.valueOf()
    }

    $("#" + tableID).on("mousedown", "tr", function () {
        if ($(this)[0].id != 'file-table-head') {
            selectRow($(this)[0])
        }
    })

    if (document.addEventListener) {
        document.addEventListener("keydown", keydown, false)
    } else if (document.attachEvent) {
        document.attachEvent("onkeydown", keydown)
    } else {
        document.onkeydown = keydown
    }

    function keydown(e) {
        if (e.which >= 37 && e.which <= 40) {
            var code = e.code
            actionOnBranch($(".selected")[0], code)
        }
    }

    function selectRow(tr) {
        var previousSelection = $(".selected")
        $(".selected").removeClass("selected")
        tr.setAttribute('class', tr.getAttribute('class') + ' selected')

        var pre = null
        if (previousSelection != null && previousSelection != undefined) {
            previousSelection = previousSelection[0]
            if (previousSelection != null && previousSelection != undefined && previousSelection.__proto__ == HTMLTableRowElement.prototype) {
                pre = previousSelection.getAttribute('data-tt-id')
            }
        }
        onNodeSelect(tr.getAttribute('data-tt-id'), pre)
    }

    this.select = select

    function select(id) {
        var node = $('#' + tableID).treetable('node', id)
        if (node != undefined) selectRow(node.row[0])
    }

    function actionOnBranch(tr, cmd) {
        if (tr == undefined) {
            if (cmd == 'ArrowUp') {
                selectNextVisibleRow(tr, true)
            } else if (cmd == 'ArrowDown') {
                selectNextVisibleRow(tr, false)
            }
            return
        }
        var expandable = getExistChildPaths(tr.getAttribute('data-tt-id')).length > 0
        var path = tr.getAttribute('data-tt-id')
        var parentPath = tr.getAttribute('data-tt-parent-id')
        var parentNode = $("#" + tableID).treetable("node", parentPath)
        var parentTr = parentNode == undefined ? undefined : $("#" + tableID).treetable("node", parentPath).row[0]
        var expanded = tr.getAttribute('class').includes('expanded')
        if (cmd == 'ArrowRight') {
            if (expandable) {
                expand(path)
            }
        } else if (cmd == 'ArrowLeft') {
            if (expanded) {
                collapse(path)
            } else {
                if (parentPath != null && parentPath != '') {
                    collapse(parentPath)
                    selectRow(parentTr)
                }
            }
        } else if (cmd == 'ArrowUp') {
            selectNextVisibleRow(tr, true)
        } else if (cmd == 'ArrowDown') {
            selectNextVisibleRow(tr, false)
        }
    }

    this.expand = expand
    this.collapse = collapse

    function expand(id) {
        $("#" + tableID).treetable("expandNode", id)
    }

    function collapse(id) {
        $("#file-table").treetable("collapseNode", id)
    }

    function selectNextVisibleRow(currentTr, reverse) {
        var tableBody = document.getElementById(tableID).getElementsByTagName('tbody')[0]
        var validTrs = getExistValidTrs()
        var visibleTrs = []
        for (var i = 0; i < validTrs.length; i++) {
            var tr = validTrs[i]
            var node = $("#" + tableID).treetable("node", tr.getAttribute('data-tt-id'))
            if (isNodeVisible(node)) {
                visibleTrs.push(tr)
            }
        }
        var preIndex = visibleTrs.indexOf(currentTr)
        var newIndex = preIndex + (reverse ? -1 : 1)
        if (newIndex < 0) newIndex = 0
        if (newIndex >= visibleTrs.length) newIndex = visibleTrs.length - 1
        if (newIndex == preIndex) return
        selectRow(visibleTrs[newIndex])
    }

    function getExistValidTrs() {
        var tableBody = document.getElementById(tableID).getElementsByTagName('tbody')[0]
        var existTrs = tableBody.getElementsByTagName('tr')
        var existTableTrs = []
        for (var i = 0; i < existTrs.length; i++) {
            if (!existTrs[i].getAttribute('id').startsWith('LazyContent:')) {
                existTableTrs.push(existTrs[i])
            }
        }
        return existTableTrs
    }

    function isNodeVisible(node) {
        while (true) {
            node = node.parentNode()
            if (node == null) return true
            if (node.collapsed()) return false
        }
    }

    function formatData(value, index) {
        var formatter = dataFormatters[index]
        if (formatter == null || formatter == undefined) {
            formatter = function (v) {
                return v
            }
        }
        return formatter(value)
    }

    onNodeExpand('')
}

//TODO root elements should be sortable

// function sortBeforeLoad(items) {
//     var fun = function (a, b) {
//         var valA = [a.creationTime, a.lastModifiedTime, a.size][tableSortingColumn]
//         var valB = [b.creationTime, b.lastModifiedTime, b.size][tableSortingColumn]
//         if (tableSortingColumn > 0) {
//             valA = parseInt(valA)
//             valB = parseInt(valB)
//             if (valA < valB) return -1 * tableSortingOrder
//             if (valA > valB) return 1 * tableSortingOrder
//         }
//         valA = a.path
//         valB = b.path
//         if (valA < valB) return -1 * tableSortingOrder
//         if (valA > valB) return 1 * tableSortingOrder
//         return 0
//     }
//     items.sort(fun)
// }