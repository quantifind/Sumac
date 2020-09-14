/**
  * Copyright 2012-2020 Quantifind, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */

package com.quantifind.sumac

import annotation.tailrec
import java.lang.reflect.{Type, ParameterizedType, Field}

import scala.reflect.ClassTag


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
        case ptpe: ParameterizedType => ParseHelper.checkType(ptpe, classOf[Argable[_]])
        case _ => false
      }
    }.map{_.asInstanceOf[ParameterizedType]}
  }

  def construct[T](clz: Class[T]): T = {
    val ctors = clz.getDeclaredConstructors
    ctors.find(_.getGenericParameterTypes.length == 0) match {
      case Some(ctor) =>
        ctor.setAccessible(true)
        ctor.newInstance().asInstanceOf[T]
      case None => throw new AssertionError("No zero-arg constructor found")
    }
  }

  def construct[T](tag: ClassTag[T]): T = {
    construct(tag.runtimeClass.asInstanceOf[Class[T]])
  }
}
