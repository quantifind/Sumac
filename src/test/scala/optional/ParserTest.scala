package optional

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
  }

  test("ListParser") {
    //Note this doesn't work w/ primitive types now, b/c its based on java reflection

    //Is there is better way to get a handle on parameterized types????
    val field = classOf[ContainerA].getDeclaredField("boundaries")
    val parsed = ParseHelper.parseInto("a,b,cdef,g", field.getGenericType)
    parsed should be (Some(ValueHolder(List("a", "b", "cdef", "g"), field.getGenericType)))
  }

  test("ParseHelper") {
    ParseHelper.parseInto("ooga", classOf[String]) should be (Some(ValueHolder("ooga", classOf[String])))
    ParseHelper.parseInto("5.6", classOf[Double]) should be (Some(ValueHolder(5.6, classOf[Double])))
    ParseHelper.parseInto("5.6", classOf[String]) should be (Some(ValueHolder("5.6", classOf[String])))
    ParseHelper.parseInto("abc", classOf[RandomUnknownClass]) should be (None)
  }

}

class RandomUnknownClass


class ContainerA(val title: String, val count: Int, val boundaries: List[String])
