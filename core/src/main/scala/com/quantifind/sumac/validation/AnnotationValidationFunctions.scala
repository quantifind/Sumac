package com.quantifind.sumac.validation

import com.quantifind.sumac.ArgException
import java.lang.annotation.Annotation

object RequiredCheck extends Function4[Any, Any, Annotation, String, Unit] {

  def apply(defaultValue: Any, currentValue: Any, annot: Annotation, name: String) {
    if (defaultValue == currentValue)
      throw new ArgException("must specify a value for " + name)
  }

  override def toString() = getClass().getSimpleName
}


object PositiveCheck extends Function4[Any, Any, Annotation, String, Unit] {
  def apply(defaultValue: Any, currentValue: Any, annot: Annotation, name: String) {
    numericAsDouble(currentValue){v =>
      if (v <= 0.0)
        throw new ArgException("must specify a positive value for " + name)
    }
  }

  def numericAsDouble[T](v: Any)(f: Double => T): Option[T] = {
    v match {
      case i: Int => Some(f(i.toDouble))
      case l: Long => Some(f(l.toDouble))
      case fl: Float => Some(f(fl.toDouble))
      case d: Double => Some(f(d))
      case _ => None
    }
  }

  override def toString() = getClass().getSimpleName
}

object RangeCheck extends Function4[Any, Any, Annotation, String, Unit] {
  def apply(defaultValue: Any, currentValue: Any, annot: Annotation, name: String) {
    annot match {
      case r:Range =>
        PositiveCheck.numericAsDouble(currentValue){v =>
          if (v < r.min() || v > r.max())
            throw new ArgException("must specify a value between " + r.min() + " and " + r.max() + " for " + name)
        }
      case _ => ()
    }
  }
}