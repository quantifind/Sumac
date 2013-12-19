package com.quantifind.sumac

import scala.annotation.tailrec
import collection.mutable.LinkedHashMap
import collection._
import scala.reflect.runtime.{universe => ru}

class ArgumentParser[T <: ArgAssignable] (val argHolders: Seq[T]) {
  lazy val nameToHolder:Map[String,T] = (LinkedHashMap.empty ++ argHolders.map(a => a.getName -> a)).withDefault { arg =>
    throw new ArgException("unknown option %s\n%s".format(arg, helpMessage))
  }

  def parse(args: Array[String]): Map[T, ValueHolder[_]] = {
    parse(ArgumentParser.argListToKvMap(args))
  }

  def parse(rawKvs: Map[String,String]): Map[T, ValueHolder[_]] = {
    if (rawKvs.contains("help"))
      throw new ArgException(helpMessage)
    rawKvs.map{case(argName, argValue) =>
      val holder = nameToHolder(argName)
      val result = try {
        ParseHelper.parseInto(argValue, holder.getType, holder.getCurrentValue) getOrElse {
          throw new ArgException("don't know how to parse type: " + holder.getType)
        }
      } catch {
        case ae: ArgException => throw ae
        case e: Throwable => throw new ArgException("Error parsing \"%s\" into field \"%s\" (type = %s)\n%s".format(argValue, argName, holder.getType, helpMessage))
      }
      holder -> result
    }
  }

  def helpMessage: String = {
    val msg = new StringBuilder
    msg.append("usage: \n")
    nameToHolder.foreach { case (k, v) =>
      msg.append(v.toString() + "\n")
    }
    msg.toString
  }
}

object ArgumentParser {
  def apply[T <: ArgAssignable](argHolders: Traversable[T]) = {
    // ignore things we don't know how to parse
    new ArgumentParser(argHolders.toSeq.filter(t => ParseHelper.findParser(t.getType).isDefined))
  }

  def argListToKvMap(args: Array[String]): Map[String,String] = {
    @tailrec
    def parse(args: List[String], acc: mutable.Map[String, String] = mutable.Map.empty): mutable.Map[String,String] = {
      args match {
        case Nil => acc
        case "--help" :: _ =>
          acc("help") = null
          acc
        case arg :: _ if (!arg.startsWith("--")) =>
          throw new ArgException("expecting argument name beginning with \"--\", instead got %s".format(arg))
        case name :: value :: tail =>
          val suffix = name.drop(2)
          acc(suffix) = value
          parse(tail, acc)
        case _ => throw new ArgException("gave a non-key value argument")
      }
    }
    parse(args.toList)
  }
}

/**
 * Container for one argument, that has name, type, and can be assigned a value.
 */
trait ArgAssignable {
  def getName: String
  def getDescription: String
  def getType: ru.Type
  def getCurrentValue: Any
  def getParser: Parser[_]
  def setValue(value: Any)
  override def toString() = {
    var t = "--" + getName + "\t" + getType
    if (getDescription != getName)
      t += "\t" + getDescription
    t += "\t" + getCurrentValue
    t
  }
}

class TermArgAssignable(val prefix: String, val field: ru.TermSymbol, val obj: Object, val parser: Parser[_]) extends ArgAssignable {

  val annotationOpt: Option[Arg] = None  //TODO
  def getParser = parser

  private def fName = field.name.toString.trim  //for some crazy reason, vars have an extra space at the end of their name

  def getName = {
    prefix + {
      val n = annotationOpt.map(_.name).getOrElse(fName)
      if (n == "") fName else n
    }
  }

  def getDescription = {
    val d = annotationOpt.map(_.description).getOrElse(getName)
    if (d == "") getName else d
  }

  def getType = field.typeSignature
  val mirror = ru.runtimeMirror(getClass.getClassLoader).reflect(obj).reflectField(field)
  def getCurrentValue = mirror.get

  def setValue(value: Any) = {
    mirror.set(value)
  }
}

object TermArgAssignable {
  def apply(prefix: String, field: ru.TermSymbol, obj: Object): TermArgAssignable = {
    val tpe = field.typeSignature
    val parser = ParseHelper.findParser(tpe) getOrElse {
      throw new ArgException("don't know how to parse type: " + tpe)
    }
    val r = new TermArgAssignable(prefix, field, obj, parser)
    r
  }
}



case class ArgException(msg: String, cause: Throwable) extends IllegalArgumentException(msg, cause) {
  def this(msg: String) = this(msg, null)
}
