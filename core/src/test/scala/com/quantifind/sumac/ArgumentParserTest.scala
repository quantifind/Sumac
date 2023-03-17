/**
  * Copyright 2012-2020 Quantifind, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */

package com.quantifind.sumac

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import scala.collection._

class ArgumentParserTest extends AnyFunSuiteLike with Matchers {

  test("parse") {
    val c = SimpleClass("a", 0, 1.4, 2)
    val fieldArgs = classOf[SimpleClass].getDeclaredFields.map{f => FieldArgAssignable("",f, c, ParseHelper.defaultParsers)}
    val argParser = new ArgumentParser(fieldArgs, ParseHelper.defaultParsers)

    {
      val parsed = getSimpleNameToArgMap(argParser.parse(Array("--name", "foo")))
      parsed.size should be (1)
      parsed should contain key ("name")
      parsed("name") should be ("foo")
    }


    {
      val parsed = getSimpleNameToArgMap(argParser.parse(Array("--count", "5", "--dummy", "7.4e3", "--name", "ooga")))
      parsed.size should be (3)
      parsed should contain key ("count")
      parsed("count") should be (5)
      parsed should contain key ("dummy")
      parsed("dummy") should be (7.4e3)
      parsed should contain key ("name")
      parsed("name") should be ("ooga")
    }

    {
      val parsed = getSimpleNameToArgMap(argParser.parse("--count  5  --dummy 7.4e3 --name ooga"))
      parsed.size should be (3)
      parsed should contain key ("count")
      parsed("count") should be (5)
      parsed should contain key ("dummy")
      parsed("dummy") should be (7.4e3)
      parsed should contain key ("name")
      parsed("name") should be ("ooga")
    }
  }

  test("reserved arguments should be filtered") {
    val c = SimpleClass("a", 0, 1.4, 2)
    val fieldArgs = classOf[SimpleClass].getDeclaredFields.map{f => FieldArgAssignable("",f, c, ParseHelper.defaultParsers)}
    val argParser = new ArgumentParser(fieldArgs, ParseHelper.defaultParsers)

    val parsed = getSimpleNameToArgMap(argParser.parse(Array("--count", "5", "--dummy", "7.4e3", "--name", "ooga", "--sumac.debugArgs", "true")))

    parsed should not contain key("sumac.debugArgs")

  }

  def getSimpleNameToArgMap[A <: ArgAssignable](parsedArgs: Map[A, ValueHolder[_]]): Map[String, Any] = {
    parsedArgs.map { case (k, v) => k.getName -> v.value }.toMap[String, Any]
  }

  test("remove newlines") {
    ArgumentParser.argCLIStringToArgList(
      """--x "5 \\\\\" 6\"\\" \
         --y 6 \
         --blank "" \
         """ + "--z '\t\n'") should be (Array("--x", """5 \\\\\" 6\"\\""", "--y", "6","--blank", "", "--z", "\t\n"))
  }
}


case class SimpleClass(val name: String, val count: Int, val dummy: Double, val count2: Int)
