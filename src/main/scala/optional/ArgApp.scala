package optional

import java.lang.reflect.{ParameterizedType, Type}

trait ArgApp[T <: FieldParsing] {

  def main(rawArgs: Array[String]) {
    mainHelper(rawArgs)
  }

  private def getArgumentClass() = {
    val argApp = this.getClass.getGenericInterfaces.find{tpe =>
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      ParseHelper.checkType(ptpe, classOf[ArgApp[_]])
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
    val argClass = getArgumentClass()
    val ctors = argClass.getDeclaredConstructors()
    val ctor = ctors.find(ctor => ctor.getGenericParameterTypes.length == 0).get
    ctor.setAccessible(true)
    val args = ctor.newInstance().asInstanceOf[T]
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
  def main(args: MyArgs) {
    println(args.a)
    println(args.b)
  }
}
