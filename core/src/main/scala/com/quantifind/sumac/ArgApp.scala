package com.quantifind.sumac

trait Argable[T <: Args] {

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

  private[sumac] def getArgumentClass = {
    //we need to get the type parameter for Argable.  Doing that requires searching through the interfaces of *all*
    // classes in the type hierarchy.
    val argApp = ReflectionUtils.findGenericInterface(getClass, classOf[Argable[_]])
    ReflectionUtils.getRawClass(argApp.get.getActualTypeArguments.apply(0))
  }
}

trait ArgMain[T <: FieldArgs] extends Argable[T] {
  def main(rawArgs: Array[String]) {
    mainHelper(rawArgs)
  }

  private def mainHelper(rawArgs: Array[String]) {
    argHolder.parse(rawArgs)
    main(argHolder)
  }

  def main(args: T)
}

trait ArgFunction[T <: FieldArgs, U] extends Function[T, U] with Argable[T]

trait ArgApp[T <: FieldArgs] extends Argable[T] with App {
  override def main(args: Array[String]) {
    argHolder.parse(args)
    super.main(args)
  }
}

//below is just for testing, but want it in compiled classes ...

class MyArgs extends FieldArgs {
  var a: String = ""
  var b: Int = 0
}

object MyMain extends ArgMain[MyArgs] {
  def main(args: MyArgs) {
    println(args.a)
    println(args.b)
  }
}
