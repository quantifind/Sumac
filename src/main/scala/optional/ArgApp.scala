package optional

import java.lang.reflect.{ParameterizedType, Type}

trait ArgApp[T <: FieldParsing] {

  def main(rawArgs: Array[String]) {
    mainHelper(rawArgs)
  }

  private lazy val argHolder = {
    val argClass = getArgumentClass()
    val ctors = argClass.getDeclaredConstructors()
    val ctor = ctors.find(ctor => ctor.getGenericParameterTypes.length == 0).get
    ctor.setAccessible(true)
    ctor.newInstance().asInstanceOf[T]
  }

  /**
   * get the instance of T that holds the parsed args.
   *
   * not needed for the user that just wants to run their code -- this is accessible just for other libs
   * built on top.
   */
  def getArgHolder : T = argHolder

  private def getArgumentClass() = {
    val argApp = this.getClass.getGenericInterfaces.find{tpe =>
      tpe match {
        case ptpe: ParameterizedType =>
          ParseHelper.checkType(ptpe, classOf[ArgApp[_]])
        case _ => false
      }
    }
    getRawClass(argApp.get.asInstanceOf[ParameterizedType].getActualTypeArguments.apply(0))
  }

  private def getRawClass(tpe: Type) = {
    tpe match {
      case x:Class[_] => x
      case p:ParameterizedType => p.getRawType.asInstanceOf[Class[_]]
    }
  }

  private def mainHelper(rawArgs: Array[String]) {
    argHolder.parse(rawArgs)
    main(argHolder)
  }

  def main(args: T) : Unit
}

//below is just for testing, but want it in compiled classes ...

class MyArgs extends FieldParsing {
  val a: String = ""
  val b: Int = 0
}

object MyApp extends ArgApp[MyArgs] {
  def main(args: MyArgs) {
    println(args.a)
    println(args.b)
  }
}
