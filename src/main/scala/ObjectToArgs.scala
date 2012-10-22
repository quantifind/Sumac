package optional

/**
 *
 */

class ObjectToArgs(val obj: Object) {
  val argParser = new ArgumentParser[FieldArgAssignable](
    ReflectionUtils.getAllDeclaredFields(obj.getClass).map{f => new FieldArgAssignable(f)}
  )

  def parse(args: Array[String],
            preParsers: Iterator[Parser[_]] = Iterator(),
            postParsers: Iterator[Parser[_]] = Iterator()) {
    val parsed = argParser.parse(args, preParsers, postParsers)
    parsed.foreach{
      kv =>
        val field = kv._1.field
        field.setAccessible(true)
        field.set(obj, kv._2.value)
    }
  }

  def helpMessage = argParser.helpMessage
}

trait FieldParsing {
  lazy val parser = new ObjectToArgs(this)
  def parse(args: Array[String],
            preParsers: Iterator[Parser[_]] = Iterator(),
            postParsers: Iterator[Parser[_]] = Iterator()) {
    parser.parse(args, preParsers, postParsers)
  }

  def helpMessage = parser.helpMessage
}