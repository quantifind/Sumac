package com.quantifind.sumac

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import com.quantifind.sumac.validation.{Positive, Required, Range}

class ValidationSuite extends FunSuite with ShouldMatchers {

  def parse(args: Map[String,String], msg: String)(builder: => FieldArgs) {
    val a = builder
    val exc = withClue(args){evaluating {a.parse(args)} should produce[ArgException]}
    withClue(args){exc.getMessage should include(msg)}
  }

  test("@Required") {
    def parseInt(args: Map[String,String], msg: String) = {
      parse(args, msg){new IntRequiredArgs()}
    }

    parseInt(Map("a" -> "1"), "must specify a value for b")

    parseInt(Map("b" -> "1"), "must specify a value for a")
    //also an error if values are given, but they match the defaults
    parseInt(Map("a" -> "0", "b" -> "7"), "must specify a value for ")

    val intArgs = new IntRequiredArgs()
    intArgs.parse(Map("a" -> "1", "b" -> "0"))
    intArgs.a should be (1)
    intArgs.b should be (0)

    //make sure that the checks still apply when called programmatically (doesn't depend on strings at all)
    intArgs.a = 0
    evaluating {intArgs.runValidation()} should produce[ArgException]


    def parseString(args: Map[String,String], msg: String) = {
      parse(args, msg){new StringRequiredArgs()}
    }

    parseString(Map("e" -> "a"), "must specify a value for f")
    parseString(Map("f" -> "hi"), "must specify a value for e")
    parseString(Map("e" -> "<null>", "f" -> "hi"), "must specify a value for e")
    parseString(Map("e" -> "blah", "f" -> "blah"), "must specify a value for f")

  }

  test("@Positive") {
    def parseP(args: Map[String,String], msg: String) {
      parse(args, msg){new PositiveArgs()}
    }

    parseP(Map("c" -> "1.0"), "must specify a positive value for a")
    parseP(Map("a" -> "3"), "must specify a positive value for c")
    parseP(Map("a" -> "3", "c" -> "-3.8"), "must specify a positive value for c")
    parseP(Map("a" -> "-3", "c" -> "3.8"), "must specify a positive value for a")

    val a = new PositiveArgs()
    a.parse(Map("a" -> "1", "c" -> "7.9"))
    a.a should be (1)
    a.c should be (7.9f)
  }

  test("@Range") {
    def parseR(args: Map[String,String], msg: String) {
      parse(args, msg) {new RangeArgs()}
    }

    val msgX = "must specify a value between 3.0 and 8.0 for x"
    parseR(Map("y" -> "-80"), msgX)
    parseR(Map("x"->"1", "y" -> "-80"), msgX)
    parseR(Map("x" -> "9", "y" -> "-80"), msgX)
    val msgY = "must specify a value between -83.0 and -72.0 for y"
    parseR(Map("x" -> "5"), msgY)
    parseR(Map("x" -> "5", "y" -> "5"), msgY)
    parseR(Map("x" -> "5", "y" -> "-90"), msgY)

    val a = new RangeArgs()
    a.parse(Map("x"->"4", "y" -> "-77"))
    a.x should be (4)
    a.y should be (-77)
  }

  test("user-defined") {
    //silly example of user-defined annotation validations
    parse(Map("x" -> "7"), "x must be 3 or 4"){new UserDefinedAnnotationArgs()}
    val a1 = new UserDefinedAnnotationArgs()
    a1.parse(Map("x" -> "3"))
    a1.x should be (3)

    //this arg class hasn't registered any validation w/ the annotation, so it is irrelevant
    val a2 = new UnregisteredAnnotationArgs()
    a2.parse(Map("x" -> "7"))
    a2.x should be (7)
  }

  test("multi-annotation") {
    parse(Map("b"->"-4"), "must specify a positive value for b"){new MultiAnnotationArgs}
    parse(Map("b"->"7"), "must specify a value for b"){new MultiAnnotationArgs}

    val a = new MultiAnnotationArgs()
    a.parse(Map("b" -> "3"))
    a.b should be (3)
  }
}

class IntRequiredArgs extends FieldArgs {
  @Required
  var a: Int = _
  @Required
  var b: Int = 7
  var c = 19
}

class StringRequiredArgs extends FieldArgs {
  @Required
  var e: String = _
  @Required
  var f: String = "blah"
}

class PositiveArgs extends FieldArgs {
  @Positive
  var a: Int = _
  var b: Int = _
  @Positive
  var c: Float = _
  var d: Float = _
}

class RangeArgs extends FieldArgs {
  @Range(min=3,max=8)
  var x: Int = _
  @Range(min= -83, max= -72)
  var y: Float = _
}


class MultiAnnotationArgs extends FieldArgs {
  @Positive @Required
  var b = 7
}

class UserDefinedAnnotationArgs extends FieldArgs {
  @ThreeOrFour
  var x: Int = _

  registerAnnotationValidation(classOf[ThreeOrFour]){(_, value, _, name) =>
    if (value != 3 && value != 4) {
      throw new ArgException(name + " must be 3 or 4")
    }
  }
}

class UnregisteredAnnotationArgs extends FieldArgs {
  @ThreeOrFour
  var x: Int = _
}
