package com.quantifind.sumac

import types.{SelectInput, MultiSelectInput}
import java.lang.reflect.{Type, ParameterizedType}
import util.matching.Regex
import java.io.File
import scala.concurrent.duration.Duration
import scala.collection._
import java.util.{GregorianCalendar, Calendar, TimeZone, Date}
import java.text.SimpleDateFormat
import scala.util.Try

trait Parser[T] {
  def parse(s: String, tpe: Type, currentValue: AnyRef): T

  /**
   * return true if this parser knows how to parse the given type
   * @param tpe
   * @return
   */
  def canParse(tpe: Type): Boolean

  def valueAsString(currentValue: AnyRef): String = {
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
  val knownTypes: Set[Class[_]]

  def canParse(tpe: Type) = {
    if (tpe.isInstanceOf[Class[_]]) knownTypes(tpe.asInstanceOf[Class[_]])
    else false
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef) = parse(s)

  def parse(s: String): T
}

trait CompoundParser[T] extends Parser[T]

object StringParser extends SimpleParser[String] {
  val knownTypes: Set[Class[_]] = Set(classOf[String])

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
  val knownTypes: Set[Class[_]] = Set(classOf[Duration])

  def parse(s: String) = {
    Duration(s.replace('.', ' '))
  }
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

object DateParser extends SimpleParser[Date] {
  val knownTypes: Set[Class[_]] = Set(classOf[Date])
  val utc = TimeZone.getTimeZone("UTC")
  val formats = Map(
    """\d{4}-\d{2}-\d{2}""".r -> "yyyy-MM-dd",
    """\d{4}/\d{2}/\d{2}""".r -> "yyyy/MM/dd",
    """\d{2}-\d{2}-\d{4}""".r -> "MM-dd-yyyy",
    """\d{2}/\d{2}/\d{4}""".r -> "MM/dd/yyyy"
  ).map{case(r,p) =>
    val fmt = new SimpleDateFormat(p)
    fmt.setTimeZone(utc)
    r -> fmt
  }

  def parse(s: String) = {
    formats.find{case(r,fmt) =>
      if (r.findFirstIn(s).isDefined) {
        val t = Try{fmt.parse(s)}
        t.isSuccess
      } else {
        false
      }
    } match {
      case Some((_,fmt)) =>
        fmt.parse(s)
      case None => throw new ArgException("no format found to parse \"" + s + "\" into Date")
    }
  }
}

object CalendarParser extends SimpleParser[Calendar] {
  val knownTypes: Set[Class[_]] = Set(classOf[Calendar])
  def parse(s: String) = {
    val d = DateParser.parse(s)
    val c = new GregorianCalendar(DateParser.utc)
    c.setTimeInMillis(d.getTime)
    c
  }

}

//TODO CompoundParser are both a pain to write, and extremely unsafe.  Design needs some work

object OptionParser extends CompoundParser[Option[_]] {
  def canParse(tpe: Type) = {
    ParseHelper.checkType(tpe, classOf[Option[_]])
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef) = {
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val subtype = ptpe.getActualTypeArguments()(0)
      val subParser = ParseHelper.findParser(subtype).get
      val x = subParser.parse(s, subtype, currentValue)
      if (x == null) None else Some(x)
    } else None
  }
}

object EnumParser extends CompoundParser[Enum[_]] {
  def canParse(tpe: Type) = {
    tpe match {
      case c:Class[_] =>
        c.isEnum
      case _ =>
        false
    }
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef) = {
    tpe match {
      case c: Class[_] =>
        val enums = c.getEnumConstants
        enums.find{_.toString() == s} match {
          case Some(x) => x.asInstanceOf[Enum[_]]
          case None =>
            throw new ArgException(s + " is not in set of enum values: " + enums.mkString(","))
        }
      case _ =>
        throw new RuntimeException("unexpected type in enum parser: " + tpe)
    }
  }
}

abstract class CollectionParser[T] extends CompoundParser[T] {
  def targetCollection: Class[T]
  def build(stuff: Any*): T
  def empty: T
  def canParse(tpe: Type) = {
    ParseHelper.checkType(tpe, targetCollection)
  }
  def parse(s: String, tpe: Type, currentValue: AnyRef) = {
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val subtype = ptpe.getActualTypeArguments()(0)
      val subParser = ParseHelper.findParser(subtype).get
      val parts = s.split(",")
      val sub: Seq[Any] = parts.map(subParser.parse(_, subtype, currentValue)).toSeq
      build(sub:_*)
    } else empty
  }
}

object ListParser extends CollectionParser[List[_]] {
  def targetCollection = classOf[List[_]]
  def build(stuff: Any*) = {
    stuff.toList
  }
  def empty = List()
}

object SetParser extends CollectionParser[Set[_]] {
  def targetCollection = classOf[Set[_]]
  def build(stuff: Any*) = {
    stuff.toSet
  }
  def empty = Set()
}

object ArrayParser extends CompoundParser[Array[_]] {
  override def canParse(tpe: Type) = {
    tpe match {
      case p: ParameterizedType =>
        false
      case c: Class[_] =>
        c.isArray
      case _ =>
        //not sure what else could be here, but should be false
        false
    }
  }
  override def parse(s: String, tpe: Type, currentValue: AnyRef) = {
    tpe match {
      case c:Class[_] =>
        val subtype = c.getComponentType
        val subParser = ParseHelper.findParser(subtype).get
        val parts = s.split(",")
        val sub: Array[Any] = parts.map(subParser.parse(_, subtype, currentValue))
        //toArray doesn't cut it here ... we end up trying to set Array[Object] on an Array[whatever], which reflection
        // doesn't like
        val o = java.lang.reflect.Array.newInstance(subtype, sub.size)
        (0 until sub.length).foreach{i => java.lang.reflect.Array.set(o, i, sub(i))}
        o.asInstanceOf[Array[_]]
      case _ =>
        throw new RuntimeException("unexpected type in array parser: " + tpe)
    }
  }
}

object SeqParser extends CollectionParser[Seq[_]] {
  def targetCollection = classOf[Seq[_]]
  def build(stuff: Any*) = {
    stuff.toSeq
  }
  def empty = Seq()
}

object VectorParser extends CollectionParser[Vector[_]] {
  def targetCollection = classOf[Vector[_]]
  def build(stuff: Any*) = {
    stuff.toVector
  }
  def empty = Vector()
}

object TraversableParser extends CollectionParser[Traversable[_]] {
  def targetCollection = classOf[Traversable[_]]
  def build(stuff: Any*) = {
    stuff.toTraversable
  }
  def empty = Traversable()
}

object MapParser extends CompoundParser[Map[_,_]] {
  def canParse(tpe: Type) = {
    ParseHelper.checkType(tpe, classOf[Map[_,_]])
  }
  def parse(s: String, tpe: Type, currentValue: AnyRef): Map[_,_] = {
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val keyType = ptpe.getActualTypeArguments()(0)
      val keyParser = ParseHelper.findParser(keyType).get
      val valueType = ptpe.getActualTypeArguments()(1)
      val valueParser = ParseHelper.findParser(valueType).get
      val parts = s.split(",")
      val r = parts.map{p =>
        //should we trim here, or keep the whitespace?  for now I'll keep the whitespace ...
        val kv = p.split(":")
        if (kv.length != 2) {
          throw new ArgException("maps expect a list of kv pairs, with each key separated from value by \":\" and pairs separated by \",\"")
        }
        val k = keyParser.parse(kv(0), keyType, currentValue)
        val v = valueParser.parse(kv(1), valueType, currentValue)
        k -> v
      }.toMap
      r
    } else Map()
  }

}


object SelectInputParser extends CompoundParser[SelectInput[_]] {
  def canParse(tpe: Type) = {
    ParseHelper.checkType(tpe, classOf[SelectInput[_]])
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef) = {
    val currentVal = currentValue.asInstanceOf[SelectInput[Any]] //not really Any, but not sure how to make the compiler happy ...
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
    val currentVal = currentValue.asInstanceOf[MultiSelectInput[Any]] //not really Any, but not sure how to make the compiler happy ...
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val subtype = ptpe.getActualTypeArguments()(0)
      val subParser = ParseHelper.findParser(subtype).get
      val parsed: Set[Any] = s.split(",").map(subParser.parse(_, subtype, "dummy")).toSet
      val illegal = parsed.diff(currentVal.options)
      if (illegal.isEmpty) currentVal.value = parsed
      else throw new IllegalArgumentException(illegal.toString + " is not the allowed values: " + currentVal.options)
      //we don't return a new object, just modify the existing one
      currentVal
    } else throw new UnsupportedOperationException()
  }
}

object ParseHelper {
  var parsers: Seq[Parser[_]] = Seq(
    StringParser,
    IntParser,
    LongParser,
    FloatParser,
    DoubleParser,
    BooleanParser,
    FileParser,
    RegexParser,
    DurationParser,
    DateParser,
    CalendarParser,
    EnumParser,

  //collections
    OptionParser,
    ListParser,
    SetParser,
    ArrayParser,
    VectorParser,
    SeqParser,
    MapParser,
    TraversableParser,  //order matters, be sure this at the end

  //special collections
    SelectInputParser,
    MultiSelectInputParser
  )

  def findParser(tpe: Type): Option[Parser[_]] = parsers.find(_.canParse(tpe))

  def parseInto[T](s: String, tpe: Type, currentValue: AnyRef): Option[ValueHolder[T]] = {
    //could change this to be a map, at least for the simple types
    findParser(tpe).map(parser => ValueHolder[T](parser.parse(s, tpe, currentValue).asInstanceOf[T], tpe))
  }

  def checkType(tpe: Type, targetClassSet: Class[_]*) = {
    def helper(tpe: Type, targetCls: Class[_]) = {
      targetCls.isAssignableFrom(ReflectionUtils.getRawClass(tpe))
    }
    targetClassSet.exists(targetClass => helper(tpe, targetClass))
  }

  def registerParser[T](parser: Parser[T]) {
    synchronized {
      parsers ++= Seq(parser)
    }
  }
}

case class ValueHolder[T](value: T, tpe: Type)
