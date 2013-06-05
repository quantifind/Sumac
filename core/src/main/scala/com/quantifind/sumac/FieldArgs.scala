package com.quantifind.sumac

import java.lang.reflect.Field

/**
 * Mix this trait into any class that you want to turn into an "argument holder".  It will automatically
 * convert each field of the class into a command line argument.  It will silently ignore fields that it doesn't
 * know how to parse.
 */
trait FieldArgs extends Args {
  override def getArgs(argPrefix:String) = {
    val args: Seq[Seq[ArgAssignable]] = ReflectionUtils.getAllDeclaredFields(getClass) collect {
      case f: Field if (isValidField(f)) => Seq(FieldArgAssignable(argPrefix, f, this))
      case nested: Field if (isNestedArgField(nested)) =>
        nested.setAccessible(true)
        val v = Option(nested.get(this)).getOrElse{
          val t = nested.getType.newInstance()
          nested.set(this, t)
          t
        }
        val subArgs: Seq[ArgAssignable] = v.asInstanceOf[Args].getArgs(argPrefix + nested.getName + ".").toSeq
        subArgs
    }
    args.flatten
  }

  def isSumacHelperField(f: Field): Boolean = f.getName == "parser" || f.getName == "bitmap$0"

  def isValidField(f: Field): Boolean = {
    ParseHelper.findParser(f.getType).isDefined && !isSumacHelperField(f) && hasSetter(f) && !f.isAnnotationPresent(classOf[Ignore])
  }

  def isNestedArgField(f: Field): Boolean = {
    classOf[Args].isAssignableFrom(f.getType)
  }

  def hasSetter(f: Field): Boolean = {
    //all fields in scala are private -- this is a way of checking if it has any public setter
    f.getDeclaringClass.getMethods.exists{_.getName() == f.getName + "_$eq"}
  }
}

/**
 * Use this trait if you want an exception anytime your Argument class has a field that we don't know how to parse.
 * (FieldArgs just silently ignores those fields).
 *
 * Yes, the name is long -- if you want to use this as your standard way of parsing arguments, just alias it to a
 * shorter name in your projects.
 */
trait FieldArgsExceptionOnUnparseable extends FieldArgs {
  override def isValidField(f: Field): Boolean = {
    !isSumacHelperField(f) && hasSetter(f) && !f.isAnnotationPresent(classOf[Ignore])
  }
}