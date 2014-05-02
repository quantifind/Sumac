package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.Matchers
import java.util.TimeZone
import java.text.SimpleDateFormat
import org.joda.time.{ReadableDateTime, DateTimeZone}

class DateTimeParserTest extends FunSuite with Matchers {
  test("date parser") {
    class B extends FieldArgs {
      registerParser(USDateTimeParser)
      var x: ReadableDateTime = _
      var y: AnyRef = _
    }
    val b = new B()
    b.getArgs("").map{_.getName} should not contain ("y")

    val tz = TimeZone.getTimeZone("UTC")
    val format = new SimpleDateFormat("yyyy-MM-dd")
    format.setTimeZone(tz)
    val d = format.parse("2013-12-26")

    Seq("2013-12-26", "2013/12/26", "12-26-2013", "12/26/2013").foreach{p =>
      withClue(p){
        b.parse(Array("--x", p))
        b.x.getMillis should be (d.getTime)
        b.x.getZone should be (DateTimeZone.forID("UTC"))

        b.getStringValues("x") should be ("2013-12-26")
      }
    }
  }
}
