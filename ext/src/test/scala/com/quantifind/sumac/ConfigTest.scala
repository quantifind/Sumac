package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import com.typesafe.config.ConfigFactory

/**
 * TODO DOC
 * User: andrews
 * Date: 12/18/13
 */
class ConfigTest extends FunSuite with ShouldMatchers {

  test("should load config from file and fallback to it") {

    val args = Array[String]()

    val test = new Test
    test.parse(args)

    test.arg1 should be("arg1")

  }

  test("if arg is provided, ignore config file") {

    val args = Array[String]("--arg1", "other")

    val test = new Test
    test.parse(args)

    test.arg1 should be("other")

  }


  test("support nested configs") {

    val args = Array[String]("--arg1", "other")

    val test = new TestWithNested
    test.parse(args)

    test.arg1 should be("other")
    test.arg2 should not be(null)
    test.arg2.arg3 should be("arg3")

  }

  test("support setter") {

    val test = new Test with ConfigSetter
    test.config = ConfigFactory.load("alternate.conf")

    test.parse(Array[String]())

    test.arg1 should be("alternate")

  }

  test("use default if nothing is provided") {
    val test = new TestWithNested with ConfigSetter
    test.config = ConfigFactory.load("alternate.conf")

    test.parse(Array[String]())

    test.arg2.arg3 should be("default")
  }

}

class Test extends FieldArgs with ConfigArgs {
  override val configPrefix = "sumac.ext.test"

  var arg1: String = _
}

class Nested extends FieldArgs {
  var arg3: String = "default"
}


class TestWithNested extends FieldArgs with ConfigArgs {
  override val configPrefix = "sumac.ext.test"

  var arg1: String = _
  var arg2: Nested = new Nested
}