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
import java.io._

class ArgBuilderTest extends AnyFunSuiteLike with Matchers {

  val nullOutput = new PrintStream(new OutputStream {
    def write(p1: Int): Unit = {}
  })

  test("prompting") {
    val args = new BuilderTestArgs()
    val input = fixedInputStream(
      //no name
      "","",
      //count = 5
      "5",
      //no property file
      "",""
    )
    //I'm not actually checking what the prompts are here ...
    ArgBuilder.promptForArgs(args, input, nullOutput)
    args.count should be (5)
    args.name should be ("Here's a name")
    args.propertyFile should be (null)
  }

  val dir = new File("test_output/" + getClass.getSimpleName)
  dir.mkdirs()

  test("prompting & saving") {
    val args = new BuilderTestArgs()
    val propFile = new File(dir, "arg_builder_test_output.properties").getAbsolutePath
    val input = fixedInputStream(
      //empty string name
      "","\"\"",
      //count = 5
      "5",
      //property file
      propFile
    )
    ArgBuilder.promptForArgs(args, input, nullOutput)
    args.saveConfig()

    val args2 = new BuilderTestArgs()
    args2.propertyFile = new File(propFile)
    args2.parse(Array[String]())
    args2.name should be ("")
    args2.count should be (5)
    args2.propertyFile should be (new File(propFile))
  }

  def fixedInputStream(lines: String*): BufferedReader = {
    //THIS IS REALLY UGLY.  it only works b/c I know I only call readLine on the Buffered Reader
    val itr = lines.iterator
    new BufferedReader(new Reader() {
      def close(): Unit = {}

      def read(p1: Array[Char], p2: Int, p3: Int): Int = 0
    }) {
      override def readLine(): String = if (itr.hasNext) itr.next() else null
    }
  }
}

class BuilderTestArgs extends FieldArgs with PropertiesConfig {
  var name = "Here's a name"
  var count: Int = _
}
