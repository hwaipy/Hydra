//package com.hydra.sydrajs
//
//import java.lang.reflect.InvocationTargetException
//
//class RuntimeInvokeTest extends FunSuite with BeforeAndAfter {
//  var runtimeInvoker: RuntimeInvoker = _
//  var invoker: Any = _
//
//  before {
//    invoker = new InvokerTestClass()
//    runtimeInvoker = new RuntimeInvoker(invoker)
//  }
//
//  test("Test Invalid Parameters") {
//    intercept[IllegalArgumentException] {
//      val dd = new RuntimeInvoker(null)
//    }
//    intercept[IllegalArgumentException] {
//      runtimeInvoker.invoke(null, kwargs = Map())
//    }
//    intercept[IllegalArgumentException] {
//      runtimeInvoker.invoke("null", Nil, null)
//    }
//    intercept[IllegalArgumentException] {
//      runtimeInvoker.invoke("null", null, Map())
//    }
//    intercept[IllegalArgumentException] {
//      runtimeInvoker.invoke(null, null, null)
//    }
//  }
//
//  test("Test Invoke None") {
//    intercept[IllegalArgumentException] {
//      runtimeInvoker.invoke("runf")
//    }
//    intercept[IllegalArgumentException] {
//      runtimeInvoker.invoke("run", kwargs = Map("asd" -> 3))
//    }
//  }
//
//  test("Test Method 1") {
//    assert(runtimeInvoker.invoke("run", kwargs = Map("a1" -> 1)) == "Method 1:1")
//    intercept[InvocationTargetException] {
//      runtimeInvoker.invoke("run", kwargs = Map("a1" -> -1))
//    }
//  }
//
//  test("Test Method 2") {
//    assert(runtimeInvoker.invoke("run", kwargs = Map("a2" -> 1, "b2" -> 1.1)) == "Method 2:1,1.1")
//  }
//
//  test("Test Method 3") {
//    assert(runtimeInvoker.invoke("run", kwargs = Map("a3" -> 1, "b3" -> 1.1, "c3" -> List("1"), "d3" -> false)) == "Method 3:1,1.1,List(1),false")
//    assert(runtimeInvoker.invoke("run", kwargs = Map("a3" -> 1, "b3" -> 1.1, "c3" -> Vector("1"), "d3" -> false)) == "Method 3:1,1.1,Vector(1),false")
//    intercept[InvocationTargetException] {
//      runtimeInvoker.invoke("run", kwargs = Map("a3" -> 1, "b3" -> 1.1, "c3" -> List(1), "d3" -> false))
//    }
//  }
//
//  test("Test No Param Method") {
//    assert(runtimeInvoker.invoke("noParam", kwargs = Map()) == "No Param")
//  }
//
//  test("Test hybrid invoke.") {
//    assert(runtimeInvoker.invoke("func", kwargs = Map("a1" -> 100, "a2" -> "hello", "a3" -> 0.5, "a4" -> 100.1010f, "a5" -> true)) == "FUNC:100,hello,0.5,100.101,true")
//    assert(runtimeInvoker.invoke("func", kwargs = Map("a1" -> 100, "a2" -> "hello", "a3" -> 0.5, "a5" -> true)) == "FUNC:100,hello,0.5,1.0,true")
//    assert(runtimeInvoker.invoke("func", kwargs = Map("a1" -> 100, "a2" -> "hello", "a3" -> 0.5)) == "FUNC:100,hello,0.5,1.0,false")
//    assert(runtimeInvoker.invoke("func", 101 :: "hi" :: Nil, Map("a1" -> 100, "a2" -> "hello", "a3" -> 0.5)) == "FUNC:101,hi,0.5,1.0,false")
//  }
//
//  test("Test message invoke.") {
//    val response = Message.newBuilder.asResponse(100, 100).create
//    intercept[IllegalArgumentException] {
//      runtimeInvoker.invoke(response)
//    }
//    val request1 = Message.newBuilder.asRequest("func", 101 :: "hi" :: Nil, Map("a1" -> 100, "a2" -> "hello", "a3" -> 0.5)).create
//    val response1 = runtimeInvoker.invoke(request1)
//    assert(response1.responseContent._1 == "FUNC:101,hi,0.5,1.0,false")
//    intercept[IllegalArgumentException] {
//      val request2 = Message.newBuilder.asRequest("func2", 101 :: "hi" :: Nil, Map("a1" -> 100, "a2" -> "hello", "a3" -> 0.5)).create
//      runtimeInvoker.invoke(request2)
//    }
//    intercept[IllegalArgumentException] {
//      val request3 = Message.newBuilder.asRequest("func", 101 :: 10 :: Nil, Map("a1" -> 100, "a2" -> "hello", "a3" -> 0.5)).create
//      runtimeInvoker.invoke(request3)
//    }
//    intercept[IllegalArgumentException] {
//      val request4 = Message.newBuilder.asRequest("func", 101 :: Nil, Map("a3" -> 0.5)).create
//      runtimeInvoker.invoke(request4)
//    }
//  }
//
//  class InvokerTestClass {
//    def run(a1: Int) = {
//      a1 match {
//        case a1 if a1 < 0 => {
//          throw new RuntimeException()
//        }
//        case _ => s"Method 1:$a1"
//      }
//    }
//
//    def run(a2: Int, b2: Double) = s"Method 2:$a2,$b2"
//
//    def run(a3: Int = 100, b3: Double = 1.0, c3: Seq[String], d3: Boolean) = {
//      val s: String = c3.head
//      s"Method 3:$a3,$b3,$c3,$d3"
//    }
//
//    def noParam = "No Param"
//
//    def func(a1: Int, a2: String, a3: Double = 0.1, a4: Float = 1, a5: Boolean = false) = s"FUNC:$a1,$a2,$a3,$a4,$a5"
//  }
//
//}
