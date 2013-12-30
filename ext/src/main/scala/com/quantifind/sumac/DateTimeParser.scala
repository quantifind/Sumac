package com.quantifind.sumac

import org.joda.time.DateTime
import org.joda.time.chrono.ISOChronology

object DateTimeParser extends SimpleParser[DateTime] {
  val knownTypes: Set[Class[_]] = Set(classOf[DateTime])
  val utc = ISOChronology.getInstanceUTC
  def parse(s:String) = {
    val d = DateParser.parse(s)
    new DateTime(d.getTime()).withChronology(utc)
  }
}
