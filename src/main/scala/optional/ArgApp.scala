package optional

trait ArgApp[T <: FieldParsing] {

//  unfortunately, can't get this to work. can't make manifest available :(
//  def main(rawArgs: Array[String]) {
//    mainHelper(rawArgs)
//  }

  def mainHelper(rawArgs: Array[String])(implicit m: Manifest[T]) {
    val args = m.erasure.newInstance().asInstanceOf[T]
    args.parse(rawArgs)
    main(args)
  }

  def main(args: T) : Unit
}


//below is just for testing, but want it in compiled classes ...

class MyArgs extends FieldParsing {
  val a: String = ""
  val b: Int = 0
}

object MyApp extends ArgApp[MyArgs] {
  def main(args: Array[String]) {
    mainHelper(args)
  }
  def main(args: MyArgs) {
    println(args.a)
    println(args.b)
  }
}
