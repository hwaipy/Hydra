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
    static create(url, invoker, serviceName) {
        var session = new HttpSession(url, invoker, serviceName)
        session.start()
        return session
    }

    constructor(url, handler, serviceName) {
        this.url = url
        this.handler = handler
        this.serviceName = serviceName
//         self.__running = True
//         self.__waitingMap = {}
//         self.__waitingMapLock = threading.Lock()
//         self.unpacker = msgpack.Unpacker(raw=False)
//         self.messageQueue = queue.Queue()
//         self.hydraToken = None
//         self.fetchThread = threading.Thread(target=self.__fetchLoop)
//         self.defaultblockingInvokerTimeout = None
    }

    start() {
        if (self.serviceName === "" || self.serviceName == null || self.serviceName == undefined) {
            this.createDynamicRemoteObject().ping()
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
        // return new DynamicRemoteObject(this,target,toMessage=False, blocking=False, target=target, objectID=0, timeout=None)
    }

    sendMessage(message) {
        if (message != undefined) {
            if (message.messageType() == MessageType.REQUEST) {
                var id = message.messageID()
                console.log(message.content)
                var buffer = message.pack()

                // var blob = new Blob(message.pack(), {type: 'application/msgpack'});
                // $.post(this.url, blob, function (data, status) {
                //     alert("Data: " + data + "\nStatus: " + status);
                // });

                var xhr = new XMLHttpRequest()
                xhr.open("POST", this.url, true)
                xhr.setRequestHeader('Content-Type', 'application/msgpack')
                xhr.onload = function () {
                    if (this.status == 200) {
                        console.log('200!!!')
                        console.log(this.getAllResponseHeaders())
                        var data = this.response
                        var msg = msgpack.decode(new Uint8Array(data))
                        onResponse(msg)
                    } else {
                        console.log('hehehe')
                    }
                }
                xhr.send(buffer)


//                 (future, onFinish, resultMap) = InvokeFuture.newFuture()
//                 self.__waitingMapLock.acquire()
//                 if self.__waitingMap.__contains__(id):
//                     raise ProtocolException("MessageID have been used.")
//                 self.__waitingMap[id] = (resultMap, onFinish)
//                 self.__waitingMapLock.release()
//
//                 threading.Thread(target=self.__makeHttpRequest, args=[message.pack()]).start()
//                 return future
            } else {
                throw '122'
//                 threading.Thread(target=self.__makeHttpRequest, args=[message.pack()]).start()
            }
        } else {
            throw '111'
            this.makeHttpRequest()
        }
    }

//     def __makeHttpRequest(self, bytes):
//         headers = {'Content-Type': 'application/msgpack'}
//         if self.hydraToken is not None:
//             headers['Cookie'] = 'HydraToken={}'.format(self.hydraToken)
//         r = requests.post(self.url, data=bytes, headers=headers)
//         if (r.status_code == 200):
//             # for head in r.headers:
//             #     print('{}: {}'.format(head, r.headers[head]))
//             # print()
//             token = r.cookies.get('HydraToken')
//             if token is not None:
//                 self.hydraToken = token
//             if (len(r.content) > 0):
//                 responseMessage = Message.unpack(r.content)
//                 self.__messageDeal(responseMessage)
//         else:
//             print("wrong!!!!!!! {}".format(r.status_code))
//             import random
//             time.sleep(random.Random().random())
//
//     def __fetchLoop(self):
//         while self.__running:
//             self.__makeHttpRequest(b'')
//
//     def __messageDeal(self, message):
//         type = message.messageType()
//         if type is Message.Type.Request:
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
//
//         elif (type is Message.Type.Response) or (type is Message.Type.Error):
//             if type is Message.Type.Response:
//                 (result, id) = message.responseContent()
//             else:
//                 (error, id) = message.errorContent()
//             self.__waitingMapLock.acquire()
//             if self.__waitingMap.__contains__(id):
//                 (futureEntry, runnable) = self.__waitingMap[id]
//                 if type is Message.Type.Response:
//                     futureEntry['result'] = result
//                 else:
//                     futureEntry['error'] = error
//                 runnable()
//             else:
//                 print('ResponseID not recognized: {}'.format(message))
//             self.__waitingMapLock.release()
//         else:
//             print('A Wrong Message: {}'.format(message))
//
//     def __getattr__(self, item):
    // var handler = {
    //     get: function (obj, prop) {
    //         return prop in obj ? obj[prop] : 37
    //     }
    // };
//         return InvokeTarget(self, item)


    createDynamicRemoteObject(target, toMessage = false) {
        var handler = {
            get: function (obj, prop) {
                if (prop in obj) return obj[prop]
                return function (...args) {
                    var builder = Message.newBuilder().asRequest(prop, args)
                    if (this.target != null && this.target != undefined && this.target != '') builder.to(this.target)
                    var message = builder.create()
                    if (this.toMessage) return message
                    else return this.session.sendMessage(message)
                }
            }
        }
        return new Proxy({session: this, target: target, toMessage: toMessage}, handler)
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

//
// class InvokeFuture:
//     @classmethod
//     def newFuture(cls):
//         future = InvokeFuture()
//         return (future, future.__onFinish, future.__resultMap)
//
//     def __init__(self):
//         self.__done = False
//         self.__result = None
//         self.__exception = None
//         self.__onComplete = None
//         self.__metux = threading.Lock()
//         self.__resultMap = {}
//         self.__awaitSemaphore = threading.Semaphore(0)
//
//     def isDone(self):
//         return self.__done
//
//     def isSuccess(self):
//         return self.__exception is None
//
//     def result(self):
//         return self.__result
//
//     def exception(self):
//         return self.__exception
//
//     def onComplete(self, func):
//         self.__metux.acquire()
//         self.__onComplete = func
//         if self.__done:
//             self.__onComplete()
//         self.__metux.release()
//
//     def waitFor(self, timeout=None):
//         # For Python 3 only.
//         # if self.__awaitSemaphore.acquire(True, timeout):
//         #     self.__awaitSemaphore.release()
//         #     return True
//         # else:
//         #     return False
//
//         # For Python 2 & 3
//         timeStep = 0.1 if timeout is None else timeout / 10
//         startTime = time.time()
//         while True:
//             acq = self.__awaitSemaphore.acquire(False)
//             if acq:
//                 return acq
//             else:
//                 passedTime = time.time() - startTime
//                 if (timeout is not None) and (passedTime >= timeout):
//                     return False
//                 time.sleep(timeStep)
//
//     def sync(self, timeout=None):
//         if self.waitFor(timeout):
//             if self.isSuccess():
//                 return self.__result
//             elif isinstance(self.__exception, BaseException):
//                 raise self.__exception
//             else:
//                 raise ProtocolException('Error state in InvokeFuture.')
//         else:
//             raise ProtocolException('Time out!')
//
//     def __onFinish(self):
//         self.__done = True
//         if self.__resultMap.__contains__('result'):
//             self.__result = self.__resultMap['result']
//         if self.__resultMap.__contains__('error'):
//             self.__exception = ProtocolException(self.__resultMap['error'])
//         if self.__onComplete is not None:
//             self.__onComplete()
//         self.__awaitSemaphore.release()
//
// }

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