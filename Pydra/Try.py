from Pydra import Session
import time
import threading
import random

print('In Try.')

session = Session.newSession(("localhost", 20102))

bi = session.blockingInvoker(timeout=1)

while True:
    time.sleep(5)
    try:
        si = bi.sessionsInformation()
        print(si)
    except BaseException as e:
        print("EXCEPTION!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
