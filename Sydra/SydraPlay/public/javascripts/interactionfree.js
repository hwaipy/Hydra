function add(x, y) {
    return x + y;
}

module.exports = add;


// class ProtocolException(Exception):
//     def __init__(self, description, message=None):
//         Exception.__init__(self)
//         self.description = description
//         self.message = message
//
//     def __str__(self):
//         if self.message:
//             return '{} - {}'.format(self.description, self.message)
//         else:
//             return self.description
//

var Message = {
    createNew: function () {
        var msg = {};
        msg.name = "大毛";
        msg.makeSound = function () {
            alert("喵喵喵");
        };
        return msg;
    }
};

// class Message:
//     KeyMessageID = u"MessageID"
//     KeyResponseID = u"ResponseID"
//     KeyObjectID = u"ObjectID"
//     KeyRequest = u"Request"
//     KeyResponse = u"Response"
//     KeyError = u"Error"
//     KeyFrom = u"From"
//     KeyTo = u"To"
//     KeyNoResponse = u"NoResponse"
//     Preserved = [KeyMessageID, KeyResponseID, KeyObjectID, KeyRequest, KeyResponse, KeyError, KeyFrom, KeyTo,
//                  KeyNoResponse]
//
//     @classmethod
//     def newBuilder(cls):
//         return MessageBuilder()
//
//     def __init__(self, content={}):
//         self.__content = content
//
//     class Type(enum.Enum):
//         Request = 1
//         Response = 2
//         Error = 3
//         Unknown = 0
//
//     def messageID(self):
//         if self.__content.__contains__(Message.KeyMessageID):
//             idO = self.__content[Message.KeyMessageID]
//             if isinstance(idO, int):
//                 return idO
//             else:
//                 raise ProtocolException("MessageID {} not recognized.".format(idO), self)
//         else:
//             raise ProtocolException("MessageID not exists in Message.")
//
//     def messageType(self):
//         if self.__content.__contains__(Message.KeyRequest): return Message.Type.Request
//         if self.__content.__contains__(Message.KeyResponse): return Message.Type.Response
//         if self.__content.__contains__(Message.KeyError): return Message.Type.Error
//         return Message.Type.Unknown
//
//     def requestContent(self):
//         if self.messageType() is not Message.Type.Request:
//             raise ProtocolException("Can not fetch request content in a {} message.".format(self.messageType()))
//         content = self.get(Message.KeyRequest, False, False)
//         if isinstance(content, str):
//             name = content
//             args = []
//         elif isinstance(content, list):
//             name = content[0].__str__()
//             args = content[1:]
//         else:
//             raise ProtocolException("Illegal message.")
//         map = {}
//         for key in self.__content.keys():
//             if not Message.Preserved.__contains__(key):
//                 map[key] = self.__content[key]
//         return (name, args, map)
//
//     def responseContent(self):
//         if self.messageType() is not Message.Type.Response:
//             raise ProtocolException("Can not fetch response content in a {} message.".format(self.messageType()))
//         content = self.get(Message.KeyResponse, True, False)
//         responseID = self.get(Message.KeyResponseID, False, False)
//         return (content, responseID)
//
//     def errorContent(self):
//         if self.messageType() is not Message.Type.Error:
//             raise ProtocolException("Can not fetch error content in a {} message.".format(self.messageType()))
//         content = self.get(Message.KeyError, False, False)
//         responseID = self.get(Message.KeyResponseID, False, False)
//         return (content, responseID)
//
//     def get(self, key, nilValid=True, nonKeyValid=True):
//         if self.__content.__contains__(key):
//             value = self.__content[key]
//             if value == None:
//                 if nilValid:
//                     return None
//                 else:
//                     raise ProtocolException("Nil value invalid with key {}.".format(key))
//             else:
//                 return value
//         elif (nonKeyValid):
//             return None
//         else:
//             raise ProtocolException("Message does not contains key {}.".format(key))
//
//     def getTo(self):
//         return self.get(Message.KeyTo)
//
//     def getFrom(self):
//         return self.get(Message.KeyFrom)
//
//     def getObjectID(self):
//         return self.get(Message.KeyObjectID)
//
//     def builder(self):
//         return Message.newBuilder().update(self.__content)
//
//     def responseBuilder(self, content):
//         return Message.newBuilder().asResponse(content, self.messageID(), self.getFrom())
//
//     def response(self, content):
//         return self.responseBuilder(content).create()
//
//     def errorBuilder(self, content):
//         return Message.newBuilder().asError(content, self.messageID(), self.getFrom())
//
//     def error(self, content):
//         return self.errorBuilder(content).create()
//
//     def pack(self, remoteObjectWrapper=None):
//         def encode(obj):
//             ro = remoteObjectWrapper(obj)
//             ext = msgpack.ExtType(11, bytes(ro.name, 'utf-8') + struct.pack('!q', ro.id))
//             return ext
//
//         packed = msgpack.packb(self.__content, use_bin_type=True, default=encode)
//         return packed
//
//     def __str__(self):
//         content = ', '.join(['{}: {}'.format(k, self.__content[k]) for k in self.__content.keys()])
//         return "Message [{}]".format(content)
//
//     def __add__(self, other):
//         if isinstance(other, dict):
//             content = self.__content.copy()
//             content.update(other)
//             return Message(content)
//         raise TypeError
//
//     @classmethod
//     def unpack(cls, bytes):
//         unpacker = msgpack.Unpacker(raw=False)
//         unpacker.feed(bytes)
//         m1 = unpacker.__next__()
//         return Message(m1)
//
//
// class MessageBuilder:
//     MessageIDs = 0
//
//     @classmethod
//     def getAndIncrementID(cls):
//         __mutex__ = threading.Lock()
//         __mutex__.acquire()
//         id = MessageBuilder.MessageIDs
//         MessageBuilder.MessageIDs += 1
//         __mutex__.release()
//         return id
//
//     def __init__(self, updateID=True):
//         self.__content = {}
//         if updateID:
//             self.__content.update({Message.KeyMessageID: MessageBuilder.getAndIncrementID()})
//
//     def create(self):
//         return Message(self.__content)
//
//     def to(self, target):
//         if isinstance(target, str) or isinstance(target, unicode):
//             self.__content.update({Message.KeyTo: target})
//         else:
//             raise TypeError("Target should be a String.")
//
//     def objectID(self, id):
//         if isinstance(id, int):
//             self.__content.update({Message.KeyObjectID: id})
//         else:
//             raise TypeError("Target should be a String.")
//
//     def asType(self, messageType, content):
//         def raiseOnUnknown():
//             raise ProtocolException("Unknown type can not be set.")
//
//         if self.__content.__contains__(Message.KeyRequest):
//             self.__content.__delitem__(Message.KeyRequest)
//         if self.__content.__contains__(Message.KeyResponse):
//             self.__content.__delitem__(Message.KeyResponse)
//         if self.__content.__contains__(Message.KeyError):
//             self.__content.__delitem__(Message.KeyError)
//         {Message.Type.Request: lambda: self.__content.update({Message.KeyRequest: content}),
//          Message.Type.Response: lambda: self.__content.update({Message.KeyResponse: content}),
//          Message.Type.Error: lambda: self.__content.update({Message.KeyError: content})
//          }.get(messageType, raiseOnUnknown)()
//         return self
//
//     def asRequest(self, name, args=[], kwargs={}):
//         content = [name] + [arg for arg in args]
//         self.asType(Message.Type.Request, content)
//         for key in kwargs:
//             if Message.Preserved.__contains__(key):
//                 raise ProtocolException("{} can not be a name of parameter.".format(key))
//         self.__content.update(kwargs)
//         return self
//
//     def asResponse(self, content, responseID, to=None):
//         self.asType(Message.Type.Response, content)
//         self.__content[Message.KeyResponseID] = responseID
//         if to is not None:
//             self.__content[Message.KeyTo] = to
//         return self
//
//     def asError(self, content, responseID, to=None):
//         self.asType(Message.Type.Error, content)
//         self.__content[Message.KeyResponseID] = responseID
//         if to is not None:
//             self.__content[Message.KeyTo] = to
//         return self
//
//     def update(self, other):
//         if isinstance(other, dict):
//             self.__content.update(other)
//             return self
//         else:
//             raise TypeError
//
//     def __str__(self):
//         return "MessageBuilder {}".format(self.__content)
//
// class HttpSession:
//     @classmethod
//     def create(cls, url, invoker=None, serviceName=None):
//         session = HttpSession(url, invoker, serviceName)
//         session.start()
//         return session
//
//     def __init__(self, url, invoker=None, serviceName=None):
//         self.url = url
//         self.serviceName = serviceName
//         self.invoker = invoker
//         self.__running = True
//         self.__waitingMap = {}
//         self.__waitingMapLock = threading.Lock()
//         self.unpacker = msgpack.Unpacker(raw=False)
//         self.messageQueue = queue.Queue()
//         self.hydraToken = None
//         self.fetchThread = threading.Thread(target=self.__fetchLoop)
//         self.defaultblockingInvokerTimeout = None
//
//     def start(self):
//         if (self.serviceName == "" or self.serviceName == None):
//             response = self.blockingInvoker().ping()
//         else:
//             response = self.blockingInvoker().registerAsService(self.serviceName)
//         self.fetchThread.start()
//
//     def stop(self):
//         if self.serviceName is not None:
//             self.blockingInvoker().unregisterAsService()
//         self.__running = False
//
//     def messageInvoker(self, target=None):
//         return DynamicRemoteObject(self, toMessage=True, blocking=False, target=target, objectID=0, timeout=None)
//
//     def asynchronousInvoker(self, target=None):
//         return DynamicRemoteObject(self, toMessage=False, blocking=False, target=target, objectID=0, timeout=None)
//
//     def blockingInvoker(self, target=None, timeout=None):
//         return DynamicRemoteObject(self, toMessage=False, blocking=True, target=target, objectID=0, timeout=timeout)
//
//     def __sendMessage__(self, message):
//         if (message is not None):
//             if (message.messageType() == Message.Type.Request):
//                 id = message.messageID()
//                 (future, onFinish, resultMap) = InvokeFuture.newFuture()
//                 self.__waitingMapLock.acquire()
//                 if self.__waitingMap.__contains__(id):
//                     raise ProtocolException("MessageID have been used.")
//                 self.__waitingMap[id] = (resultMap, onFinish)
//                 self.__waitingMapLock.release()
//
//                 threading.Thread(target=self.__makeHttpRequest, args=[message.pack()]).start()
//                 return future
//             else:
//                 threading.Thread(target=self.__makeHttpRequest, args=[message.pack()]).start()
//         else:
//             threading.Thread(target=self.__makeHttpRequest, args=[b'']).start()
//
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
//         return InvokeTarget(self, item)
//
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
//
// class InvokeTarget:
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
//
//
// class RemoteObject(object):
//     def __init__(self, name, id):
//         self.name = name
//         self.id = id
//
//     def __str__(self):
//         return "RemoteObject[{},{}]".format(self.name, self.id)
//
//
// class DynamicRemoteObject(RemoteObject):
//     def __init__(self, session, toMessage, blocking, target, objectID, timeout):
//         super(DynamicRemoteObject, self).__init__(target, objectID)
//         self.__session = session
//         self.__target = target
//         self.__objectID = objectID
//         self.__toMessage = toMessage
//         self.__blocking = blocking
//         self.__timeout = timeout
//         self.name = target
//         self.id = objectID
//
//     def __getattr__(self, item):
//         item = u'{}'.format(item)
//
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
//
//     def __str__(self):
//         return "DynamicRemoteObject[{},{}]".format(self.name, self.id)
