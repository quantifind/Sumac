package com.quantifind.sumac

import org.joda.time.{ReadableDateTime, Chronology, DateTime}
import org.joda.time.chrono.ISOChronology
import java.util.{Calendar, Date}
import java.lang.reflect.Type
import scala.util.matching.Regex
import scala.collection._

class DateTimeParser(fmts:Map[Regex,String], chronology: Chronology = ISOChronology.getInstanceUTC)
  extends DateParser(fmts, chronology.getZone.toTimeZone)
{
  override val knownTypes: Set[Class[_]] = Set(
    classOf[DateTime], classOf[Date], classOf[Calendar], classOf[ReadableDateTime]
  )

  override def parse(s:String, tpe: Type, currentVal: AnyRef) = {
    tpe match {
      case dt: Class[_] if dt.isAssignableFrom(classOf[DateTime]) =>
        val d = parseDate(s)
        new DateTime(d.getTime()).withChronology(chronology)
    }
  }
}

object USDateTimeParser extends DateTimeParser(DateTimeFormats.usFormats)

object StandardDateTimeParse extends DateTimeParser(DateTimeFormats.stdFormats)