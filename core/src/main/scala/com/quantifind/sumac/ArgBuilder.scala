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

import java.io._

/**
 * Util for building up a set of arguments interactively through command line.  Prompt the user for each argument
 */
object ArgBuilder extends ArgMain[ArgBuilderArgs] {
  def main(mainArgs: ArgBuilderArgs) {
    val clz = Class.forName(mainArgs.argClass)
    val args = clz.newInstance().asInstanceOf[Args]
    promptForArgs(args)

    if (mainArgs.save) {
      println("Saving config")
      args.saveConfig()
    } else {
      println("Got arguments:")
      args.getStringValues.foreach{println}
    }
  }

  def promptForArgs(args: Args) {
    val in = new BufferedReader(new InputStreamReader(System.in))
    promptForArgs(args, in, System.out)
  }

  private[sumac] def promptForArgs(args: Args, input: BufferedReader, out: PrintStream) {
    out.println("Enter values for each argument.  To skip an argument, enter 2 blank lines. To enter an empty string, enter" +
      "one blank line followed by a line with \"\"")
    var newVals = Map[String,String]()
    args.getArgs("").foreach{aa =>
      out.println(aa)
      val line = input.readLine()
      if (line == "") {
        val l2 = input.readLine()
        if (l2 == "\"\"")
          newVals += aa.getName -> ""
        else if (l2 == ""){
          //noop
        } else {
          throw new RuntimeException("after entering one blank line, you may only enter another blank line (to skip" +
            "the argument) or \"\", for an empty string. You entered " + l2)
        }
      } else {
        newVals += aa.getName -> line
      }
    }
   args.parse(newVals, false)
  }
}


class ArgBuilderArgs extends FieldArgs {
  var argClass: String = _
  var save: Boolean = true
}
