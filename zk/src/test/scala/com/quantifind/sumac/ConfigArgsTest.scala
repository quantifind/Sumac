package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class ConfigArgsTest extends FunSuite with ShouldMatchers {

  test("basic config export") {
    val args1 = new SimpleConfig()
    val config1 = args1.toTypesafeConfig()
    config1.getString("name") should be ("ooga")
    config1.getInt("x") should be (0)
  }

  test("types maintained") {
    pending
  }

  test("nested args") {
    pending
  }

  test("config import") {
    pending
  }
}


class SimpleConfig extends ConfigArgs with FieldArgs {
  var x: Int = 0
  var name: String = "ooga"
}