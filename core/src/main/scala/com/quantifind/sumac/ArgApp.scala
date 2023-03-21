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

trait Argable[T <: Args] {

  protected lazy val argHolder = {
    val argClass = getArgumentClass
    ReflectionUtils.construct[T](argClass)
  }

  /**
   * get the instance of T that holds the parsed args.
   *
   * not needed for the user that just wants to run their code -- this is accessible just for other libs
   * built on top.
   */
  def getArgHolder: T = argHolder

  private[sumac] def getArgumentClass: Class[T] = {
    //we need to get the type parameter for Argable.  Doing that requires searching through the interfaces of *all*
    // classes in the type hierarchy.
    val argApp = ReflectionUtils.findGenericInterface(getClass, classOf[Argable[_]])
    ReflectionUtils.getRawClass(argApp.get.getActualTypeArguments.apply(0)).asInstanceOf[Class[T]]
  }
}

trait ArgMain[T <: FieldArgs] extends Argable[T] {
  def main(rawArgs: Array[String]): Unit = {
    mainHelper(rawArgs)
  }

  private def mainHelper(rawArgs: Array[String]): Unit = {
    try {
      argHolder.parse(rawArgs)
    } catch {
      case ex: FeedbackException =>
        println(ex.getMessage)
        System.exit(1)
    }
    main(argHolder)
  }

  def main(args: T): Unit
}

trait ArgFunction[T <: FieldArgs, U] extends Function[T, U] with Argable[T]

//below is just for testing, but want it in compiled classes ...

class MyArgs extends FieldArgs {
  var a: String = ""
  var b: Int = 0
}

object MyMain extends ArgMain[MyArgs] {
  def main(args: MyArgs): Unit = {
    println(args.a)
    println(args.b)
  }
}
