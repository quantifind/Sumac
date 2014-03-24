package com.quantifind.sumac

import scala.util.parsing.combinator.RegexParsers

/**
 * parser for map arguments. A map can be represented as a sequence of key/value pairs.
 * - keys are separated by values with a column : . eg: foo:bar
 * - key/value pairs separated by a comma , . eg: foo:bar,keybar:foovalue,anotherK,AnotherV
 * - if a key or a value has to contain a comma or column, it needs to be provided between quotes. eg: foo:bar,"foo,key":bar,"bar:bar,key":"10,5"
 * User: andrews
 * Date: 3/24/14
 */
object MapCombinatorParser extends RegexParsers {

  /**
   * a token (key or value) is either anything without a : or , in it, or a quote string with these reserved characters
   */
  val token: Parser[String] = """^[^",:]+""".r | "\"" ~> """[^"]+""".r <~ "\""

  /**
   * an entry is a key:value pair
   */
  val entry: Parser[(String, String)] = token ~ ":" ~ token ^^ {
    case (key ~ ":" ~ value) => (key, value)
  }

  /**
   * a map is a sequence of entry separated by commas ,
   */
  val map: Parser[List[(String, String)]] = rep1(entry, "," ~> entry)

  /**
   * parse a string to a Map or throw an IllegalArgumentException
   * @param in a string to parse
   * @return the parsed map
   */
  def apply(in: String): Map[String, String] = parseAll(map, in) match {
    case Success(result, _) => result.toMap
    case failure: NoSuccess =>
      throw new IllegalArgumentException(s"'$in' cannot be parsed to a map. Caused by: ${failure.msg}}")
  }


}