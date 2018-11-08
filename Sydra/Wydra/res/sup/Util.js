function toArray(buffer) {
    return Array.prototype.slice.call(buffer);
}

function formatSize(size) {
    if (size < 0) return '--'
    if (size < 1000) return size + ' B'
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

function getFullDate(date) {
    return date.getFullYear() + '-' + fillToTwoDigits(date.getMonth() + 1) + '-' + fillToTwoDigits(date.getDate())
}

function getFullTime(date) {
    return fillToTwoDigits(date.getHours()) + ':' + fillToTwoDigits(date.getMinutes()) + ':' + fillToTwoDigits(date.getSeconds())
}

function fillToTwoDigits(num) {
    var ns = '' + num
    if (ns.length >= 2) return ns
    else return '0' + ns
}

if (!String.prototype.endsWith)
    String.prototype.endsWith = function (searchStr, Position) {
        // This works much better than >= because
        // it compensates for NaN:
        if (!(Position < this.length))
            Position = this.length;
        else
            Position |= 0; // round position
        return this.substr(Position - searchStr.length,
            searchStr.length) === searchStr;
    };