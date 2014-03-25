package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * User: pierre
 * Date: 3/25/14
 */
class CollectionConbinatorParserTest extends FunSuite with ShouldMatchers {

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

}
