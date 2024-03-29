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
import collection._

class ExternalConfigTest extends AnyFunSuiteLike with Matchers {
  test("modifying args"){
    val args = new ExtArgs() with DefaultNumeroUno
    args.parse(Array("--x", "5", "--y", "hi there"))
    args.x should be (5)
    args.y should be ("hi there")

    //Let x get defaulted by external config.  Note that this is NOT the right way to provide argument default.
    // This is just an easy way to test the functionality
    args.parse(Array("--y", "blah"))
    args.x should be (17)
    args.y should be ("blah")


    args.saveConfig()
    args.numeroUnoSave should be (true)
  }

  test("stacked traits"){
    //right-most trait goes first
    val arg1 = new ExtArgs() with DefaultNumeroUno with DefaultNumberTwo
    arg1.parse(Array[String]())
    arg1.x should be (21)
    arg1.ooga should be (3.5f)

    val arg2 = new ExtArgs() with DefaultNumberTwo with DefaultNumeroUno
    arg2.parse(Array[String]())
    arg2.parse(Array[String]())
    arg2.x should be (17)
    arg2.ooga should be (3.5f)

    arg1.saveConfig()
    arg1.numeroUnoSave should be (true)
    arg1.numberTwoSave should be (true)

    arg2.saveConfig()
    arg2.numeroUnoSave should be (true)
    arg2.numberTwoSave should be (true)
  }


  class ExtArgs extends FieldArgs {
    var x: Int = _
    var y: String = _
    var ooga: Float = _
  }


  trait DefaultNumeroUno extends ExternalConfig {
    self: Args =>
    var numeroUnoSave = false
    abstract override def readArgs(originalArgs: Map[String,String]): Map[String,String] = {
      super.readArgs(
        if (originalArgs.contains("x"))
          originalArgs
        else
          originalArgs ++ Map("x" -> "17")
      )
    }

    abstract override def saveConfig(): Unit = {
      numeroUnoSave = true
      super.saveConfig()
    }
  }

  trait DefaultNumberTwo extends ExternalConfig {
    self: Args =>
    var numberTwoSave = false
    abstract override def readArgs(originalArgs: Map[String,String]): Map[String,String] = {
      val withX = if (originalArgs.contains("x"))
        originalArgs
      else
        originalArgs ++ Map("x" -> "21")

      val withOoga = if (withX.contains("ooga"))
        withX
      else
        withX ++ Map("ooga" -> "3.5")

      super.readArgs(withOoga)
    }

    abstract override def saveConfig(): Unit = {
      numberTwoSave  = true
      super.saveConfig()
    }

  }

}
