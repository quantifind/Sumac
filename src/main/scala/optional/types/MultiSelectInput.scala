package optional.types

class MultiSelectInput[T](var value: Set[T], val options: Set[T])

object MultiSelectInput {
  def apply[T](options: T*) = new MultiSelectInput[T](Set(), options.toSet)
}


