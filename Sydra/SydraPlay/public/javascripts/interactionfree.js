// var msgpack = require('./msgpack.min.js');

class ProtocolException {
    constructor(message) {
        this.message = message;
    }
}

class Message {

    static newBuilder() {
        return new MessageBuilder()
    }

    constructor(content = {}) {
        this.content = content
    }

    messageID() {
        var idO = this.content[Message.KeyMessageID]
        if (Number.isInteger(idO)) {
            return idO
        } else {
            throw new ProtocolException(`MessageID not recognized.`)
        }
    }

    messageType() {
        if (this.content.hasOwnProperty(Message.KeyRequest)) return MessageType.REQUEST
        if (this.content.hasOwnProperty(Message.KeyResponse)) return MessageType.RESPONSE
        if (this.content.hasOwnProperty(Message.KeyError)) return MessageType.ERROR
        return MessageType.UNKNOWN
    }

    requestContent() {
        if (this.messageType() !== MessageType.REQUEST) throw new ProtocolException(`Can not fetch request content in a ${this.messageType()} message.`)
        var content = this.get(Message.KeyRequest, false, false)
        if (Array.isArray(content)) {
            var name = content[0].toString()
            var args = content.slice(1, content.length)
        } else throw new ProtocolException("Illegal message.")
        var map = {}
        for (var key in this.content) {
            if (!Message.Preserved.includes(key)) map[key] = this.content[key]
        }
        return [name, args, map]
    }

    responseContent() {
        if (this.messageType() !== MessageType.RESPONSE) throw new ProtocolException(`Can not fetch response content in a ${self.messageType()} message.`)
        var content = this.get(Message.KeyResponse, true, false)
        var responseID = this.get(Message.KeyResponseID, false, false)
        return [content, responseID]
    }

    errorContent() {
        if (this.messageType() !== MessageType.ERROR) throw new ProtocolException(`Can not fetch error content in a ${this.messageType()} message.`)
        var content = this.get(Message.KeyError, false, false)
        var responseID = this.get(Message.KeyResponseID, false, false)
        return [content, responseID]
    }

    get(key, nilValid = true, nonKeyValid = true) {
        if (this.content.hasOwnProperty(key)) {
            var value = this.content[key]
            if (value === undefined || value == null) {
                if (nilValid) {
                    return null
                } else {
                    throw new ProtocolException(`Nil value invalid with key ${key}.`)
                }
            } else {
                return value
            }
        } else if (nonKeyValid) {
            return null
        } else {
            throw new ProtocolException(`Message does not contains key ${key}.`)
        }
    }

    getTo() {
        return this.get(Message.KeyTo)
    }

    getFrom() {
        return this.get(Message.KeyFrom)
    }

    builder() {
        return Message.newBuilder().update(this.content)
    }

    responseBuilder(content) {
        return Message.newBuilder().asResponse(content, this.messageID(), this.getFrom())
    }

    response(content) {
        return this.responseBuilder(content).create()
    }

    errorBuilder(content) {
        return Message.newBuilder().asError(content, this.messageID(), this.getFrom())
    }

    error(content) {
        return this.errorBuilder(content).create()
    }

    pack() {
        return msgpack.encode(this.content)
    }

    addAndCreate(others) {
        var newContent = {}
        Object.assign(newContent, this.content)
        Object.assign(newContent, others)
        return new Message(newContent)
    }

    static unpack(bytes) {
        var map = msgpack.decode(bytes)
        return new Message(map)
    }
}

Message.KeyMessageID = "MessageID"
Message.KeyResponseID = "ResponseID"
Message.KeyObjectID = "ObjectID"
Message.KeyRequest = "Request"
Message.KeyResponse = "Response"
Message.KeyError = "Error"
Message.KeyFrom = "From"
Message.KeyTo = "To"
Message.KeyNoResponse = "NoResponse"
Message.Preserved = [Message.KeyMessageID, Message.KeyResponseID, Message.KeyObjectID, Message.KeyRequest, Message.KeyResponse, Message.KeyError, Message.KeyFrom, Message.KeyTo, Message.KeyNoResponse]

const MessageType = {
    REQUEST: 'REQUEST',
    RESPONSE: 'RESPONSE',
    ERROR: 'ERROR',
    UNKNOWN: 'UNKNOWN'
}

class MessageBuilder {
    static getAndIncrementID(cls) {
        var id = MessageBuilder.MessageIDs
        MessageBuilder.MessageIDs += 1
        return id
    }

    constructor(updateID = true) {
        this.content = {}
        if (updateID) this.content[Message.KeyMessageID] = MessageBuilder.getAndIncrementID()
    }

    create() {
        return new Message(this.content)
    }

    to(target) {
        if (typeof target === 'string') {
            this.content[Message.KeyTo] = target
        } else {
            throw new ProtocolException("Target should be a String.")
        }
    }

    asType(messageType, content) {
        if (this.content.hasOwnProperty(Message.KeyRequest)) delete this.content[Message.KeyRequest]
        if (this.content.hasOwnProperty(Message.KeyResponse)) delete this.content[Message.KeyResponse]
        if (this.content.hasOwnProperty(Message.KeyError)) delete this.content[Message.KeyError]
        var key = new Map([
            [MessageType.REQUEST, Message.KeyRequest],
            [MessageType.RESPONSE, Message.KeyResponse],
            [MessageType.ERROR, Message.KeyError]]).get(messageType)
        if (key === undefined || key == null) throw new ProtocolException('Unknown type can not be set.')
        this.content[key] = content
        return this
    }

    asRequest(name, args = [], kwargs = {}) {
        this.asType(MessageType.REQUEST, [name].concat(args))
        for (var key in kwargs) {
            if (kwargs.hasOwnProperty(key)) {
                if (Message.Preserved.includes(key)) throw new ProtocolException(`${key} can not be a name of parameter.`)
                this.content[key] = kwargs[key]
            }
        }
        return this
    }

    asResponse(content, responseID, to = undefined) {
        this.asType(MessageType.RESPONSE, content)
        this.content[Message.KeyResponseID] = responseID
        if (to !== undefined) this.content[Message.KeyTo] = to
        return this
    }

    asError(content, responseID, to = undefined) {
        this.asType(MessageType.ERROR, content)
        this.content[Message.KeyResponseID] = responseID
        if (to !== undefined) this.content[Message.KeyTo] = to
        return this
    }

    update(others) {
        Object.assign(this.content, others)
        return this
    }
}

MessageBuilder.MessageIDs = 0

class HttpSession {
    static create(url, onReady, invoker, serviceName, onUncaughtError) {
        var session = new HttpSession(url, onReady, invoker, serviceName, onUncaughtError)
        session.start()
        return new Proxy(session, {
            get: function (obj, prop) {
                if (prop in obj) return obj[prop]

                function proxyFunction() {
                }

                proxyFunction.name1 = prop
                return new Proxy(proxyFunction, {
                    get: function (obj, name) {
                        console.log(`!!! ${name}   ${typeof name}`)
                    },
                    apply: function (target, thisArg, argList) {
                        return session.asynchronousInvoker()[proxyFunction.name1]()
                    }
                })
            },
        })
    }

    constructor(url, onReady, handler, serviceName, onUncaughtError = function (error) {
        console.log(`Uncaughted error: ${error}`)
    }) {
        this.url = url
        this.handler = handler
        this.serviceName = serviceName
        this.token = undefined
        this.onReady = onReady
        this.onUncaughtError = onUncaughtError
        //         self.__running = True
        this.waitingMap = {}
//         self.messageQueue = queue.Queue()
//         self.hydraToken = None
//         self.fetchThread = threading.Thread(target=self.__fetchLoop)
//         self.defaultblockingInvokerTimeout = None
    }

    start() {
        if (self.serviceName === "" || self.serviceName == null || self.serviceName == undefined) {
            this.asynchronousInvoker().ping()
            // response = self.blockingInvoker().ping()
        } else {
            // response = self.blockingInvoker().registerAsService(self.serviceName)
        }
//         self.fetchThread.start()
    }

//
//     def stop(self):
//         if self.serviceName is not None:
//             self.blockingInvoker().unregisterAsService()
//         self.__running = False
//
    messageInvoker(target) {
        return this.createDynamicRemoteObject(target, true)
    }

    asynchronousInvoker(target) {
        return this.createDynamicRemoteObject(target, false)
    }

    sendMessage(message) {
        if (message != undefined) {
            if (message.messageType() == MessageType.REQUEST) {
                var id = message.messageID()
                var buffer = message.pack()
                var invokeFuture = new InvokeFuture(this)
                this.waitingMap[id] = invokeFuture
                this.makeHttpRequest(buffer)
                return invokeFuture
            } else {
                throw '122'
//                 threading.Thread(target=self.__makeHttpRequest, args=[message.pack()]).start()
            }
        } else {
            throw '111'
            this.makeHttpRequest()
        }
    }

    makeHttpRequest(bytes) {
        var xhr = new XMLHttpRequest()
        xhr.open("POST", this.url, true)
        xhr.setRequestHeader('Content-Type', 'application/msgpack')
        if (this.token) xhr.setRequestHeader('InteractionFree-Token', this.token)
        xhr.responseType = "arraybuffer"
        var session = this
        xhr.onload = function () {
            if (this.status == 200) {
                var data = this.response
                var newToken = this.getResponseHeader("InteractionFree-Token")
                if (newToken) session.token = newToken
                if (session.onReady) {
                    session.onReady()
                    session.onReady = undefined
                }
                var binary = new Uint8Array(data)
                if (binary.length > 0) {
                    var msg = Message.unpack(binary)
                    session.messageDeal(msg)
                }
            } else {
                console.log('hehehe')
                //             print("wrong!!!!!!! {}".format(r.status_code))
//             import random
//             time.sleep(random.Random().random())
            }
        }
        xhr.send(bytes)
    }

//     def __fetchLoop(self):
//         while self.__running:
//             self.__makeHttpRequest(b'')
//
    messageDeal(message) {
        var type = message.messageType()
        if (type === MessageType.REQUEST) {
//             (name, args, kwargs) = message.requestContent()
//             try:
//                 method = getattr(self.invoker, name)
//                 noResponse = message.get(Message.KeyNoResponse)
//                 if callable(method):
//                     try:
//                         result = method(*args, **kwargs)
//                         response = message.response(result)
//                         if noResponse is not True:
//                             self.__sendMessage__(response)
//                     except BaseException as e:
//                         error = message.error(e.__str__())
//                         self.__sendMessage__(error)
//                     return
//             except BaseException as e:
//                 response = message.error('InvokeError: Command {} not found.'.format(name))
//                 self.__sendMessage__(response)
        } else if (type === MessageType.RESPONSE) {
            var responseContent = message.responseContent()
            var content = responseContent[0]
            var responseID = responseContent[1]
            var invokeFuture = this.waitingMap[responseID]
            delete this.waitingMap[responseID]
            invokeFuture.response(content)
        } else if (type === MessageType.ERROR) {
            var errorContent = message.errorContent()
            var error = errorContent[0]
            var responseID = errorContent[1]
            var invokeFuture = this.waitingMap[responseID]
            delete this.waitingMap[responseID]
            invokeFuture.error(errorContent)
        } else {
            //             print('A Wrong Message: {}'.format(message))
        }
    }

//     def __getattr__(self, item):
    // var handler = {
    //     get: function (obj, prop) {
    //         return prop in obj ? obj[prop] : 37
    //     }
    // };
//         return InvokeTarget(self, item)

    createDynamicRemoteObject(target, toMessage = false) {
        return new Proxy({session: this, target: target, toMessage: toMessage}, {
            get: function (obj, prop) {
                if (prop in obj) return obj[prop]
                return function (...args) {
                    var builder = Message.newBuilder().asRequest(prop, args)
                    if (this.target != null && this.target != undefined && this.target != '') builder.to(this.target)
                    var message = builder.create()
                    if (this.toMessage) return message
                    else return this.session.sendMessage(message, function () {
                        console.log('calling back')
                    })
                }
            }
        })
    }

//     def __getattr__(self, item):
//         item = u'{}'.format(item)
//         def invoke(*args, **kwargs):
//             builder = Message.newBuilder().asRequest(item, args, kwargs)
//             if self.__target is not None:
//                 builder.to(self.__target)
//             if self.__objectID is not 0:
//                 builder.objectID(self.__objectID)
//             message = builder.create()
//             if self.__toMessage:
//                 return message
//             elif self.__blocking:
//                 return self.__session.__sendMessage__(message).sync(self.__timeout)
//             else:
//                 return self.__session.__sendMessage__(message)
//
//         return invoke
}

class RuntimeInovker {
    constructor(invoker) {
        this.invoker = invoker
    }

    invoke(name, args) {
        var method = this.invoker[name]
        if (method) {
            if (typeof method === 'function') return method.call(this.invoker, ...args)
            else return method
        } else {
            throw new ProtocolException(`Method not found: ${name}`)
        }
    }
}

class InvokeFuture {
    constructor(session) {
        this.session = session
        this.response = function (response) {
            if (this.success) this.success(response)
        }
        this.error = function (error) {
            if (this.failure) this.failure(error)
            else if (this.session.onUncaughtError) this.session.onUncaughtError(error)
        }
    }

    onSuccess(func) {
        this.success = func
        return this
    }

    onFailure(func) {
        this.failure = func
        return this
    }
}

// class InvokeTarget {
//     def __init__(self, session, item):
//         self.session = session
//         self.name = item
//
//     def __getattr__(self, item):
//         item = u'{}'.format(item)
//         return self.session.blockingInvoker(self.name, self.session.defaultblockingInvokerTimeout).__getattr__(item)
//
//     def __call__(self, *args, **kwargs):
//         invoker = self.session.blockingInvoker(None, self.session.defaultblockingInvokerTimeout)
//         func = invoker.__getattr__(self.name)
//         return func(*args, **kwargs)
// }


// module.exports = {
//     Message,
//     MessageBuilder,
//     MessageType,
//     ProtocolException,
//     RuntimeInovker,
//     HttpSession
// };