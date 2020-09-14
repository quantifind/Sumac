/**
  * Copyright 2012-2020 Quantifind, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */

package com.quantifind.sumac

import types.{SelectInput, MultiSelectInput}
import java.lang.reflect.{Type, ParameterizedType}
import util.matching.Regex
import java.io.File
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.collection._
import java.util.{GregorianCalendar, Calendar, TimeZone, Date}
import java.text.SimpleDateFormat
import scala.util.Try
import java.util.concurrent.TimeUnit

trait Parser[T] {
  def parse(s: String, tpe: Type, currentValue: AnyRef, parsers: Seq[Parser[_]]): T

  /**
   * return true if this parser knows how to parse the given type
   * @param tpe
   * @return
   */
  def canParse(tpe: Type): Boolean

  def valueAsString(currentValue: AnyRef, tpe: Type, parsers: Seq[Parser[_]]): String = {
    if (currentValue == null)
      Parser.nullString
    else
      currentValue.toString
  }

  def allowedValues(tpe: Type, currentValue: AnyRef): Option[Set[String]] = None
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

  def parse(s: String, tpe: Type, currentValue: AnyRef, parsers: Seq[Parser[_]]): T = parse(s)

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

object FiniteDurationParser extends SimpleParser[FiniteDuration] {
  val knownTypes: Set[Class[_]] = Set(classOf[FiniteDuration])

  def parse(s: String) = {
    val maybeFinite = DurationParser.parse(s)
    if (maybeFinite.isFinite) FiniteDuration(maybeFinite.toNanos, TimeUnit.NANOSECONDS)
    else throw new IllegalArgumentException(s"'$s' is not a finite duration")
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
    val fullPath = if (s.startsWith("~")) System.getProperty("user.home")+s.drop(1) else s
    new File(fullPath)
  }
}

class DateParser(val fmts:Map[Regex,String], zone: TimeZone = TimeZone.getTimeZone("UTC")) extends Parser[AnyRef] {
  val knownTypes: Set[Class[_]] = Set(classOf[Date], classOf[Calendar])
  def canParse(tpe: Type) = {
    if (tpe.isInstanceOf[Class[_]]) {
      val tc = tpe.asInstanceOf[Class[_]]
      knownTypes.exists{ _ == tc}
    }
    else false
  }
  val formats = fmts.map {
    case (r, p) =>
      val fmt = new SimpleDateFormat(p)
      fmt.setTimeZone(zone)
      r -> fmt
  }

  def parse(s: String, tpe: Type, currentVal: AnyRef, parsers: Seq[Parser[_]]): AnyRef = {
    val d = parseDate(s)
    tpe match {
      case dc:Class[_] if dc.isAssignableFrom(classOf[Date]) =>
        d
      case cc:Class[_] if cc.isAssignableFrom(classOf[Calendar]) =>
        val c = new GregorianCalendar(zone)
        c.setTimeInMillis(d.getTime)
        c
    }
  }

  def parseDate(s:String) = {
    formats.find {
      case (r, fmt) =>
        if (r.findFirstIn(s).isDefined) {
          val t = Try {
            fmt.synchronized {
              fmt.parse(s)
            }
          }
          t.isSuccess
        } else {
          false
        }
    } match {
      case Some((_, fmt)) =>
        fmt.synchronized {
          fmt.parse(s)
        }
      case None => throw new FeedbackException("no format found to parse \"" + s + "\" into Date")
    }
  }

  private val stdFormat = {
    val f = new SimpleDateFormat("yyyy-MM-dd")
    f.setTimeZone(zone)
    f
  }

  override def valueAsString(v: AnyRef, t: Type, parsers: Seq[Parser[_]]): String = {
    v match {
      case d: Date =>
        stdFormat.format(d)
      case c: Calendar =>
        stdFormat.format(c.getTime())
    }
  }
}

object DateTimeFormats {
  val usFormats =
    Map(
      """\d{4}-\d{2}-\d{2}""".r -> "yyyy-MM-dd",
      """\d{4}/\d{2}/\d{2}""".r -> "yyyy/MM/dd",
      """\d{2}-\d{2}-\d{4}""".r -> "MM-dd-yyyy",
      """\d{2}/\d{2}/\d{4}""".r -> "MM/dd/yyyy"
    )

  val stdFormats =
    Map(
      """\d{4}-\d{2}-\d{2}""".r -> "yyyy-MM-dd",
      """\d{4}/\d{2}/\d{2}""".r -> "yyyy/MM/dd",
      """\d{2}-\d{2}-\d{4}""".r -> "dd-MM-yyyy",
      """\d{2}/\d{2}/\d{4}""".r -> "dd/MM/yyyy"
    )
}

object USDateParser extends DateParser(DateTimeFormats.usFormats)

object StandardDateParser extends DateParser(DateTimeFormats.stdFormats)

//TODO CompoundParser are both a pain to write, and extremely unsafe.  Design needs some work

object OptionParser extends CompoundParser[Option[_]] {
  def canParse(tpe: Type) = {
    ParseHelper.checkType(tpe, classOf[Option[_]])
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef, parsers: Seq[Parser[_]]) = {
    if (tpe.isInstanceOf[ParameterizedType]) {
      val (subtype, subParser) = ParseHelper.getSubParser(tpe, parsers)
      val x = subParser.parse(s, subtype, currentValue, parsers)
      if (x == null) None else Some(x)
    } else None
  }

  override def valueAsString(v: AnyRef, tpe: Type, parsers: Seq[Parser[_]]): String = {
    v match {
      case Some(s) =>
        val (subtype, subParser) = ParseHelper.getSubParser(tpe, parsers)
        subParser.valueAsString(s.asInstanceOf[AnyRef],subtype, parsers)
      case None =>
        Parser.nullString
    }
  }
}

object EnumParser extends CompoundParser[Enum[_]] {
  def canParse(tpe: Type) = {
    tpe match {
      case c: Class[_] =>
        c.isEnum
      case _ =>
        false
    }
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef, parsers: Seq[Parser[_]]) = {
    tpe match {
      case c: Class[_] =>
        val enums = c.getEnumConstants
        enums.find {
          _.toString() == s
        } match {
          case Some(x) => x.asInstanceOf[Enum[_]]
          case None =>
            throw new FeedbackException(s + " is not in set of enum values: " + enums.mkString(","))
        }
      case _ =>
        throw new RuntimeException("unexpected type in enum parser: " + tpe)
    }
  }

  override def allowedValues(tpe: Type, currentValue: AnyRef): Option[Set[String]] = {
    tpe match {
      case c: Class[_] =>
        Some(c.getEnumConstants.map{_.asInstanceOf[Enum[_]].name}.toSet)
      case _ =>
        throw new RuntimeException("unexpected type in enum parser: " + tpe)
    }
  }

}

abstract class CollectionParser[T <: Traversable[_]] extends CompoundParser[T] {
  def targetCollection: Class[T]

  def build(stuff: Any*): T

  def empty: T

  def canParse(tpe: Type) = {
    ParseHelper.checkType(tpe, targetCollection)
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef, parsers: Seq[Parser[_]]) = {
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val subtype = ptpe.getActualTypeArguments()(0)
      val subParser = ParseHelper.findParser(subtype, parsers).get
      val parts = CollectionCombinatorParser(s)
      val sub: Seq[Any] = parts.map(subParser.parse(_, subtype, currentValue, parsers))
      build(sub: _*)
    } else empty
  }

  override def valueAsString(v: AnyRef, tpe: Type, parsers: Seq[Parser[_]]):String = {
    (v,tpe) match {
      case (t: Traversable[_],ptpe: ParameterizedType) =>
        val (subtype, subparser) = ParseHelper.getSubParser(tpe, parsers)
        t.map{x =>
            val value = subparser.valueAsString(x.asInstanceOf[AnyRef], subtype, parsers)
            if(value.contains(",")) s""""$value""""
            else value
        }.mkString(",")
    }
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

  override def parse(s: String, tpe: Type, currentValue: AnyRef, parsers: Seq[Parser[_]]) = {
    tpe match {
      case c: Class[_] =>
        val subtype = c.getComponentType
        val subParser = ParseHelper.findParser(subtype, parsers).get
        val parts = CollectionCombinatorParser(s).toArray
        val sub: Array[Any] = parts.map(subParser.parse(_, subtype, currentValue, parsers))
        //toArray doesn't cut it here ... we end up trying to set Array[Object] on an Array[whatever], which reflection
        // doesn't like
        val o = java.lang.reflect.Array.newInstance(subtype, sub.size)
        (0 until sub.length).foreach {
          i => java.lang.reflect.Array.set(o, i, sub(i))
        }
        o.asInstanceOf[Array[_]]
      case _ =>
        throw new RuntimeException("unexpected type in array parser: " + tpe)
    }
  }

  override def valueAsString(v: AnyRef, tpe: Type, parsers: Seq[Parser[_]]):String = {
    tpe match {
      case c: Class[_] =>
        val subtype = c.getComponentType
        val subParser = ParseHelper.findParser(subtype, parsers).get
        v.asInstanceOf[Array[AnyRef]].map{ v =>
          val value = subParser.valueAsString(v, subtype, parsers)
          if(value.contains(",")) s""""$value""""
          else value
        }.mkString(",")
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

object MapParser extends CompoundParser[Map[_, _]] {
  def canParse(tpe: Type) = {
    ParseHelper.checkType(tpe, classOf[Map[_, _]])
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef, parsers: Seq[Parser[_]]): Map[_, _] = {
    if (tpe.isInstanceOf[ParameterizedType]) {
      val (keyType, keyParser) = ParseHelper.getSubParser(tpe, parsers, 0)
      val (valueType, valueParser) = ParseHelper.getSubParser(tpe, parsers, 1)
      MapCombinatorParser(s) map {
        case (key, value) =>
          val k = keyParser.parse(key, keyType, currentValue, parsers)
          val v = valueParser.parse(value, valueType, currentValue, parsers)
          k -> v
      }
    } else Map()
  }

  override def valueAsString(v: AnyRef, tpe: Type, parsers: Seq[Parser[_]]):String = {
    (v,tpe) match {
      case (t: Map[_,_],ptpe: ParameterizedType) =>
        val (keyType, keyParser) = ParseHelper.getSubParser(tpe, parsers, 0)
        val (valueType, valueParser) = ParseHelper.getSubParser(tpe, parsers, 1)
        t.map{case(k,v) =>
          val key = keyParser.valueAsString(k.asInstanceOf[AnyRef],keyType, parsers)
          val value = valueParser.valueAsString(v.asInstanceOf[AnyRef], valueType, parsers)
          val qK = if(key.contains(':') || key.contains(',')) s""""$key"""" else key
          val qV = if(value.contains(':') || value.contains(',')) s""""$value"""" else value
          s"$qK:$qV"
        }.mkString(",")

    }
  }

}


object SelectInputParser extends CompoundParser[SelectInput[_]] {
  def canParse(tpe: Type) = {
    ParseHelper.checkType(tpe, classOf[SelectInput[_]])
  }

  def parse(s: String, tpe: Type, currentValue: AnyRef, parsers: Seq[Parser[_]]) = {
    val currentVal = currentValue.asInstanceOf[SelectInput[Any]] //not really Any, but not sure how to make the compiler happy ...
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val subtype = ptpe.getActualTypeArguments()(0)
      val subParser = ParseHelper.findParser(subtype, parsers).get
      val parsed = subParser.parse(s, subtype, currentVal.value, parsers)
      if (currentVal.options(parsed)) currentVal.value = Some(parsed)
      else throw new IllegalArgumentException(parsed + " is not the allowed values: " + currentVal.options)
      //we don't return a new object, just modify the existing one
      currentVal
    } else throw new UnsupportedOperationException()
  }

  override def allowedValues(tpe: Type, currentValue: AnyRef): Option[Set[String]] = {
    val currentVal = currentValue.asInstanceOf[SelectInput[Any]] //not really Any, but not sure how to make the compiler happy ...
    Some(currentVal.options.map{_.toString})
  }
}

object MultiSelectInputParser extends CompoundParser[MultiSelectInput[_]] {
  override def canParse(tpe: Type) = ParseHelper.checkType(tpe, classOf[MultiSelectInput[_]])

  override def parse(s: String, tpe: Type, currentValue: AnyRef, parsers: Seq[Parser[_]]) = {
    val currentVal = currentValue.asInstanceOf[MultiSelectInput[Any]] //not really Any, but not sure how to make the compiler happy ...
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val subtype = ptpe.getActualTypeArguments()(0)
      val subParser = ParseHelper.findParser(subtype, parsers).get
      val parsed: Set[Any] = s.split(",").map(subParser.parse(_, subtype, "dummy", parsers)).toSet
      val illegal = parsed.diff(currentVal.options)
      if (illegal.isEmpty) currentVal.value = parsed
      else throw new IllegalArgumentException(illegal.toString + " is not the allowed values: " + currentVal.options)
      //we don't return a new object, just modify the existing one
      currentVal
    } else throw new UnsupportedOperationException()
  }

  override def allowedValues(tpe: Type, currentValue: AnyRef): Option[Set[String]] = {
    val currentVal = currentValue.asInstanceOf[MultiSelectInput[Any]] //not really Any, but not sure how to make the compiler happy ...
    Some(currentVal.options.map{_.toString})
  }
}

object ParseHelper {
  val defaultParsers: Seq[Parser[_]] = Seq(
    StringParser,
    IntParser,
    LongParser,
    FloatParser,
    DoubleParser,
    BooleanParser,
    FileParser,
    RegexParser,
    DurationParser,
    FiniteDurationParser,
    EnumParser,

    //collections
    OptionParser,
    ListParser,
    SetParser,
    ArrayParser,
    VectorParser,
    SeqParser,
    MapParser,
    TraversableParser, //order matters, be sure this at the end

    //special collections
    SelectInputParser,
    MultiSelectInputParser
  )

  def findParser(tpe: Type, parsers: Seq[Parser[_]]): Option[Parser[_]] = parsers.find(_.canParse(tpe))

  def parseInto[T](s: String, tpe: Type, currentValue: AnyRef, parsers: Seq[Parser[_]]): Option[ValueHolder[T]] = {
    //could change this to be a map, at least for the simple types
    findParser(tpe, parsers).map { parser =>
      ValueHolder[T](parser.parse(s, tpe, currentValue, parsers).asInstanceOf[T], tpe)
    }
  }

  def checkType(tpe: Type, targetClassSet: Class[_]*) = {
    def helper(tpe: Type, targetCls: Class[_]) = {
      targetCls.isAssignableFrom(ReflectionUtils.getRawClass(tpe))
    }
    targetClassSet.exists(targetClass => helper(tpe, targetClass))
  }

  def getSubParser(tpe:Type, parsers: Seq[Parser[_]], pos: Int = 0): (Type, Parser[_]) = {
    val ptpe = tpe.asInstanceOf[ParameterizedType]
    val subtype = ptpe.getActualTypeArguments()(pos)
    (subtype, ParseHelper.findParser(subtype, parsers).get)
  }
}

case class ValueHolder[T](value: T, tpe: Type)
