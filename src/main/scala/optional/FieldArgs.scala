package optional

trait FieldArgs extends Args {
  override
  def getArgs = {
    ReflectionUtils.getAllDeclaredFields(this.getClass).
      filter{f => f.getName != "parser" && f.getName != "bitmap$0"}.map{f => new FieldArgAssignable(f, this)}
  }
}

/**
 * @deprecated legacy naming
 */
trait FieldParsing extends FieldArgs