function SortedTable(tableID, tablesorterParas, onClickOfCell) {
    $("#" + tableID).tablesorter(tablesorterParas)

    this.updateTrs = updateTrs

    function updateTrs(newTrs) {
        var table = document.getElementById('client-table').getElementsByTagName('tbody')[0]
        var oldTrs = table.getElementsByTagName('tr')
        while (oldTrs.length > 0) {
            table.removeChild(oldTrs[0])
        }
        for (var i = 0; i < newTrs.length; i++) {
            table.appendChild(newTrs[i])
            $('tr#' + newTrs[i].getAttribute('id') + ' td').click(function (e) {
                onClickOfCell(e)
            })
        }
        $(".tablesorter").trigger("update")
    }
}