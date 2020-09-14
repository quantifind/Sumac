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

import collection.Map

/**
 * Some external source of config information.  It both allows config to be read from, and saved to, some external
 * format.
 */
trait ExternalConfig {
  self: Args =>

  /**
   * Read config from an external source, and use that to modify the set of arguments.  The "original" arguments
   * are given as an argument to this function, so that this method can base its behavior on those arguments.  (Eg.,
   * it can take a filename from those args.)  It should return the complete set of args that should be used.  It is
   * free to choose to completely replace the original args, append to them, modify them, etc.
   *
   * in general, implementations should be abstract override with a call to super, to allow multiple external configs
   * via the Stackable Trait Pattern
   */
  def readArgs(originalArgs: Map[String,String]): Map[String,String]

  /**
   * save the config back to the external source.  Any parameters for this method should have already been extracted
   * from the call to readArgs
   *
   * as with readArgs, this should in general be implemented as an abstract override with a call to super
   */
  def saveConfig(): Unit
}

object ExternalConfigUtil {

  def mapWithDefaults(original: Map[String,String], defaults: Map[String,String]):Map[String,String] = {
    defaults.foldLeft(original){case (orig, (k,v)) =>
      if (orig.contains(k))
        orig
      else
        orig + (k -> v)
    }
  }
}


/**
 * a mixin for ExternalConfig to make sure that the parse and setting of fields is done before the ExternalConfig
 *  is processed. Mix it in first. A second parsing and the validation will take place after the ExternalConfig is
 *  applied.
 */
trait PreParse extends ExternalConfig {
  self: Args =>

  abstract override def readArgs(originalArgs: Map[String, String]): Map[String, String] = {
    val parsedArgs = argParser.parse(originalArgs)
    parsedArgs.foreach { case (argAssignable, valueHolder) =>
      argAssignable.setValue(valueHolder.value)
    }
    super.readArgs(originalArgs)
  }

}
