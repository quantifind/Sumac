package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class ArgAppTest extends FunSuite {

  test("main") {
    val m = new MyApp()
    m.main(Array("--a", "hello", "--b", "17"))
  }

}

class MyArgs extends FieldParsing {
  val a: String = ""
  val b: Int = 0
}

class MyApp extends Dummy with ArgApp[MyArgs] with ShouldMatchers {
  def main(args: MyArgs) {
    args.a should be ("hello")
    args.b should be (17)
  }
}

trait Dummy

