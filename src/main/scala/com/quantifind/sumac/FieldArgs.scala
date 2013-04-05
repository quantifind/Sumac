package com.quantifind.sumac

import java.lang.reflect.Field

trait FieldArgs extends Args {
  override def getArgs = ReflectionUtils.getAllDeclaredFields(getClass) collect {
    case f if (validField(f)) => new FieldArgAssignable(f, this)
  }

  def validField(f: Field): Boolean = {
    f.getName != "parser" && f.getName != "bitmap$0" && hasSetter(f) && !f.isAnnotationPresent(classOf[Ignore])
  }

  def hasSetter(f: Field): Boolean = {
    //all fields in scala private -- this is a way of checking if it has any public setter
    !f.getDeclaringClass.getMethods.filter{_.getName() == f.getName + "_$eq"}.isEmpty
  }
}

@deprecated("legacy naming")
trait FieldParsing extends FieldArgs
