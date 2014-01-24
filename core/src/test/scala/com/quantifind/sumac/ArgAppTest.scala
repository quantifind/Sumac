package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class ArgAppTest extends FunSuite with ShouldMatchers {

  test("getArgumentClass") {
    val m = new MyTestApp()
    m.getArgumentClass should be (classOf[MyArgs])

    val m2 = new MyNestedArgApp()
    m2.getArgumentClass should be (classOf[MyArgs])
  }

  test("main") {
    val m = new MyTestApp()
    m.main(Array("--a", "hello", "--b", "17"))

    val m2 = new MyNestedArgApp()
    m2.main(Array("--a", "byebye", "--b", "3"))
  }

}

class MyTestArgs extends FieldArgs {
  var a: String = ""
  var b: Int = 0
}

class MyTestApp extends Dummy with ArgApp[MyArgs] with ShouldMatchers {
  def main(args: MyArgs) {
    args.a should be ("hello")
    args.b should be (17)
  }
}

trait Dummy


trait NestedArgMain extends ArgMain[MyArgs] {
  def blah(x: Int) = x + 5
}

class MyNestedArgApp extends NestedArgMain with ShouldMatchers {
  def main(args: MyArgs) {
    args.a should be ("byebye")
    args.b should be (3)
  }
}