package com.hydra.`type`

object Type {

}


object NumberTypeConversions {
  implicit def anyToLong(x: Any) = {
    x match {
      case x: Byte => x.toLong
      case x: Short => x.toLong
      case x: Int => x.toLong
      case x: Long => x.toLong
      case _ => throw new IllegalArgumentException(s"$x can not be cast to Long.")
    }
  }

  implicit def anyToInt(x: Any) = {
    x match {
      case x: Byte => x.toInt
      case x: Short => x.toInt
      case x: Int => x.toInt
      case x: Long => x.toInt
      case _ => throw new IllegalArgumentException(s"$x can not be cast to Int.")
    }
  }

  implicit def anyToDouble(x: Any) = {
    x match {
      case x: Byte => x.toDouble
      case x: Short => x.toDouble
      case x: Int => x.toDouble
      case x: Long => x.toDouble
      case x: Float => x.toDouble
      case x: Double => x.toDouble
      case _ => throw new IllegalArgumentException(s"$x can not be cast to Int.")
    }
  }

}