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

import org.scalatest.FunSuite
import org.scalatest.Matchers

/**
 * Test the Map parser combinator
 * User: andrews
 * Date: 3/24/14
 */
class MapCombinatorParserTest  extends FunSuite with Matchers {

  test("should not parse something wrong") {
    an[IllegalArgumentException] should be thrownBy {
      MapCombinatorParser("akdfaskdf")
    }
  }

  test("should not parse unquoted things") {
    an[IllegalArgumentException] should be thrownBy {
      MapCombinatorParser("akdfaskdf:kaklfjd,;dlksjdf")
    }
  }

  test("should parse a single entry") {

    val parsed = MapCombinatorParser("key:value")

    parsed("key") should be("value")
    parsed.size should be(1)

  }

  test("should parse a set of entries") {

    val parsed = MapCombinatorParser("key:value,foo:bar")

    parsed("key") should be("value")
    parsed("foo") should be("bar")
    parsed.size should be(2)

  }

  test("should parse a entry with , in the key") {

    val parsed = MapCombinatorParser(""""key,foo":value""")

    parsed("key,foo") should be("value")
    parsed.size should be(1)

  }

  test("should parse a entry with , in the value") {

    val parsed = MapCombinatorParser("""key:"foo,value"""")

    parsed("key") should be("foo,value")
    parsed.size should be(1)

  }

  test("should parse a entry with : in the key") {

    val parsed = MapCombinatorParser(""""key:foo":value""")

    parsed("key:foo") should be("value")
    parsed.size should be(1)

  }

  test("should parse a entry with : in the value") {

    val parsed = MapCombinatorParser("""key:"foo:value"""")

    parsed("key") should be("foo:value")
    parsed.size should be(1)

  }

  test("should parse a entry with : or , in both key and values") {

    val parsed = MapCombinatorParser(""""key,foo":"foo:value"""")

    parsed("key,foo") should be("foo:value")
    parsed.size should be(1)

  }

  test("should parse a sequence of entries with : or , in both key and values") {

    val parsed = MapCombinatorParser(""""key,foo":"foo:value","foo:bar":foo,bar:"foo,bar"""")

    parsed("key,foo") should be("foo:value")
    parsed("foo:bar") should be("foo")
    parsed("bar") should be("foo,bar")
    parsed.size should be(3)

  }

  test("allow entries with a single double quote in them") {
    val parsed = MapCombinatorParser("""key"bar:value,foo:bar""")
    parsed("key\"bar") should be("value")
    parsed("foo") should be("bar")
  }

  test("should be happy with single quotes") {
    val parsed = MapCombinatorParser("""'key"bar':value,'foo:bar':"foo,value"""")
    parsed("key\"bar") should be("value")
    parsed("foo:bar") should be("foo,value")
  }
}
