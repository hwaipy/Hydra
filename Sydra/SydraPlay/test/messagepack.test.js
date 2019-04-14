var IF = require('../public/javascripts/interactionfree.js');
var assert = require('assert');

mapIn = {
    "keyString": "value1",
    "keyInt": 123,
    "keyLong": (Number.MAX_SAFE_INTEGER + 100),
    "keyBigInteger": 2 ** 64 - 1,
    "keyBooleanFalse": false,
    "KeyBooleanTrue": true,
    "keyByteArray": new Int8Array([1, 2, 2, 2, 2, 1, 1, 2, 2, 1, 1, 4, 5, 4, 4, 255]),
    "keyIntArray": [3, 526255, 1321, 4, -1],
    "keyNull": null,
    "keyDouble": 1.242,
    "keyDouble2": -12.2323e-100
}
map = {"keyMap": mapIn}

describe('message pack test', function () {
    it('testMapPackAndUnpack', function () {
        bytes = new IF.Message(map).pack()
        rebuildedMessage = IF.Message.unpack(bytes)
        assert.deepEqual(map, rebuildedMessage.content)
    })
    it('testMapPackAndClassUnpack', function () {
        bytes = new IF.Message(map).pack()
        assert.throws(function () {
            rebuildedMessage = IF.Message.unpack(bytes.slice(0, 10))
        });
    })
});
