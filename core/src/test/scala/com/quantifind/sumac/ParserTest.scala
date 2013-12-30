package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.collection._
import java.io.File
import java.util.{Calendar, TimeZone, GregorianCalendar, Date}
import java.text.SimpleDateFormat


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
    DurationParser.parse("10.seconds") should be (10 seconds)
    DurationParser.parse("10.minutes") should be (10 minutes)
    FiniteDurationParser.parse("3.days") should be (3 days)
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

  test("array parser") {
    class A extends FieldArgs {
      var x: Array[Duration] = _
    }
    val a = new A()
    a.parse(Array("--x", "10 seconds, 15.seconds, 30 minutes"))
    a.x should be (Array(10 seconds, 15 seconds, 30 minutes))
  }

  test("traversable parser") {
    class A extends FieldArgs {
      var x: Traversable[Duration] = _
    }
    val a = new A()
    a.parse(Array("--x", "10 seconds, 15.seconds, 30 minutes"))
    a.x should be (Traversable(10 seconds, 15 seconds, 30 minutes))
  }

  test("seq parser") {
    //note that these are all the types in scala.collection, NOT the ones in scala.predef
    class A extends FieldArgs {
      var x: Seq[Duration] = _
    }
    val a = new A()
    a.parse(Array("--x", "10 seconds, 15.seconds, 30 minutes"))
    a.x should be (Seq(10 seconds, 15 seconds, 30 minutes))
  }

  test("vector parser"){
    class B extends FieldArgs {
      var x: Vector[Duration] = _
    }
    val b = new B()
    b.parse(Array("--x", "10 seconds, 15.seconds, 30 minutes"))
    b.x should be (Seq(10 seconds, 15 seconds, 30 minutes))
  }

  test("map parser") {
    class A extends FieldArgs {
      var x: Map[File,Duration] = _
    }

    val a = new A()
    a.parse(Array("--x", "/blah/ooga:10 seconds,/foo/bar:1 hour"))
    a.x should be (Map(
      new File("/blah/ooga") -> (10 seconds),
      new File("/foo/bar") -> (1 hour)
    ))

    val ex = evaluating {a.parse(Array("--x", "adfadfdfa"))} should produce [IllegalArgumentException]
    ex.getMessage should include ("""expect a list of kv pairs, with each key separated from value by ":" and pairs separated by ","""")
  }

  test("date parser") {
    class A extends FieldArgs {
      var x: Date = _
    }
    val a = new A()

    class B extends FieldArgs {
      var x: Calendar = _
    }
    val b = new B()


    //using java time classes is a serious pain ...
    val tz = TimeZone.getTimeZone("UTC")
    val format = new SimpleDateFormat("yyyy-MM-dd")
    format.setTimeZone(tz)
    val d = format.parse("2013-12-26")

    Seq("2013-12-26", "2013/12/26", "12-26-2013", "12/26/2013").foreach{p =>
      withClue(p){
        a.parse(Array("--x", p))
        a.x should be (d)

        b.parse(Array("--x", p))
        b.x.getTimeInMillis should be (d.getTime)
        b.x.getTimeZone should be (tz)
      }
    }
  }

  test("enum parser") {
    class A extends FieldArgs {
      var x: MyEnum = _
    }
    val a = new A()

    a.parse(Array("--x", "Abigail"))
    a.x should be (MyEnum.Abigail)



    val ex = evaluating{a.parse(Array("--x", "foobar"))} should produce [ArgException]
    ex.getMessage should include ("foobar is not in set of enum values: " + MyEnum.values.mkString(","))
  }

}

class RandomUnknownClass


class ContainerA(val title: String, val count: Int, val boundaries: List[String])

class ContainerOption(val string: Option[String], val listOfString: Option[List[String]])
