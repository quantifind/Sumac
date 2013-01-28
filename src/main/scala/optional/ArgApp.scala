package optional

import java.lang.reflect.{ParameterizedType, Type}

trait Argable[T <: FieldParsing] {

  protected lazy val argHolder = {
    val argClass = getArgumentClass
    val ctors = argClass.getDeclaredConstructors
    ctors.find(_.getGenericParameterTypes.length == 0) match {
      case Some(ctor) =>
        ctor.setAccessible(true)
        ctor.newInstance().asInstanceOf[T]
      case None => throw new AssertionError("No zero-arg constructor found")
    }
  }

  /**
   * get the instance of T that holds the parsed args.
   *
   * not needed for the user that just wants to run their code -- this is accessible just for other libs
   * built on top.
   */
  def getArgHolder: T = argHolder

  private def getArgumentClass = {
    val argApp = getClass.getGenericInterfaces.find { tpe =>
      tpe match {
        case ptpe: ParameterizedType => ParseHelper.checkType(ptpe, classOf[Argable[_]])
        case _ => false
      }
    }
    getRawClass(argApp.get.asInstanceOf[ParameterizedType].getActualTypeArguments.apply(0))
  }

  private def getRawClass(tpe: Type) = {
    tpe match {
      case x: Class[_] => x
      case p: ParameterizedType => p.getRawType.asInstanceOf[Class[_]]
    }
  }

}

trait ArgMain[T <: FieldParsing] extends Argable[T] {
  def main(rawArgs: Array[String]) {
    mainHelper(rawArgs)
  }

  private def mainHelper(rawArgs: Array[String]) {
    argHolder.parse(rawArgs)
    main(argHolder)
  }

  def main(args: T)
}

trait ArgFunction[T <: FieldParsing, U] extends Function[T, U] with Argable[T]

trait ArgApp[T <: FieldParsing] extends Argable[T] with App {
  override def main(args: Array[String]) {
    argHolder.parse(args)
    super.main(args)
  }
}

//below is just for testing, but want it in compiled classes ...

class MyArgs extends FieldParsing {
  val a: String = ""
  val b: Int = 0
}

object MyMain extends ArgMain[MyArgs] {
  def main(args: MyArgs) {
    println(args.a)
    println(args.b)
  }
}

object MyApp extends ArgApp[MyArgs] {
  println(argHolder.a)
  println(argHolder.b)
}
