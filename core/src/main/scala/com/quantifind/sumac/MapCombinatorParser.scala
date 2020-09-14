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

/**
 * parser for map arguments. A map can be represented as a sequence of key/value pairs.
 * - keys are separated by values with a column : . eg: foo:bar
 * - key/value pairs separated by a comma , . eg: foo:bar,keybar:foovalue,anotherK,AnotherV
 * - if a key or a value has to contain a comma or column, it needs to be provided between quotes. eg: foo:bar,"foo,key":bar,"bar:bar,key":"10,5"
 * User: andrews
 * Date: 3/24/14
 */
object MapCombinatorParser extends BaseCombinatorParser[(String, String)] {

  override val extraForbiddenChars = ":"

  /**
   * an entry is a key:value pair
   */
  override val item: Parser[(String, String)] = token ~ ":" ~ token ^^ {
    case (key ~ ":" ~ value) => (key, value)
  }


  /**
   * parse a string to a Map or throw an IllegalArgumentException
   * @param in a string to parse
   * @return the parsed map
   */
  def apply(in: String): Map[String, String] = parse(in).toMap


}