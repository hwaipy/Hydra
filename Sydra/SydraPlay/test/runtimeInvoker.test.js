var IF = require('../public/javascripts/interactionfree.js');
var assert = require('assert');


class InvokerTestClass {
    constructor() {
        this.noParam = "No Param"
    }

    run(a3 = 100, b3 = 1.0, c3, d3) {
        return `Method:${a3},${b3},${c3},${d3}`
    }


    func(a1, a2, a3 = 0.1, a4 = 1, a5 = false) {
        return `FUNC:${a1},${a2},${a3},${a4},${a5}`
    }
}

describe('runtime invoker test', function () {
    invoker = new InvokerTestClass()
    runtimeInvoker = new IF.RuntimeInovker(invoker)

    it('Test Method', function () {
        assert.throws(function () {
            runtimeInvoker.invoke("run1")
        }, IF.ProtocolException)
        assert.deepEqual(runtimeInvoker.invoke("run", []), "Method:100,1,undefined,undefined")
        assert.deepEqual(runtimeInvoker.invoke("run", [1, 2, 3]), "Method:1,2,3,undefined")
        assert.deepEqual(runtimeInvoker.invoke("run", [1, 2, 3, 4]), "Method:1,2,3,4")
        assert.deepEqual(runtimeInvoker.invoke("run", [1, 2, 3, 4, 5]), "Method:1,2,3,4")
        assert.deepEqual(runtimeInvoker.invoke("noParam", []), 'No Param')
        assert.deepEqual(runtimeInvoker.invoke("noParam", [1, 2, 3, 4, 5]), 'No Param')
    })
});