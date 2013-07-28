package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 *
 */

class ParserTest extends FunSuite with ShouldMatchers {

  test("SimpleParser") {
    StringParser.parse("ooga") should be ("ooga")
    IntParser.parse("5") should be (5)
    DoubleParser.parse("5") should be (5.0)
    DoubleParser.parse("1e-10") should be (1e-10)
    BooleanParser.parse("false") should be (false)
    BooleanParser.parse("true") should be (true)
    val homeDir = System.getProperty("user.home")
    FileParser.parse("~/foo") should be (new java.io.File(homeDir, "foo"))
    val cwd = System.getProperty("user.dir")
    FileParser.parse("ooga").getAbsolutePath should be (new java.io.File(cwd, "ooga").getAbsolutePath)
    import scala.concurrent.duration._
    DurationParser.parse("10.seconds") should be (10 seconds)
    DurationParser.parse("10.minutes") should be (10 minutes)
  }

  test("ListParser") {
    //Note this doesn't work w/ primitive types now, b/c its based on java reflection

    //Is there is better way to get a handle on parameterized types????
    val field = classOf[ContainerA].getDeclaredField("boundaries")
    val parsed = ParseHelper.parseInto("a,b,cdef,g", field.getGenericType, "dummy")
    parsed should be (Some(ValueHolder(List("a", "b", "cdef", "g"), field.getGenericType)))
  }
  
  test("OptionParser") {
    //Doesn't work with primitive types, same problem as ListParser?
    OptionParser.parse("foo", classOf[ContainerOption].getDeclaredField("string").getGenericType, null) should be (Some("foo"): Option[String])
    OptionParser.parse(null, classOf[ContainerOption].getDeclaredField("string").getGenericType, null) should be (None: Option[String])
    OptionParser.parse(Parser.nullString, classOf[ContainerOption].getDeclaredField("string").getGenericType, null) should be (None: Option[String])

    OptionParser.parse("a,b,cdef,g", classOf[ContainerOption].getDeclaredField("listOfString").getGenericType, null) should be (Some(List("a", "b", "cdef", "g")): Option[List[String]])
  }

  test("ParseHelper") {
    ParseHelper.parseInto("ooga", classOf[String], "dummy") should be (Some(ValueHolder("ooga", classOf[String])))
    ParseHelper.parseInto("5.6", classOf[Double], "dummy") should be (Some(ValueHolder(5.6, classOf[Double])))
    ParseHelper.parseInto("5.6", classOf[String], "dummy") should be (Some(ValueHolder("5.6", classOf[String])))
    ParseHelper.parseInto("abc", classOf[RandomUnknownClass], "dummy") should be (None)
  }

}

class RandomUnknownClass


class ContainerA(val title: String, val count: Int, val boundaries: List[String])

class ContainerOption(val string: Option[String], val listOfString: Option[List[String]])
