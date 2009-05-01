package optional.examples

import java.io.File
import java.lang.reflect

//
// A fancier example showing some other features.
//

// Any class can be used as a parameter type if it has a one
// argument String constructor, like this one does.
class Whatever(x: String) {
  override def toString() = "Whatever(" + x + ")"
}

object Fancy extends optional.Application
{
  // Function1[String,T] methods in the class will be used to convert arguments
  // to the desired type, unless isConversionMethod excludes them.
  override def isConversionMethod(m: reflect.Method) = m.getName != "excludedConversionMethod"
  
  // this one will be ignored
  def excludedConversionMethod(s: String): List[Int] = List(1,2,3)

  // these will be used
  def arbitraryName(s: String): Set[Char] = Set(s : _*)
  def anotherArbitraryName(s: String): List[Float] = List(1.1f, 2.2f)
  
  def main(
    file: Option[File],
    what: Option[Whatever],
    chars: Set[Char],
    arg1: Double)
  {
    // getRawArgs() returns the command line in its original form.
    println("   Raw arguments: " + getRawArgs())
    
    // getArgs() returns all the positional arguments (those not associated with an --option.)
    println("Unprocessed args: " + getArgs())
    
    // the conversion defined above was used to create 'chars' from the string argument
    println("Set of chars == " + (chars mkString " "))
  }
}
