package com.quantifind.sumac.validation

import com.quantifind.sumac.ArgException

object RequiredCheck extends Function3[Any, Any, String, Unit] {

  def apply(defaultValue: Any, currentValue: Any, name: String) {
    if (defaultValue == currentValue)
      throw new ArgException("must specify a value for " + name)
  }

  override def toString() = getClass().getSimpleName
}


object PositiveCheck extends Function3[Any, Any, String, Unit] {
  def apply(defaultValue: Any, currentValue: Any, name: String) {
    val doubleVal = currentValue match {
      case i: Int => i.toDouble
      case l: Long => l.toDouble
      case f: Float => f.toDouble
      case d: Double => d
      case _ => 1.0
    }
    if (doubleVal <= 0.0)
      throw new ArgException("must specify a positive value for " + name)
  }

  override def toString() = getClass().getSimpleName
}