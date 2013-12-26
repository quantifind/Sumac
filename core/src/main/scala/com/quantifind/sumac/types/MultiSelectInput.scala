package com.quantifind.sumac.types

import scala.collection._

class MultiSelectInput[T](var value: Set[T], val options: Set[T])

object MultiSelectInput {
  def apply[T](options: T*) = new MultiSelectInput[T](Set(), options.toSet)
}


