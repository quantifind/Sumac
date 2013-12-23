package com.quantifind.sumac

import java.lang.annotation.Annotation
import scala.collection._
import com.quantifind.sumac.validation._
import scala.reflect.runtime.{universe => ru}

/**
 * Mix this trait into any class that you want to turn into an "argument holder".  It will automatically
 * convert each field of the class into a command line argument.  It will silently ignore fields that it doesn't
 * know how to parse.
 */
trait FieldArgs extends Args {
  private[sumac] override def getArgs(argPrefix:String, gettingDefaults: Boolean) = {
    val args: Seq[Seq[ArgAssignable]] = getMyTerms.toSeq collect {
      case f: GetterSetterPair if (isValidField(f)) => {
        val fa = TermArgAssignable(argPrefix, f, this)
//        if(!gettingDefaults) addAnnotationValidations(fa)
        Seq(fa)
      }
      //TODO nesting
//      case nested: ru.TermSymbol if (isNestedArgField(nested)) =>
//        val v = Option(nested.get(this)).getOrElse{
//          val t = nested.getType.newInstance()
//          nested.set(this, t)
//          t
//        }
//        val subArgs: Seq[ArgAssignable] = v.asInstanceOf[Args].getArgs(argPrefix + nested.getName + ".", gettingDefaults).toSeq
//        subArgs
    }
    args.flatten
  }

  def isSumacHelperField(f: GetterSetterPair): Boolean = f.name == "parser" || f.name == "bitmap$0"
  private[sumac] def getMyTerms: Traversable[GetterSetterPair] = {
    val tpe = ReflectionUtils.getRuntimeType(this)
    ReflectionUtils.extractGetterSetterPairs(tpe)
  }

  def isValidField(f: GetterSetterPair): Boolean = {
    val r = ParseHelper.findParser(f.fieldType).isDefined && !isSumacHelperField(f) //TODO ignore annotation
    r
  }

  def isNestedArgField(f: ru.TermSymbol): Boolean = {
    ru.typeOf[Args] =:= f.typeSignature //TODO wrong, need it to be a bound, not exact
  }

//  private[sumac] def addAnnotationValidations(f: FieldArgAssignable) {
//    val defaultVals = getDefaultArgs.map{a => a.getName -> a}.toMap
//    //Q: do inherited annotations mean anything on a field?  does it matter if I use getAnnotations vs getDeclaredAnnotations?
//    f.field.getAnnotations.foreach { annot =>
//      annotationValidationFunctions.get(annot.annotationType()).foreach{func =>
//        val default = defaultVals(f.getName).getCurrentValue
//        validationFunctions +:= new AnnotationValidationFunction(f, default, annot, func)
//      }
//    }
//  }
//
//  //just for debugging, nice to have a toString on this
//  private class AnnotationValidationFunction(
//    field: FieldArgAssignable,
//    default: Any,
//    annot: Annotation,
//    check: (Any, Any, Annotation, String) => Unit
//  ) extends (() => Unit) {
//    def apply() {
//      check(default, field.getCurrentValue, annot, field.getName)
//    }
//    override def toString() = "annotation check of " + field.getName
//  }

  @transient
  private[sumac] val annotationValidationFunctions = mutable.Map[Class[_ <: Annotation], (Any,Any, Annotation, String) => Unit]()


  /**
   * Use this function to make an annotation automatically imply a validation function.  This registers the annotation
   * with *this* instance, so that any use of the annotation automatically adds the validation function on the field.
   *
   * In general, if you create user-defined annotation validation functions, you will want to call this in a base trait,
   * which all your arg classes extend, so you can use those annotations anywhere.
   *
   * @param annotation the class of the annotation to add a validation function to
   * @param validationFunction the function that will be called to validate every field marked w/ the annotation.  The
   *                           first argument is the default value of the argument, the second is the current value,
   *                           the third is the annotation, and the fourth is the name of the argument (for error msgs).
   */
  def registerAnnotationValidation(annotation: Class[_ <: Annotation])(validationFunction: (Any,Any, Annotation, String) => Unit) {
    annotationValidationFunctions += annotation -> validationFunction
  }

  {
    //some built-in annotation validations
    registerAnnotationValidation(classOf[Required])(RequiredCheck)
    registerAnnotationValidation(classOf[Positive])(PositiveCheck)
    registerAnnotationValidation(classOf[Range])(RangeCheck)
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
  override def isValidField(f: GetterSetterPair): Boolean = {
    !isSumacHelperField(f) //TODO ignore annotation && !f.isAnnotationPresent(classOf[Ignore])
  }
}