package com.hydra.core

import reflect.runtime.currentMirror
import reflect.runtime.universe._
import com.hydra.core.MessageType._

class RuntimeInvoker(invoker: Any) {
  require(invoker != null)
  private val members = currentMirror.classSymbol(invoker.getClass).toType.members.collect {
    case m: MethodSymbol => m
  }
  private val instanceMirror = currentMirror.reflect(invoker)
  private val typeSignature = instanceMirror.symbol.typeSignature

  def invoke(message: Message): Message = {
    if (message.messageType != Request) throw new IllegalArgumentException("The message must be a Request.")
    val (name, args, kwargs) = message.requestContent
    val result = invoke(name, args, kwargs)
    message.response(result)
  }

  def invoke(name: String, args: List[Any] = Nil, kwargs: Map[String, Any] = Map()): Any = {
    require(name != null && args != null && kwargs != null)
    val methods = members.collect {
      case m if (m.name.toString == name) => matchParameters(m, args, kwargs)
    }
    if (methods.size == 0) throw new IllegalArgumentException(s"Method not found: ${name}.")
    val valids = methods.collect {
      case m: Option[(Any, Any)] if m != None => m
    }
    valids.size match {
      case 0 => throw new IllegalArgumentException("No matched method.")
      case _ => {
        doInvoke(valids.head.get._1, valids.head.get._2)
      }
    }
  }

  private def matchParameters(method: MethodSymbol, args: List[Any], kwargs: Map[String, Any]) = {
    val paramInfo = (for (ps <- method.paramLists; p <- ps) yield p).zipWithIndex.map({ p =>
      (p._1.name.toString, p._1.asTerm.isParamWithDefault,
        typeSignature member TermName(s"${method.name}$$default$$${p._2 + 1}") match {
          case defarg if defarg != NoSymbol => Some((instanceMirror reflectMethod defarg.asMethod) ())
          case _ => None
        })
    }).drop(args.size)
    val paramMatches = paramInfo.collect {
      case info if kwargs.contains(info._1) => Some(kwargs(info._1))
      case info if info._2 => info._3
      case info => None
    }
    paramMatches.contains(None) match {
      case true => None
      case false => Some((method, args ::: paramMatches.map(p => p.get)))
    }
  }

  private def doInvoke(method: MethodSymbol, args: List[Any]): Any = {
    val methodInstance = instanceMirror.reflectMethod(method)
    methodInstance(args: _*)
  }
}