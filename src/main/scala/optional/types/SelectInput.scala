package optional.types

class SelectInput[T](var value: Option[T], val options: mutable.LinkedHashSet[T])

object SelectInput{
  def apply[T](options: T*) = new OurSelectInput[T](value = None, options = (mutable.LinkedHashSet[T]() ++ options))
}
