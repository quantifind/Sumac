package optional

import java.lang.reflect.{Type, Field}

import scala.collection._

class ArgumentParser[T <: ArgAssignable] (val argHolders: Seq[T]) {
  lazy val nameToHolder = argHolders.map{ a => a.getName -> a}.toMap


  def parse(args: Array[String],
            preParsers: Iterator[Parser[_]] = Iterator(),
            postParsers: Iterator[Parser[_]] = Iterator()) : Map[T, ValueHolder[_]] = {
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
        result(holderOption.get) =
          ParseHelper.parseInto(args(idx +1), holderOption.get.getType, preParsers, postParsers).get
        idx += 2
      }
      result
    } catch {
      case exc => throw new ArgException(helpMessage, exc)
    }
  }

  def helpMessage = {
    val msg = StringBuilder.newBuilder
    msg.append("usage: \n")
    nameToHolder.foreach{ kv =>
      msg.append("--" + kv._1 + "\t" + kv._2.getType + "\n\n")
      //TODO add some way to include a usage message
    }
    msg.toString
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

class ArgException(val msg: String, val cause: Throwable) extends IllegalArgumentException(msg, cause) {
  def this(msg:String) = this(msg, null)
}