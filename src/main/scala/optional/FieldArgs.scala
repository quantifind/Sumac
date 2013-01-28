package optional

trait FieldArgs extends Args {
  override def getArgs = ReflectionUtils.getAllDeclaredFields(getClass) collect {
    case f if (f.getName != "parser" && f.getName != "bitmap$0") => new FieldArgAssignable(f, this)
  }
}

@deprecated("legacy naming")
trait FieldParsing extends FieldArgs