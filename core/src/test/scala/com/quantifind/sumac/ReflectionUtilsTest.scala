package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import scala.reflect.runtime.{universe => ru}

class ReflectionUtilsTest extends FunSuite with ShouldMatchers {
  import ReflectionUtils._
  test("extractGetterSetterPairs") {
    def termHelper[T:ru.TypeTag] = extractGetterSetterPairs(ru.typeOf[T]).map{gs => termName(gs.getter)}.toSet
    termHelper[Foo] should be (Set("x", "y"))

    termHelper[Blah] should be (Set("x", "y", "a"))

    termHelper[Ooga] should be (Set("q"))

    termHelper[Booga] should be (Set("x", "y", "q", "wakka"))
  }

  test("getterSetter return type") {
    val gsOpt = extractGetterSetterPairs(ru.typeOf[Booga]).find{gs => gs.name == "wakka"}
    gsOpt should be ('defined)
    assert(gsOpt.get.fieldType =:= ru.typeOf[Float])
  }
}

class Foo {
  var x: Int = _
  var y: String = _
}

class Blah extends Foo {
  var a: Float = _
}

trait Ooga {
  var q: Double = _
}

class Booga extends Foo with Ooga {
  var wakka : Float = _
  val ignore = 7
}