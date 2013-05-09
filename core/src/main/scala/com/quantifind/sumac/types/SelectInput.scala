package com.quantifind.sumac.types

import collection.mutable.LinkedHashSet
import collection.Set

class SelectInput[T](var value: Option[T], val options: Set[T])

object SelectInput{
  def apply[T](options: T*) = new SelectInput[T](value = None, options = (LinkedHashSet.empty ++ options))
}
