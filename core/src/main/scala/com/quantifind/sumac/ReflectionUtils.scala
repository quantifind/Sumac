package com.quantifind.sumac

import annotation.tailrec
import scala.reflect.runtime.{universe => ru}
import java.lang.reflect.{Type, ParameterizedType, Field}

object ReflectionUtils {

  @tailrec
  def getAllDeclaredFields(cls: Class[_], acc: Seq[Field] = Seq.empty): Seq[Field] = {
    val fields = acc ++ cls.getDeclaredFields
    Option(cls.getSuperclass) match {
      case Some(clazz) => getAllDeclaredFields(clazz, fields)
      case _ => fields
    }
  }

  def findGenericInterface(cls: Class[_], interface: Class[_]) : Option[ParameterizedType] = {
    val interfaces = cls.getGenericInterfaces
    //first check if this directly implements it
    findParameterizedType(interfaces, interface).orElse{
      //then check if its inherited by any of those interfaces
      interfaces.flatMap{tpe =>
        findGenericInterface(getRawClass(tpe), interface)
      }.headOption
    }
  }

  def getRawClass(tpe: Type) : Class[_] = {
    tpe match {
      case x: Class[_] => x
      case p: ParameterizedType => p.getRawType.asInstanceOf[Class[_]]
    }
  }

  def findParameterizedType(types: Array[Type], interface: Class[_]) : Option[ParameterizedType] = {
    types.find { tpe =>
      tpe match {
        case ptpe: ParameterizedType =>
          false
          //TODO
          //ParseHelper.checkType(ptpe, classOf[Argable[_]])
        case _ => false
      }
    }.map{_.asInstanceOf[ParameterizedType]}
  }

  def getRuntimeType[A](item: A): ru.Type = {
    val mirror = ru.runtimeMirror(this.getClass.getClassLoader)
    mirror.classSymbol(item.getClass).toType
  }

  def getTerms(typ: ru.Type): Traversable[ru.TermSymbol] = {
    typ.members.collect{case x if x.isTerm => x.asTerm}
  }

  def getVariableTerms(typ: ru.Type): Traversable[ru.TermSymbol] = {
    getTerms(typ).filter{x =>
      x.isCaseAccessor || x.isVal || x.isVar
    }
  }

  def extractGetterSetterPairs(typ: ru.Type): Traversable[GetterSetterPair] = {
    //scala reflection really pushes us towards just tracking getters & setters
    val terms = getTerms(typ)
    terms.filter{x => x.isGetter}.map{x => x -> x.setter}.
      filter{case(g,s) => s.isTerm}.map{case(g,s) => GetterSetterPair(g,s.asTerm)}
  }

  def termName(t: ru.TermSymbol): String = {
    t.name.toString.trim
  }

}

case class GetterSetterPair(getter: ru.TermSymbol, setter: ru.TermSymbol) {
  val name = ReflectionUtils.termName(getter)

  val fieldType = {
    //this is way more complicated than it should be. But
    // 1) getters for some reason are not instances of ru.MethodType
    //        java.lang.ClassCastException: scala.reflect.internal.Types$NullaryMethodType cannot be cast to scala.reflect.api.Types$MethodTypeApi
    // 2) its a headache to get the types out of setters
    val m = setter.typeSignature.
      asInstanceOf[ru.MethodType]
    m.params.head.typeSignature
  }
}