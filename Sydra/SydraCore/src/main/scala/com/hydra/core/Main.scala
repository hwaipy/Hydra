package com.hydra.core

import javax.swing.{JFrame, WindowConstants}

import scala.io.Source

object Main extends App {
  println("Sydra-Core")
  if (args.size > 0 && args(0) == "MemoryTest") memoryTest();

  def memoryTest() = {
    val manager = new MessageSessionManager
    val service = new StatelessMessageService(manager)

    class PXITDCHandle {
      var session: MessageClient = null

      def begin() = {
        val storage = session.blockingInvoker("Storage")
        val ask = storage.ask()
        ask
      }
    }
    class StorageHandle {
      def ask() = "YES"
    }
    val sto = MessageClient.create(new LocalStatelessMessageChannel(service, internalDelay = 1), "Storage", new StorageHandle)
    val pxiTDCHandler = new PXITDCHandle
    val s1 = MessageClient.create(new LocalStatelessMessageChannel(service, internalDelay = 1), "PXITDC", pxiTDCHandler)
    pxiTDCHandler.session = s1

    def printMemoryInfo() = {
      val runtime = Runtime.getRuntime

      def showMemory(memory: Long) = s"${memory / 1000 / 1000.0}"

      println(s"Memory Status: Used Memory ${showMemory(runtime.totalMemory() - runtime.freeMemory())} MB, Free Memory ${showMemory(runtime.freeMemory())} MB, Max Memory ${showMemory(runtime.maxMemory())} MB.")
    }

    def printServiceInfo() = {
      val sizes = List(service.statelessSessions.size) ++ service.manager.debugCollectionSizes
      println(s"${sizes},   ${MessageSession.sessionCount.get}")
    }

    val thread = new Thread(() => {
      while (true) {
        val localChannel = new LocalStatelessMessageChannel(service, internalDelay = 1)
        val slocal = MessageClient.create(localChannel)
        (0 to 1000).foreach(i => {
//          slocal.Storage.ask()
          slocal.PXITDC.begin()
        })
        slocal.close
        Runtime.getRuntime.gc()
        Thread.sleep(1000)
        printMemoryInfo()
        printServiceInfo()
      }
    })
    thread.setDaemon(true)
    thread.start()
    showFrame()
  }

  def showFrame() = {
    val frame = new JFrame()
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    frame.setVisible(true)
  }
}