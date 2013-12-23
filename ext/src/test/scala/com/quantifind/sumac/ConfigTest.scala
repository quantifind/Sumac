package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.collection.Map

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

    val test = new Test
    test.useDefaultConfig = false
    test addConfig "alternate.conf"

    test.parse(Array[String]())

    test.arg1 should be(Some(3 days))

  }

  test("use default if nothing is provided") {
    val test = new TestWithNested
    test.useDefaultConfig = false
    test addConfig "alternate.conf"

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

  test("get the name of the config file from another arg -- use default value") {
    val t1 = new TestFromArg

    t1.parse(Array[String]())

    t1.arg1 should be(Some(3.days)) //no env arg, should fallback on default: 'dev' and use alternate.conf
  }


  test("get the name of the config file from another arg -- use arg") {
    val t2 = new TestFromArg

    t2.parse(Array[String]("--env", "prod")) //prod env, use application.conf

    t2.arg1 should be(Some(10.seconds)) //use the prod application.conf
  }


  test("get the name of the config file from another arg -- use nothing") {

    val t3 = new TestFromArg
    t3.parse(Array[String]("--env", "")) //defaults to nothing, use the default arg1

    t3.arg1 should be(None)

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

class TestFromArg extends Test with ConfigFromArg {
  var env: String = "dev"

  useDefaultConfig = false

  override lazy val configFilenameFromArg: Option[String] = {
    env match {
      case "prod" => Some("application.conf")
      case "dev" => Some("alternate.conf")
      case _ => None
    }
  }
}