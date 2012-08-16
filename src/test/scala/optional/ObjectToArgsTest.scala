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

}


case class StringHolder(val name: String, val comment: String)

case class MixedTypes(val name: String, val count: Int)