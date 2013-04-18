package com.quantifind.sumac

import collection._

trait Args extends ExternalConfig {
  def getArgs: Traversable[ArgAssignable]

  lazy val parser = ArgumentParser(getArgs)
  var validationFunctions: Seq[() => Unit] = Seq()

  def parse(args: Array[String]) {
    val originalKvPairs = ArgumentParser.argListToKvMap(args)
    val modifiedKvPairs = readArgs(originalKvPairs)
    val parsedArgs = parser.parse(modifiedKvPairs)
    parsedArgs.foreach { case (argAssignable, valueHolder) =>
      argAssignable.setValue(valueHolder.value)
    }
    runValidation()
  }

  override def readArgs(originalArgs: Map[String,String]): Map[String,String] = {
    originalArgs  //here for stackable traits
  }

  override def saveConfig() {} //noop, here for stackable traits

  /**
   * run all validation functions.
   *
   * Note that parse automatically runs all validation, so in general a user will not need to call this.  However,
   * if you are programatically filling in the the args of this object, you probably want to call this.
   */
  def runValidation() {
    validationFunctions.foreach{_()}
  }

  def helpMessage = parser.helpMessage

  /**
   * add the ability to parse your own custom types.  Note that this
   * registers the parser *globally*, not just for this object.
   */
  def registerParser[T](parser: Parser[T]) {
    ParseHelper.registerParser(parser)
  }

  def addValidation(f:  => Unit) {
    validationFunctions ++= Seq(() => f)
  }
}
