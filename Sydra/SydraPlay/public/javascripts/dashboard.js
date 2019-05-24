var table

$(document).ready(function () {
  table = $('#ServiceListTable').DataTable({
    "columnDefs": [ {
        "targets": 1,
        "data": function ( row, type, val, meta ) {
          var dt = moment.duration(row[1], 'ms')
          if (dt.get('days') > 1) return dt.get('days') + ' days'
          if (dt.get('days') > 0) return dt.get('days') + ' day'
          if (dt.get('hours') > 1) return dt.get('hours') + ' hours'
          if (dt.get('hours') > 0) return dt.get('hours') + ' hour'
          if (dt.get('minutes') > 1) return dt.get('minutes') + ' minutes'
          if (dt.get('minutes') > 0) return dt.get('minutes') + ' minute'
          if (dt.get('seconds') > 1) return dt.get('seconds') + ' seconds'
          return dt.get('seconds') + ' second'
        }
      } ]
    });
  session = HttpSession.create('/message', function () {
    fetchServiceList()
    window.setInterval(fetchServiceList, 2000)
  })
  testService()
});

function fetchServiceList(){
  session.getServiceListWithMeta().onSuccess(function (response) {
    updateServiceList(response)
  })
}

function updateServiceList(response){
  console.log(response);
  var newNames = []
  for (var i = 0; i < response.length; i++) {
    newNames.push(response[i][0])
  }

  for (var i = 0; i < table.data().length; i++) {
    var existName = table.data()[i][0]
    if(!newNames.includes(existName)){
      table.row(i).remove()
      i--
    }
  }

  var existNames = []
  for (var i = 0; i < table.data().length; i++) {
    existNames.push(table.data()[i][0])
  }
  for (var i = 0; i < response.length; i++) {
    entry = response[i]
    newName = entry[0]
    if(existNames.includes(newName)){
      var row = table.row(i)
      var d = row.data()
      d[1] = entry[1]
      d[2] = entry[2]
      row.invalidate()
    }else{
      table.row.add([entry[0], entry[1], entry[2], 'X'])
    }
  }

  table.draw(false);
}

function testService() {
    class Target {
        constructor(name) {
            this.name = name
        }

        fetch(pre, post) {
            return `${pre}-${this.name}-${post}`
        }
    }

    service = HttpSession.create('/message', function () {
    }, new Target('target1'), 'target1')
    service2 = HttpSession.create('/message', function () {
    }, new Target('target2'), 'target2')
    // client = HttpSession.create('/message', function () {
    //     client.target1.fetchNo().onSuccess(function (response) {
    //         console.log(response)
    //     }).onFailure(function (error) {
    //         console.log(error)
    //     })
    // })

    // window.setInterval(function () {
    //     client.target1.fetch("{[(", ")]}").onSuccess(function (response) {
    //         console.log(response)
    //     })
    // }, 3000)

    window.onbeforeunload = function () {
        service.unregisterAsService()
        service2.unregisterAsService()
        return true
    }

}
