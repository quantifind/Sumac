package com.quantifind.sumac

import types.{SelectInput,MultiSelectInput}
import java.lang.reflect.{Type, ParameterizedType}
import util.matching.Regex
import java.io.File

trait Parser[T] {
  def parse(s: String, tpe: Type, currentValue: AnyRef): T

  /**
   * return true if this parser knows how to parse the given type
   * @param tpe
   * @return
   */
  def canParse(tpe: Type): Boolean
}

trait SimpleParser[T] extends Parser[T] {
  val knownTypes: Set[Class[_]]
  def canParse(tpe: Type) = {
    if (tpe.isInstanceOf[Class[_]]) knownTypes(tpe.asInstanceOf[Class[_]])
    else false
  }
  def parse(s: String, tpe:Type, currentValue: AnyRef) = parse(s)
  def parse(s: String): T
}

trait CompoundParser[T] extends Parser[T]

object StringParser extends SimpleParser[String] {
  val knownTypes: Set[Class[_]] = Set(classOf[String])
  def parse(s: String) = s
}

object IntParser extends SimpleParser[Int] {
  val knownTypes: Set[Class[_]] = Set(classOf[Int], classOf[java.lang.Integer])
  def parse(s: String) = s.toInt
}

object LongParser extends SimpleParser[Long] {
  val knownTypes: Set[Class[_]] = Set(classOf[Long], classOf[java.lang.Long])
  def parse(s: String) = s.toLong
}

object BooleanParser extends SimpleParser[Boolean] {
  val knownTypes: Set[Class[_]] = Set(classOf[Boolean], classOf[java.lang.Boolean])
  def parse(s: String) = s.toBoolean
}

object FloatParser extends SimpleParser[Float] {
  val knownTypes: Set[Class[_]] = Set(classOf[Float], classOf[java.lang.Float])
  def parse(s: String) = s.toFloat
}

object DoubleParser extends SimpleParser[Double] {
  val knownTypes: Set[Class[_]] = Set(classOf[Double], classOf[java.lang.Double])
  def parse(s: String) = s.toDouble
}

object RegexParser extends SimpleParser[Regex] {
  val knownTypes: Set[Class[_]] = Set(classOf[Regex])
  def parse(s: String) = s.r
}

object FileParser extends SimpleParser[File] {
  val knownTypes: Set[Class[_]] = Set(classOf[File])
  def parse(s: String) = {
    val fullPath = if (s.startsWith("~")) s.replaceFirst("~", System.getProperty("user.home")) else s
    new File(fullPath)
  }
}

//TODO CompoundParser are both a pain to write, and extremely unsafe.  Design needs some work

object ListParser extends CompoundParser[List[_]] {

  def canParse(tpe: Type) = {
    ParseHelper.checkType(tpe, classOf[List[_]])
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef) = {
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val subtype = ptpe.getActualTypeArguments()(0)
      val subParser = ParseHelper.findParser(subtype).get //TODO need to handle cases where its a list, but can't parse subtype
      val parts = s.split(",")
      parts.map(subParser.parse(_, subtype, currentValue)).toList
    } else List.empty
  }
}

object SetParser extends CompoundParser[collection.Set[_]] {
  def canParse(tpe: Type) = {
    ParseHelper.checkType(tpe, classOf[collection.Set[_]])
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef) = {
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val subtype = ptpe.getActualTypeArguments()(0)
      val subParser = ParseHelper.findParser(subtype).get
      val parts = s.split(",")
      parts.map(subParser.parse(_, subtype, currentValue)).toSet
    } else Set.empty
  }
}

object SelectInputParser extends CompoundParser[SelectInput[_]] {
  def canParse(tpe: Type) = {
    ParseHelper.checkType(tpe, classOf[SelectInput[_]])
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef) = {
    val currentVal = currentValue.asInstanceOf[SelectInput[Any]]  //not really Any, but not sure how to make the compiler happy ...
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val subtype = ptpe.getActualTypeArguments()(0)
      val subParser = ParseHelper.findParser(subtype).get
      val parsed = subParser.parse(s, subtype, currentVal.value)
      if (currentVal.options(parsed)) currentVal.value = Some(parsed)
      else throw new IllegalArgumentException(parsed + " is not the allowed values: " + currentVal.options)
      //we don't return a new object, just modify the existing one
      currentVal
    } else throw new UnsupportedOperationException()
  }
}

object MultiSelectInputParser extends CompoundParser[MultiSelectInput[_]] {
  def canParse(tpe: Type) = ParseHelper.checkType(tpe, classOf[MultiSelectInput[_]])

  def parse(s: String, tpe: Type, currentValue: AnyRef) = {
    val currentVal = currentValue.asInstanceOf[MultiSelectInput[Any]]  //not really Any, but not sure how to make the compiler happy ...
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val subtype = ptpe.getActualTypeArguments()(0)
      val subParser = ParseHelper.findParser(subtype).get
      val parsed : Set[Any] = s.split(",").map(subParser.parse(_, subtype, "dummy")).toSet
      val illegal = parsed.diff(currentVal.options)
      if (illegal.isEmpty) currentVal.value = parsed
      else throw new IllegalArgumentException(illegal.toString + " is not the allowed values: " + currentVal.options)
      //we don't return a new object, just modify the existing one
      currentVal
    } else throw new UnsupportedOperationException()
  }
}

object ParseHelper {
  val parsers = Seq(
    StringParser,
    IntParser,
    LongParser,
    FloatParser,
    DoubleParser,
    BooleanParser,
    ListParser,
    SetParser,
    SelectInputParser,
    MultiSelectInputParser,
    FileParser,
    RegexParser)

  def findParser(tpe: Type): Option[Parser[_]] = parsers.find(_.canParse(tpe))

  def parseInto[T](s: String, tpe: Type, currentValue: AnyRef) : Option[ValueHolder[T]] = {
    //could change this to be a map, at least for the simple types
    findParser(tpe).map(parser => ValueHolder[T](parser.parse(s, tpe, currentValue).asInstanceOf[T], tpe))
  }

  def checkType(tpe: Type, targetClassSet: Class[_]*) = {
    def helper(tpe: Type, targetCls: Class[_]) = {
      val clz = if (tpe.isInstanceOf[Class[_]])
        tpe.asInstanceOf[Class[_]]
      else if (tpe.isInstanceOf[ParameterizedType])
        tpe.asInstanceOf[ParameterizedType].getRawType.asInstanceOf[Class[_]]
      else
        classOf[Int]  //just need something that won't match
      targetCls.isAssignableFrom(clz)
    }
    targetClassSet.exists(targetClass => helper(tpe, targetClass))
  }
}

case class ValueHolder[T](value: T, tpe: Type)
