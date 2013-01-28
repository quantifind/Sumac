package optional

trait Args {
  def getArgs: Traversable[ArgAssignable]

  val parser = ArgumentParser(getArgs)

  def parse(args: Array[String]) {
    val parsedArgs = parser.parse(args)
    parsedArgs.foreach { case (argAssignable, valueHolder) =>
      argAssignable.setValue(valueHolder.value)
    }
  }

  def helpMessage = parser.helpMessage
}
