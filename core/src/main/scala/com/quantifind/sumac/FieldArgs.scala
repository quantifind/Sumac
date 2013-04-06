package com.quantifind.sumac

import java.lang.reflect.Field

trait FieldArgs extends Args {
  override def getArgs = ReflectionUtils.getAllDeclaredFields(getClass) collect {
    case f if (isValidField(f)) => new FieldArgAssignable(f, this)
  }

  def isValidField(f: Field): Boolean = {
    f.getName != "parser" && f.getName != "bitmap$0" && hasSetter(f) && !f.isAnnotationPresent(classOf[Ignore])
  }

  def hasSetter(f: Field): Boolean = {
    //all fields in scala are private -- this is a way of checking if it has any public setter
    f.getDeclaringClass.getMethods.exists{_.getName() == f.getName + "_$eq"}
  }
}

@deprecated("legacy naming")
trait FieldParsing extends FieldArgs
