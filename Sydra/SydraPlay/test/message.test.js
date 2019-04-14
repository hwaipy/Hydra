var IF = require('../public/javascripts/interactionfree.js');
var assert = require('assert');

testMap = {
    "keyString": "value1",
    "keyInt": 123,
    "keyLong": 84294967295,
    "keyBigInteger": 2 ^ 100,
    "keyBooleanFalse": false,
    "KeyBooleanTrue": true,
    "keyByteArray": new Int8Array([1, 2, 2, 2, 2, 1, 1, 2, 2, 1, 1, 4, 5, 4, 4, 255]),
    "keyIntArray": [3, 526255, 1321, 4, -1],
    "keyNull": null,
    "keyDouble": 1.242,
    "keyDouble2": -12.2323e-100
}

describe('message test', function () {
    it('testMessageIDAndItsIncrecement', function () {
        m0 = IF.Message.newBuilder().create()
        id0 = m0.messageID()
        for (i = 0; i < 10; i++) {
            mi = IF.Message.newBuilder().create()
            assert.equal(mi.messageID(), id0 + 1 + i)
        }
        newContent = {}
        newContent[IF.Message.KeyMessageID] = 100
        m100 = new IF.Message(newContent)
        assert.equal(m100.messageID(), 100)
    })
    it('testGetInformation', function () {
        m = new IF.Message(testMap)
        assert.equal(m.get("keyString"), "value1")
        assert.throws(function () {
            m.get("keyNull", false, false)
        }, IF.ProtocolException);
        assert.equal(m.get("keyNull", true), undefined)
    })
    it('testBasicInformation', function () {
        b = IF.Message.newBuilder()
        assert.equal(b.create().getTo(), undefined)
        b.to("the target")
        assert.equal(b.create().getTo(), "the target")
    })
    it('testTypeAndContent', function () {
        builder = IF.Message.newBuilder()
        assert.equal(builder.create().messageType(), IF.MessageType.UNKNOWN)
        m1 = builder.asRequest("TestRequest1").create()
        assert.equal(m1.messageType(), IF.MessageType.REQUEST)
        assert.deepEqual(m1.requestContent(), ["TestRequest1", [], {}])
        newKwargs = {a: 1}
        newKwargs[IF.Message.KeyTo] = '11'
        assert.throws(function () {
            builder.asRequest("TestRequest2", [100, "arg"], newKwargs).create()
        }, IF.ProtocolException);
        m2 = builder.asRequest("TestRequest2", [100, "arg"], {a: 1, b: "bb"}).create()
        assert.equal(m2.messageType(), IF.MessageType.REQUEST)
        assert.deepEqual(m2.requestContent(), ["TestRequest2", [100, "arg"], {"b": "bb", "a": 1}])
        m3 = builder.asResponse("ContentOfResponse", 100).create()
        assert.equal(m3.messageType(), IF.MessageType.RESPONSE)
        assert.throws(function () {
            m3.requestContent()
        }, IF.ProtocolException)
        assert.deepEqual(m3.responseContent(), ["ContentOfResponse", 100])
        m4 = builder.asError("ContentOfError", 1001).create()
        assert.equal(m4.messageType(), IF.MessageType.ERROR)
        assert.throws(function () {
            m4.requestContent()
        }, IF.ProtocolException)
        assert.deepEqual(m4.errorContent(), ["ContentOfError", 1001])
    })
    it('testUpdateMessage', function () {
        m1 = IF.Message.newBuilder().asRequest("TestRequest1").create()
        assert.equal(m1.get("testitem"), undefined)
        m2 = m1.addAndCreate({"testitem": 100})
        assert.equal(m2.get("testitem"), 100)
        m3 = m2.builder().create()
        assert.equal(m3.get("testitem"), 100)
        m4 = m3.builder().update({"t2": "11"}).create()
        assert.equal(m4.get("testitem"), 100)
        assert.equal(m4.get("t2"), "11")
        r1 = m4.response(99)
        assert.equal(r1.messageType(), IF.MessageType.RESPONSE)
        assert.deepEqual(r1.responseContent(), [99, m4.messageID()])
        e1 = m4.error(999)
        assert.equal(e1.messageType(), IF.MessageType.ERROR)
        assert.deepEqual(e1.errorContent(), [999, m4.messageID()])
    })
    it('Test dynamic invoker', function () {
        messageTestSession = new IF.HttpSession('')
        serverInvoker = messageTestSession.messageInvoker()
        m1 = serverInvoker.fun1(1, 2, "3", null, [1, 2, "3d"])
        assert.equal(m1.messageType(), IF.MessageType.REQUEST)
        assert.deepEqual(m1.requestContent(), ["fun1", [1, 2, "3", null, [1, 2, "3d"]], {}])
        invoker2 = messageTestSession.messageInvoker("OnT")
        m2 = invoker2.fun2()
        assert.equal(m2.messageType(), IF.MessageType.REQUEST)
        assert.deepEqual(m2.requestContent(), ["fun2", [], {}])
        assert.equal(m2.getTo(), "OnT")
    })
});
