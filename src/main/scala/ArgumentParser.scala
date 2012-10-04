package optional

import java.lang.reflect.{Type, Field}

import scala.collection._

class ArgumentParser[T <: ArgAssignable] (val argHolders: Seq[T]) {
  lazy val nameToHolder = argHolders.map{ a => a.getName -> a}.toMap


  def parse(args: Array[String]) : Map[T, ValueHolder[_]] = {
    val result = mutable.Map[T, ValueHolder[_]]()
    var idx = 0
    while (idx < args.length) {
      val arg = args(idx)
      if (!arg.startsWith("--"))
        throw new RuntimeException("expecting argument name beginning with \"--\", instead got " + arg)
      val name = arg.substring(2)
      val holderOption = nameToHolder.get(name)
      if (holderOption.isEmpty)
        throw new RuntimeException("unknown option " + name)
      result(holderOption.get) =  ParseHelper.parseInto(args(idx +1), holderOption.get.getType).get
      idx += 2
    }
    result
  }

}

trait ArgAssignable {
  def getName : String
  def getType: Type
}


class FieldArgAssignable(val field: Field) extends ArgAssignable {
  def getName = field.getName
  def getType = field.getGenericType
}