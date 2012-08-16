package optional

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 *
 */

class ApplicationTest extends FunSuite with ShouldMatchers {

//  test("readme parse args") {
//    //straight from the Readme
//    MyAwesomeCommandLineTool.main(Array("--count", "5", "quux"))
//    MyAwesomeCommandLineTool.args._1 should be (Some(5))
//    MyAwesomeCommandLineTool.args._2 should be (None)
//    MyAwesomeCommandLineTool.args._3 should be ("quux")
//  }
//
//  test("longNames"){
//    easy.main(Array("--aOne", "1", "--bTwo", "2", "--myDouble", "2.9", "--floatOptional", "2.3"))
//    easy.args._1 should be (1)
//    easy.args._2 should be (2)
//    easy.args._3 should be (2.9)
//    easy.args._4 should be (Some(2.3f))
//  }
//
//  test("conflicting short names"){
//
//    conflictingNames.main(Array("-a", "7", "-b", "18", "--longerName", "some string", "--lousyConflictingName", "another string"))
//    conflictingNames.args._1 should be (7)
//    conflictingNames.args._2 should be (18)
//    conflictingNames.args._3 should be ("some string")
//    conflictingNames.args._4 should be ("another string")
//
//    conflictingNames.main(Array("--a", "9", "--b", "10", "--longerName", "abcde", "--lousyConflictingName", "efgh"))
//    conflictingNames.args._1 should be (9)
//    conflictingNames.args._2 should be (10)
//    conflictingNames.args._3 should be ("abcde")
//    conflictingNames.args._4 should be ("efgh")
//
//
//    evaluating {
//      conflictingNames.main(Array("-a", "90", "-b", "100", "-l", "xyz", "--lousyConflictingName", "pqr"))
//    } should produce [Exception]
//  }


  test("register single char args") {
    registeredSingleChars.main(Array("-c", "5", "-d", "7.2"))
    registeredSingleChars.args._1 should be (5)
    registeredSingleChars.args._2 should be (7.2)
  }

//
//  test("short param from long variables") {
//    easy.main(Array("-a", "1", "-b", "2", "-m", "2.9", "-f", "2.3"))
//    easy.args._1 should be (1)
//    easy.args._2 should be (2)
//    easy.args._3 should be (2.9)
//    easy.args._4 should be (Some(2.3f))
//  }
//
//
//  //won't work as long as we use java reflection!
//  test("optional primitives") {
//    primitiveOptions.main(Array("--count", "5", "--double", "7.6"))
//    primitiveOptions.args._1 should be (Some(5))
//    primitiveOptions.args._2 should be (Some(7.6))
//
//    primitiveOptions.main(Array("--count", "1"))
//    primitiveOptions.args._1 should be (Some(1))
//    primitiveOptions.args._2 should be (None)
//
//    primitiveOptions.main(Array("--double", "13.9"))
//    primitiveOptions.args._1 should be (None)
//    primitiveOptions.args._2 should be (Some(13.9))
//
//    primitiveOptions.main(Array())
//    primitiveOptions.args._1 should be (None)
//    primitiveOptions.args._2 should be (None)
//  }
}

object MyAwesomeCommandLineTool extends optional.Application {
  //unfortunately, b/c this uses the java reflection api, it doesn't support Option for primitives
  var args: (Option[java.lang.Integer], Option[java.io.File], String) = _
  def main(count: Option[java.lang.Integer], file: Option[java.io.File], arg1: String) {
    args = (count, file, arg1)
  }
}

object easy extends optional.Application {
  var args : (Int, Int, Double, Option[java.lang.Float]) = _
  def main(aOne: Int, bTwo: Int, myDouble: Double, floatOptional: Option[java.lang.Float]) {
    args = (aOne, bTwo, myDouble, floatOptional)
  }
}

object conflictingNames extends optional.Application {
  var args : (Int, Int, String, String) = _
  def main(a: Int, b : Int, longerName: String, lousyConflictingName: String) {
    args = (a, b, longerName, lousyConflictingName)
  }
}

object primitiveOptions extends optional.Application {
  var args: (Option[Int], Option[Double]) = _
  def main(count: Option[Int], double: Option[Double]) {
    args = (count, double)
  }
}

object registeredSingleChars extends optional.Application {
  var args: (Option[Int], Option[Double]) = _
  register(ArgInfo('c', "count", false, "the count"), ArgInfo('d', "double", false, "a double"))
  def main(count: Option[Int], double: Option[Double]) {
    args = (count, double)
  }

}