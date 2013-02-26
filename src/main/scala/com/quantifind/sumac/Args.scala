package com.quantifind.sumac

trait Args {
  def getArgs: Traversable[ArgAssignable]

  lazy val parser = ArgumentParser(getArgs)

  def parse(args: Array[String]) {
    val parsedArgs = parser.parse(args)
    parsedArgs.foreach { case (argAssignable, valueHolder) =>
      argAssignable.setValue(valueHolder.value)
    }
  }

  def helpMessage = parser.helpMessage

  /**
   * add the ability to parse your own custom types.  Note that this
   * registers the parser *globally*, not just for this object.
   */
  def registerParser[T](parser: Parser[T]) {
    ParseHelper.registerParser(parser)
  }
}
