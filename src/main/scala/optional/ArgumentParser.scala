package optional

import java.lang.reflect.{Type, Field}

import scala.collection._

class ArgumentParser[T <: ArgAssignable] (val argHolders: Seq[T]) {
  lazy val nameToHolder = argHolders.map{ a => a.getName -> a}.toMap


  def parse(args: Array[String]) : Map[T, ValueHolder[_]] = {
    try {
      val result = mutable.Map[T, ValueHolder[_]]()
      var idx = 0
      while (idx < args.length) {
        val arg = args(idx)
        if (arg == "--help") {
          throw new ArgException(helpMessage)
        }
        if (!arg.startsWith("--"))
          throw new ArgException("expecting argument name beginning with \"--\", instead got " + arg + "\n" + helpMessage)
        val name = arg.substring(2)
        val holderOption = nameToHolder.get(name)
        if (holderOption.isEmpty)
          throw new ArgException("unknown option " + name + "\n" + helpMessage)
        try {
          val parsed = ParseHelper.parseInto(args(idx +1), holderOption.get.getType, holderOption.get.getCurrentValue)
          parsed match {
            case Some(x) => result(holderOption.get) = x
            case None => throw new ArgException("don't know how to parse type: " + holderOption.get.getType)
          }
        } catch {
          case exc => throw new ArgException("Error parsing \"" + args(idx + 1) + "\" into field \"" + name + "\" (type = " + holderOption.get.getType + ")\n" + helpMessage, exc)
        }
        idx += 2
      }
      result
    } catch {
      case ae: ArgException => throw ae
      case exc => throw new ArgException(helpMessage, exc)
    }
  }

  def helpMessage = {
    val msg = StringBuilder.newBuilder
    msg.append("usage: \n")
    nameToHolder.foreach{ kv =>
      msg.append("--" + kv._1 + "\t" + kv._2.getType + "\t" + kv._2.getDescription + "\n\n")
    }
    msg.toString
  }

}

object ArgumentParser {
  def apply[T <: ArgAssignable](argHolders: Traversable[T]) = {
    //ignore things we dont' know how to parse
    new ArgumentParser(argHolders.toSeq.filter{t => ParseHelper.findParser(t.getType).isDefined})
  }
}

trait ArgAssignable {
  def getName : String
  def getDescription: String
  def getType: Type
  def getCurrentValue: AnyRef
}


class FieldArgAssignable(val field: Field, val obj: Object) extends ArgAssignable {
  field.setAccessible(true)
  val annotationOpt = Option(field.getAnnotation(classOf[Arg]))
  def getName = {
    val n = annotationOpt.map{_.name()}.getOrElse(field.getName)
    if (n == "")
      field.getName
    else
      n
  }
  def getDescription = {
    val d = annotationOpt.map{_.description()}.getOrElse(field.getName)
    if (d == "")
      getName
    else
      d
  }
  def getType = field.getGenericType
  def getCurrentValue = field.get(obj)
}

class ArgException(val msg: String, val cause: Throwable) extends IllegalArgumentException(msg, cause) {
  def this(msg:String) = this(msg, null)
}