$(document).ready(function () {
    session = HttpSession.create('/hydra/message', function () {
        window.setInterval(function () {
            session.getServiceListWithMeta().onSuccess(function (response) {
                console.log(response)
                text = ""
                for (serviceName of response) {
                    text += `<p>${serviceName}</p>`
                }
                $("#serviceList").html(text)
            })
        }, 5000)
    })
    testService()
});

function testService() {
    class Target {
        constructor(name) {
            this.name = name
        }

        fetch(pre, post) {
            return `${pre}-${this.name}-${post}`
        }
    }

    service = HttpSession.create('/hydra/message', function () {
    }, new Target('target1'), 'target1')
    client = HttpSession.create('/hydra/message', function () {
        client.target1.fetchNo().onSuccess(function (response) {
            console.log(response)
        }).onFailure(function (error) {
            console.log(error)
        })
    })

    window.setInterval(function () {
        client.target1.fetch("{[(", ")]}").onSuccess(function (response) {
            console.log(response)
        })
    }, 3000)

    window.onbeforeunload = function () {
        service.unregisterAsService()
        client.unregisterAsService()
        return true
    }

}