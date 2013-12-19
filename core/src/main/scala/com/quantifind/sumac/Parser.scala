package com.quantifind.sumac

import types.{SelectInput, MultiSelectInput}
import java.lang.reflect.{Type, ParameterizedType}
import util.matching.Regex
import java.io.File
import scala.concurrent.duration.Duration
import scala.reflect.runtime.{universe => ru}

trait Parser[T] {
  def parse(s: String, tpe: ru.Type, currentValue: Any): T

  /**
   * return true if this parser knows how to parse the given type
   * @param tpe
   * @return
   */
  def canParse(tpe: ru.Type): Boolean

  def valueAsString(currentValue: Any): String = {
    if (currentValue == null)
      Parser.nullString
    else
      currentValue.toString
  }
}

object Parser {
  val nullString = "<null>"
}

trait SimpleParser[T] extends Parser[T] {
  val knownTypes: Set[ru.Type]

  def canParse(tpe: ru.Type) = {
    val r = knownTypes.find{_ =:= tpe}
    r.isDefined
  }

  def parse(s: String, tpe: ru.Type, currentValue: Any) = parse(s)

  def parse(s: String): T
}

trait CompoundParser[T] extends Parser[T]

object StringParser extends SimpleParser[String] {
  val knownTypes = Set(ru.typeOf[String])

  def parse(s: String) = {
    if (s == Parser.nullString)
      null
    else
      s
  }
}

/**
 * parse a duration, the format should be with a point between the number and the unit:
 * e.g.:   10.seconds
 * 20.minutes
 */
object DurationParser extends SimpleParser[Duration] {
  val knownTypes = Set(ru.typeOf[Duration])

  def parse(s: String) = {
    Duration(s.replace('.', ' '))
  }
}

object IntParser extends SimpleParser[Int] {
  val knownTypes = Set(ru.typeOf[Int], ru.typeOf[java.lang.Integer])

  def parse(s: String) = s.toInt
}

object LongParser extends SimpleParser[Long] {
  val knownTypes = Set(ru.typeOf[Long], ru.typeOf[java.lang.Long])

  def parse(s: String) = s.toLong
}

object BooleanParser extends SimpleParser[Boolean] {
  val knownTypes = Set(ru.typeOf[Boolean], ru.typeOf[java.lang.Boolean])

  def parse(s: String) = s.toBoolean
}

object FloatParser extends SimpleParser[Float] {
  val knownTypes = Set(ru.typeOf[Float], ru.typeOf[java.lang.Float])

  def parse(s: String) = s.toFloat
}

object DoubleParser extends SimpleParser[Double] {
  val knownTypes = Set(ru.typeOf[Double], ru.typeOf[java.lang.Double])

  def parse(s: String) = s.toDouble
}

object RegexParser extends SimpleParser[Regex] {
  val knownTypes = Set(ru.typeOf[Regex])

  def parse(s: String) = s.r
}

object FileParser extends SimpleParser[File] {
  val knownTypes = Set(ru.typeOf[File])

  def parse(s: String) = {
    val fullPath = if (s.startsWith("~")) s.replaceFirst("~", System.getProperty("user.home")) else s
    new File(fullPath)
  }
}

//TODO CompoundParser are both a pain to write, and extremely unsafe.  Design needs some work

//object OptionParser extends CompoundParser[Option[_]] {
//  def canParse(tpe: ru.Type) = {
//    ParseHelper.checkType(tpe, classOf[Option[_]])
//  }
//
//  def parse(s: String, tpe: ru.Type, currentValue: AnyRef) = {
//    //TODO
//    None
////    if (tpe.isInstanceOf[ParameterizedType]) {
////      val ptpe = tpe.asInstanceOf[ParameterizedType]
////      val subtype = ptpe.getActualTypeArguments()(0)
////      val subParser = ParseHelper.findParser(subtype).get
////      val x = subParser.parse(s, subtype, currentValue)
////      if (x == null) None else Some(x)
////      None
////    } else None
//  }
//}
//
//object ListParser extends CompoundParser[List[_]] {
//
//  def canParse(tpe: ru.Type) = {
//    ParseHelper.checkType(tpe, classOf[List[_]])
//  }
//
//  def parse(s: String, tpe: ru.Type, currentValue: AnyRef) = {
//    //TODO
//    None
////    if (tpe.isInstanceOf[ParameterizedType]) {
////      val ptpe = tpe.asInstanceOf[ParameterizedType]
////      val subtype = ptpe.getActualTypeArguments()(0)
////      val subParser = ParseHelper.findParser(subtype).get //TODO need to handle cases where its a list, but can't parse subtype
////      val parts = s.split(",")
////      parts.map(subParser.parse(_, subtype, currentValue)).toList
////    } else List.empty
//  }
//}
//
//object SetParser extends CompoundParser[collection.Set[_]] {
//  def canParse(tpe: ru.Type) = {
//    ParseHelper.checkType(tpe, classOf[collection.Set[_]])
//  }
//
//  def parse(s: String, tpe: ru.Type, currentValue: AnyRef) = {
//    //TODO
//    None
////    if (tpe.isInstanceOf[ParameterizedType]) {
////      val ptpe = tpe.asInstanceOf[ParameterizedType]
////      val subtype = ptpe.getActualTypeArguments()(0)
////      val subParser = ParseHelper.findParser(subtype).get
////      val parts = s.split(",")
////      parts.map(subParser.parse(_, subtype, currentValue)).toSet
////    } else Set.empty
//  }
//}
//
//object SelectInputParser extends CompoundParser[SelectInput[_]] {
//  def canParse(tpe: ru.Type) = {
//    ParseHelper.checkType(tpe, classOf[SelectInput[_]])
//  }
//
//  def parse(s: String, tpe: ru.Type, currentValue: AnyRef) = {
//    val currentVal = currentValue.asInstanceOf[SelectInput[Any]] //not really Any, but not sure how to make the compiler happy ...
//    if (tpe.isInstanceOf[ParameterizedType]) {
//      val ptpe = tpe.asInstanceOf[ParameterizedType]
//      val subtype = ptpe.getActualTypeArguments()(0)
//      val subParser = ParseHelper.findParser(subtype).get
//      val parsed = subParser.parse(s, subtype, currentVal.value)
//      if (currentVal.options(parsed)) currentVal.value = Some(parsed)
//      else throw new IllegalArgumentException(parsed + " is not the allowed values: " + currentVal.options)
//      //we don't return a new object, just modify the existing one
//      currentVal
//    } else throw new UnsupportedOperationException()
//  }
//}
//
//object MultiSelectInputParser extends CompoundParser[MultiSelectInput[_]] {
//  def canParse(tpe: ru.Type) = ParseHelper.checkType(tpe, classOf[MultiSelectInput[_]])
//
//  def parse(s: String, tpe: ru.Type, currentValue: AnyRef) = {
//    val currentVal = currentValue.asInstanceOf[MultiSelectInput[Any]] //not really Any, but not sure how to make the compiler happy ...
//    if (tpe.isInstanceOf[ParameterizedType]) {
//      val ptpe = tpe.asInstanceOf[ParameterizedType]
//      val subtype = ptpe.getActualTypeArguments()(0)
//      val subParser = ParseHelper.findParser(subtype).get
//      val parsed: Set[Any] = s.split(",").map(subParser.parse(_, subtype, "dummy")).toSet
//      val illegal = parsed.diff(currentVal.options)
//      if (illegal.isEmpty) currentVal.value = parsed
//      else throw new IllegalArgumentException(illegal.toString + " is not the allowed values: " + currentVal.options)
//      //we don't return a new object, just modify the existing one
//      currentVal
//    } else throw new UnsupportedOperationException()
//  }
//}

object ParseHelper {
  var parsers: Seq[Parser[_]] = Seq(
    StringParser,
    IntParser,
    LongParser,
    FloatParser,
    DoubleParser,
    BooleanParser,
//    OptionParser,
//    ListParser,
//    SetParser,
//    SelectInputParser,
//    MultiSelectInputParser,
    FileParser,
    RegexParser,
    DurationParser)

  def findParser(tpe: ru.Type): Option[Parser[_]] = parsers.find(_.canParse(tpe))

  def parseInto[T](s: String, tpe: ru.Type, currentValue: Any): Option[ValueHolder[T]] = {
    //could change this to be a map, at least for the simple types
    findParser(tpe).map(parser => ValueHolder[T](parser.parse(s, tpe, currentValue).asInstanceOf[T], tpe))
  }

  def checkType(tpe: ru.Type, targetClassSet: Class[_]*) = {
    def helper(tpe: ru.Type, targetCls: Class[_]) = {
      true
      //TODO
//      targetCls.isAssignableFrom(ReflectionUtils.getRawClass(tpe))
    }
    targetClassSet.exists(targetClass => helper(tpe, targetClass))
  }

  def registerParser[T](parser: Parser[T]) {
    synchronized {
      parsers ++= Seq(parser)
    }
  }
}

case class ValueHolder[T](value: T, tpe: ru.Type)
