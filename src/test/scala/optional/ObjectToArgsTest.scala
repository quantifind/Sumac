package optional

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 *
 */

class ObjectToArgsTest extends FunSuite with ShouldMatchers {

  test("parseStrings") {
    val o = new StringHolder(null, null)
    val parser = new ObjectToArgs(o)
    parser.parse(Array("--name", "hello"))
    o.name should be ("hello")
    parser.parse(Array("--comment", "blah di blah blah"))
    o.name should be ("hello")
    o.comment should be ("blah di blah blah")
    parser.parse(Array("--name", "ooga", "--comment", "stuff"))
    o.name should be ("ooga")
    o.comment should be ("stuff")
  }

  test("parseMixed") {
    val o = new MixedTypes(null, 0)

    val parser = new ObjectToArgs(o)

    parser.parse(Array("--name", "foo", "--count", "17"))
    o.name should be ("foo")
    o.count should be (17)
    parser.parse(Array("--count", "-5"))
    o.name should be ("foo")
    o.count should be (-5)
  }

  test("field parsing") {
    val o = new MixedTypes(null, 0) with FieldParsing

    o.parse(Array("--count", "981", "--name", "wakkawakka"))
    o.name should be ("wakkawakka")
    o.count should be (981)
  }

  test("subclass parsing") {
    val o = new Child(false, null, 0) with FieldParsing

    o.parse(Array("--flag", "true", "--name", "bugaloo"))
    o.name should be ("bugaloo")
    o.flag should be (true)
  }

  test("custom parsers") {
    val o = new SpecialTypes(null, null) with FieldParsing

    o.parse(Array("--name", "blah"))
    o.name should be ("blah")

    evaluating {o.parse(Array("--funky", "xyz"))} should produce [Exception]

    o.parse(Array("--funky", "xyz", "--name", "hi"), preParsers = Iterator(MyFunkyTypeParser))
    o.name should be ("hi")
    o.funky should be (MyFunkyType("xyzoogabooga"))

  }

  //TODO tests that there are sensible errors on bad arguments


}


case class StringHolder(val name: String, val comment: String)

case class MixedTypes(val name: String, val count: Int)

//is there an easier way to do this in scala?
class Child(val flag: Boolean, name: String, count: Int) extends MixedTypes(name, count)

case class MyFunkyType(val stuff: String)

object MyFunkyTypeParser extends Parser[MyFunkyType] {
  def canParse(tpe: java.lang.reflect.Type) =
    classOf[MyFunkyType].isAssignableFrom(tpe.asInstanceOf[Class[_]])
  def parse(s: String, tpe: java.lang.reflect.Type) =
    MyFunkyType(s + "oogabooga")
}

case class SpecialTypes(val name: String, val funky: MyFunkyType)