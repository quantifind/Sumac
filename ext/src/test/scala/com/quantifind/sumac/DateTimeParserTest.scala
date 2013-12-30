package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.util.TimeZone
import java.text.SimpleDateFormat
import org.joda.time.{DateTimeZone, DateTime}

class DateTimeParserTest extends FunSuite with ShouldMatchers {
  test("date parser") {
    class B extends FieldArgs {
      registerParser(DateTimeParser)
      var x: DateTime = _
    }
    val b = new B()

    val tz = TimeZone.getTimeZone("UTC")
    val format = new SimpleDateFormat("yyyy-MM-dd")
    format.setTimeZone(tz)
    val d = format.parse("2013-12-26")

    Seq("2013-12-26", "2013/12/26", "12-26-2013", "12/26/2013").foreach{p =>
      withClue(p){
        b.parse(Array("--x", p))
        b.x.getMillis should be (d.getTime)
        b.x.getZone should be (DateTimeZone.forID("UTC"))
      }
    }
  }
}
