package optional

import java.lang.reflect.{Type,Field}
import scala.collection._

class ArgumentParser[T <: ArgAssignable](val argHolders: Seq[T]) {

  val nameToHolder = argHolders.map{h => h.getName -> h}.toMap

  def parse(args: Seq[String]) : Map[T, ValueHolder[_]] = {
    parse(args.toArray)
  }

  def parse(args: Array[String]) : Map[T, ValueHolder[_]] = {
    var idx = 0
    val result = mutable.Map[T, ValueHolder[_]]()
    while (idx < args.length) {
      val arg : String = args(idx)
      val argName =
        if (arg.startsWith("--"))
          arg.substring(2)
        else
          throw new RuntimeException("arg at position " + idx + " does not begin w/ --")
      val holder = nameToHolder.get(argName)
      if (holder.isEmpty)
        throw new RuntimeException("no place to put argument with name " + argName)
      result(holder.get) = ParseHelper.parseInto(args(idx + 1), holder.get.getType).get
      idx += 2
    }
    result
  }
}

trait ArgAssignable {
  def getName: String
  def getType: Type
}

class FieldArgAssignable(val field: Field) extends ArgAssignable {
  def getName = field.getName
  def getType = field.getGenericType
}