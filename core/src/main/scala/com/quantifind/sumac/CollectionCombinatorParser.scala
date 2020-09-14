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
 * Parse a list separated by commas, allow items in the list to be quoted so they can include commas.
 * User: pierre
 * Date: 3/25/14
 */
object CollectionCombinatorParser  extends BaseCombinatorParser[String] {


  override val extraForbiddenChars: String = ""
  override val item = token

  def apply(in: String): Seq[String] = parse(in)

}
