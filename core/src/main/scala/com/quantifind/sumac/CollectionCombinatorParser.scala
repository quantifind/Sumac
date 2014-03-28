package com.quantifind.sumac

import scala.util.parsing.combinator.RegexParsers

/**
 * Parse a list separated by commas, allow items in the list to be quoted so they can include commas.
 * User: pierre
 * Date: 3/25/14
 */
object CollectionCombinatorParser  extends RegexParsers {

  /**
   * an item  is either anything without a , in it, or a quotes string that might contain a comma
   */
  val noQuote = """[^",]*"?[^",]*""".r
  val quoted = "\"" ~> """[^"]+""".r <~ "\""
  val item: Parser[String] = quoted | noQuote

  val list: Parser[Seq[String]] = rep1(item, "," ~> item)

  def apply(in: String): Seq[String] = parseAll(list, in) match {
    case Success(result, _) => result
    case failure: NoSuccess =>
      throw new IllegalArgumentException(s"'$in' cannot be parsed to a collection. Caused by: ${failure.msg}}")
  }

}
