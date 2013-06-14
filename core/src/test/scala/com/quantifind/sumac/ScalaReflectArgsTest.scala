package com.quantifind.sumac

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import scala.reflect.runtime._

class ScalaReflectArgsTest extends FunSuite with ShouldMatchers {

  test("find members") {
    val f = new Foo

    f.getArgs("")


  }

}

class Foo extends ScalaReflectArgs {
  //this works, but sucks -- want to get this automatically, don't want to make user do this.  Need
  // some equivalent of this.getClass() like this.getScalaType()
  implicit val tpe = scala.reflect.runtime.universe.typeOf[Foo]
  var x: Int = 5
  var y: List[Int] = _
}