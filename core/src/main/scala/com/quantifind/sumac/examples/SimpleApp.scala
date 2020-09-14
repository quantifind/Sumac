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

package com.quantifind.sumac.examples

import com.quantifind.sumac.{FieldArgs, ArgMain}

/**
 * after you compile the library, run this with
 *
 * java -cp core/target/scala-2.9.3/classes/:$SCALA_HOME/lib/scala-library.jar com.quantifind.sumac.examples.SimpleApp <args>
 *
 * eg.
 *
 * java -cp core/target/scala-2.9.3/classes/:$SCALA_HOME/lib/scala-library.jar com.quantifind.sumac.examples.SimpleApp
 * or
 * java -cp core/target/scala-2.9.3/classes/:$SCALA_HOME/lib/scala-library.jar com.quantifind.sumac.examples.SimpleApp --count 2
 * or
 * java -cp core/target/scala-2.9.3/classes/:$SCALA_HOME/lib/scala-library.jar com.quantifind.sumac.examples.SimpleApp --name ooga
 * or
 * java -cp core/target/scala-2.9.3/classes/:$SCALA_HOME/lib/scala-library.jar com.quantifind.sumac.examples.SimpleApp --help
 *
 * etc.
 *
 */
object SimpleApp extends ArgMain[SimpleAppArgs] {
  def main(args: SimpleAppArgs) {
    (0 until args.count).foreach{_ => println(args.name)}
  }
}

class SimpleAppArgs extends FieldArgs {
  var name: String = "the default name"
  var count: Int = 5
}
