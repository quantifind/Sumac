package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import scala.reflect.runtime.{universe => ru}

class ReflectionUtilsTest extends FunSuite with ShouldMatchers {
  import ReflectionUtils._
  def getterSetterNames[T:ru.TypeTag] = extractGetterSetterPairs(ru.typeOf[T]).map{gs => termName(gs.getter)}.toSet
  test("extractGetterSetterPairs") {
    getterSetterNames[Foo] should be (Set("x", "y"))

    getterSetterNames[Blah] should be (Set("x", "y", "a"))

    getterSetterNames[Ooga] should be (Set("q"))

    getterSetterNames[Booga] should be (Set("x", "y", "q", "wakka"))
  }

  test("getterSetter return type") {
    val gsOpt = extractGetterSetterPairs(ru.typeOf[Booga]).find{gs => gs.name == "wakka"}
    gsOpt should be ('defined)
    assert(gsOpt.get.fieldType =:= ru.typeOf[Float])
  }

  test("getterSetter with name reused in subclass constructor") {
    //weird behavior from scala reflection ...
    getterSetterNames[B] should be (Set("x", "y"))
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

class A(var x:Int)

class B(x: Int, var y: Int) extends A(x)
