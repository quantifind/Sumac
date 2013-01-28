package optional

import scala.annotation.tailrec
import java.lang.reflect.{Type, Field}

class ArgumentParser[T <: ArgAssignable] (val argHolders: Seq[T]) {
  lazy val nameToHolder = argHolders.map(a => a.getName -> a).toMap.withDefault { arg =>
    throw new ArgException("unknown option %s\n%s".format(arg, helpMessage))
  }

  def parse(args: Array[String]): Map[T, ValueHolder[_]] = {
    @tailrec
    def parse(args: List[String], acc: Map[T, ValueHolder[_]] = Map.empty): Map[T, ValueHolder[_]] = {
      args match {
        case Nil => acc
        case "--help" :: _ => throw new ArgException(helpMessage)
        case x :: _ if (!x.startsWith("--")) =>
          throw new ArgException("expecting argument name beginning with \"--\", instead got %s".format(x))
        case name :: value :: tail =>
          val suffix = name.drop(2)
          val holder = nameToHolder(suffix)
          val result = try {
            ParseHelper.parseInto(value, holder.getType, holder.getCurrentValue) getOrElse {
              throw new ArgException("don't know how to parse type: " + holder.getType)
            }
          } catch {
            case ae: ArgException => throw ae
            case e: Throwable => throw new ArgException("Error parsing \"%s\" into field \"%s\" (type = %s)\n%s".format(value, suffix, holder.getType, helpMessage))
          }

          // parse remaining options
          parse(tail, acc + (holder -> result))
      }
    }

    try {
      parse(args.toList)
    } catch {
      case ae: ArgException => throw ae
      case e: Throwable => throw new ArgException(helpMessage, e)
    }
  }

  def helpMessage: String = {
    val msg = new StringBuilder
    msg.append("usage: \n")
    nameToHolder.foreach { case (k, v) =>
      msg.append("--%s\t%s\t%s\n\n".format(k, v.getType, v.getDescription))
    }
    msg.toString
  }
}

object ArgumentParser {
  def apply[T <: ArgAssignable](argHolders: Traversable[T]) = {
    // ignore things we don't know how to parse
    new ArgumentParser(argHolders.toSeq.filter(t => ParseHelper.findParser(t.getType).isDefined))
  }
}

/**
 * Container for one argument, that has name, type, and can be assigned a value.
 */
trait ArgAssignable {
  def getName: String
  def getDescription: String
  def getType: Type
  def getCurrentValue: AnyRef
  def setValue(value: Any)
}

class FieldArgAssignable(val field: Field, val obj: Object) extends ArgAssignable {
  field.setAccessible(true)
  val annotationOpt = Option(field.getAnnotation(classOf[Arg]))

  def getName = {
    val n = annotationOpt.map(_.name).getOrElse(field.getName)
    if (n == "") field.getName else n
  }

  def getDescription = {
    val d = annotationOpt.map(_.description).getOrElse(field.getName)
    if (d == "") getName else d
  }

  def getType = field.getGenericType
  def getCurrentValue = field.get(obj)

  def setValue(value: Any) = {
    field.set(obj, value)
  }
}

case class ArgException(msg: String, cause: Throwable) extends IllegalArgumentException(msg, cause) {
  def this(msg: String) = this(msg, null)
}