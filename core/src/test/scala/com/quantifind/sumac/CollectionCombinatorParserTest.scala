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

/**
 * User: pierre
 * Date: 3/25/14
 */
class CollectionCombinatorParserTest extends AnyFunSuiteLike with Matchers {

  test("should parse a single item") {
    val item = CollectionCombinatorParser("item")

    item should contain("item")
    item.size should be(1)
  }

  test("should parse a single quoted item") {
    val item = CollectionCombinatorParser(""""item,foo"""")

    item should contain("item,foo")
    item.size should be(1)
  }

  test("should parse a list of unquoted items") {
    val item = CollectionCombinatorParser("item1,item2,item3")

    item should contain("item1")
    item should contain("item2")
    item should contain("item3")
    item.size should be(3)
  }

  test("should parse a list of quoted items") {
    val item = CollectionCombinatorParser(""""item,foo","item2,bar","item3"""")

    item should contain("item,foo")
    item should contain("item2,bar")
    item should contain("item3")
    item.size should be(3)
  }

  test("should parse a list of mixed items") {
    val item = CollectionCombinatorParser(""""item,foo",item2,item3,"item4,bar"""")

    item should contain("item,foo")
    item should contain("item2")
    item should contain("item3")
    item should contain("item4,bar")
    item.size should be(4)
  }

  test("should still allow a quote in unquoted items") {
    val item = CollectionCombinatorParser("""it"em1,item2,item3""")

    item should contain("it\"em1")
    item should contain("item2")
    item should contain("item3")
    item.size should be(3)

  }

  test("should allow single quoted items") {
    val item = CollectionCombinatorParser("""'item,1','item"2',item3,"item,4"""")

    item should contain("item,1")
    item should contain("item\"2")
    item should contain("item3")
    item should contain("item,4")
  }

}
