package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

/**
 * Test ConfigArgs
 *
 * there should be an application.conf and alternate.conf in the resources folder
 *
 * User: andrews
 * Date: 12/18/13
 */
class ConfigTest extends FunSuite with ShouldMatchers {

  test("should load config from file and fallback to it") {

    val args = Array[String]()

    val test = new Test
    test.parse(args)

    test.arg1 should be(Some(10.seconds))

  }

  test("if arg is provided, ignore config file") {

    val args = Array[String]("--arg1", "22.minutes")

    val test = new Test
    test.parse(args)

    test.arg1 should be(Some(22.minutes))

  }


  test("support nested configs") {

    val args = Array[String]("--arg1", "10 days")

    val test = new TestWithNested
    test.parse(args)

    test.arg1 should be(Some(10.days))
    test.arg2 should not be (null)
    test.arg2.arg3 should be(42.42)

  }

  test("support setter") {

    val test = new Test with ConfigSetter
    test.config = ConfigFactory.load("alternate.conf")

    test.parse(Array[String]())

    test.arg1 should be(Some(3 days))

  }

  test("use default if nothing is provided") {
    val test = new TestWithNested with ConfigSetter
    test.config = ConfigFactory.load("alternate.conf")

    test.parse(Array[String]())

    test.arg2.arg3 should be(10.5)
  }

  test("validations should be applied on the config value too") {
    val test = new Test {
      addValidation {
        if (arg1.equals(Some(10 seconds))) throw new IllegalArgumentException(s"test arg1 = $arg1")
      }
    }
    val ex = intercept[IllegalArgumentException] {
      test.parse(Array[String]())
    }
    ex.getMessage should be("test arg1 = Some(10 seconds)")
  }

}

class Test extends FieldArgs with ConfigArgs {
  override val configPrefix = "sumac.ext.test"

  var arg1: Option[Duration] = None
}

class Nested extends FieldArgs {
  var arg3: Double = 10.5
}


class TestWithNested extends Test {
  var arg2: Nested = new Nested
}