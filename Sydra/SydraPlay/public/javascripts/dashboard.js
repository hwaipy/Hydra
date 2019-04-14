$(document).ready(function () {
    session = HttpSession.create('/hydra/message', function () {
        console.log('ready')
        session.getServiceList().onSuccess(function (response) {
            console.log(response)
        }).onFailure(function (error) {
            console.log(error)
        })
        session.getServiceList1().onSuccess(function (response) {
            console.log(response)
        })
    })
});