var add = require('../public/javascripts/interactionfree.js');
var expect = require('chai').expect;

map = {
    "keyString": "value1",
    // "keyInt": 123,
    // "keyLong": 84294967295,
    // "keyBigInteger": 2 ^ 100,
    // "keyBooleanFalse": false,
    // "KeyBooleanTrue": true,
    // "keyByteArray": bytearray([1, 2, 2, 2, 2, 1, 1, 2, 2, 1, 1, 4, 5, 4, 4, 255]),
    // "keyIntArray": [3, 526255, 1321, 4, -1],
    // "keyNull": None,
    // "keyDouble": 1.242,
    // "keyDouble2": -12.2323e-100
}

function testMessageIDAndItsIncrecement() {
    expect(add(1, 1)).to.be.equal(2);
}

//     m0 = Message.newBuilder().create()
//     id0 = m0.messageID()
//     for i in range(0, 100):
//         mi = Message.newBuilder().create()
//         self.assertTrue(mi.messageID() == id0 + 1 + i)
//     for m in [Message({}),
//               Message({Message.KeyMessageID: "-1"}),
//               Message({Message.KeyMessageID: None}),
//               Message({Message.KeyMessageID: 1.2})]:
//         self.assertRaises(ProtocolException, m.messageID)
//     self.assertTrue(Message({Message.KeyMessageID: 100}).messageID() == 100)
//
// def testGetInformation(self):
//     m = Message(MessageTest.map)
//     self.assertEqual(m.get("keyString"), "value1")
//     self.assertRaises(TypeError, lambda: m.get("keyString") + 1)
//     self.assertRaises(ProtocolException, lambda: m.get("keyNull", False))
//     self.assertIsNone(m.get("keyNull", True))
//
// def testBasicInformation(self):
//     b = Message.newBuilder()
//     self.assertIsNone(b.create().getTo())
//     b.to("the target")
//     self.assertEqual(b.create().getTo(), "the target")
//
// def testTypeAndContent(self):
//     builder = Message.newBuilder()
//     self.assertEqual(builder.create().messageType(), Message.Type.Unknown)
//     m1 = builder.asRequest("TestRequest1").create()
//     self.assertEqual(m1.messageType(), Message.Type.Request)
//     self.assertEqual(m1.requestContent(), ("TestRequest1", [], {}))
//     self.assertRaises(ProtocolException,
//                       lambda: builder.asRequest("TestRequest2", [100, "arg"],
//                                                 {"a": 1, Message.KeyTo: "11"}).create())
//     m2 = builder.asRequest("TestRequest2", [100, "arg"], {"a": 1, "b": "bb"}).create()
//     self.assertEqual(m2.messageType(), Message.Type.Request)
//     self.assertEqual(m2.requestContent(), ("TestRequest2", [100, "arg"], {"b": "bb", "a": 1}))
//     m3 = builder.asResponse("ContentOfResponse", 100).create()
//     self.assertEqual(m3.messageType(), Message.Type.Response)
//     self.assertRaises(ProtocolException, m3.requestContent)
//     self.assertEqual(m3.responseContent(), ("ContentOfResponse", 100))
//     m4 = builder.asError("ContentOfError", 1001).create()
//     self.assertEqual(m4.messageType(), Message.Type.Error)
//     self.assertRaises(ProtocolException, m4.requestContent)
//     self.assertRaises(ProtocolException, m4.responseContent)
//     self.assertEqual(m4.errorContent(), ("ContentOfError", 1001))
//
// def testTypeAndContentAgain(self):
//     self.testTypeAndContent()
//
// def testUpdateMessage(self):
//     m1 = Message.newBuilder().asRequest("TestRequest1").create()
//     self.assertEqual(m1.get("testitem"), None)
//     m2 = m1 + {"testitem": 100}
//     self.assertEqual(m2.get("testitem"), 100)
//     m3 = m2.builder().create()
//     self.assertEqual(m3.get("testitem"), 100)
//     m4 = m3.builder().update({"t2": "11"}).create()
//     self.assertEqual(m4.get("testitem"), 100)
//     self.assertEqual(m4.get("t2"), "11")
//     r1 = m4.response(99)
//     self.assertEqual(r1.messageType(), Message.Type.Response)
//     self.assertEqual(r1.responseContent(), (99, m4.messageID()))
//     e1 = m4.error(999)
//     self.assertEqual(e1.messageType(), Message.Type.Error)
//     self.assertEqual(e1.errorContent(), (999, m4.messageID()))

describe('message test', function () {
    it('testMessageIDAndItsIncrecement', testMessageIDAndItsIncrecement);
});


