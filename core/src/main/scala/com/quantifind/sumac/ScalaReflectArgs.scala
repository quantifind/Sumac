package com.quantifind.sumac

import scala.reflect.runtime.{universe => ru}

/**
* discovers fields using scala reflection from 2.10, which allows us to get proper type params for primitives
*/
trait ScalaReflectArgs{
  implicit def tpe : scala.reflect.api.Types#Type
  def getArgs(argPrefix: String): Seq[ArgAssignable] = {
    println(tpe)
    println(tpe.members)
//    val m = ru.runtimeMirror(getClass.getClassLoader)
//    //TODO get a *Type* from *this*
//    val instanceMirror = m.reflect(this)
//    val cm = m.reflectClass(instanceMirror.symbol)
//    cm.symbol
//    println(cm)
//
//    //this is what I really want to do ... but I need a way to get typeOf on *this*, where the type is determined at runtime
////    ru.typeOf[XXX].members.filter{m => m.isField && m.isVariable}.map{m => m.asTerm.typeSignature}
    Nil
  }
}