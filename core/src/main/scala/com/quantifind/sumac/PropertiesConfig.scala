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

import collection._
import java.util.Properties
import java.io.{FileOutputStream, File, FileInputStream, BufferedInputStream}

import collection.JavaConverters._

/**
 * Mix this into your Argument class to add the ability to read your config from a property file
 */
trait PropertiesConfig extends ExternalConfig {
  self: Args =>

  var propertyFile: File = _

  abstract override def readArgs(originalArgs: Map[String,String]): Map[String,String] = {
    parse(originalArgs, false)

    val props = new Properties()
    if (propertyFile != null) {
      val in = new BufferedInputStream(new FileInputStream(propertyFile))
      props.load(in)
      in.close()
    }
    //append args we read from the property file to the args from the command line, and pass to next trait
    super.readArgs(ExternalConfigUtil.mapWithDefaults(originalArgs,props.asScala))
  }

  abstract override def saveConfig() {
    PropertiesConfig.saveConfig(this, propertyFile)
    super.saveConfig()
  }

}

object PropertiesConfig {
  def saveConfig(args: Args, propertyFile: File) {
    val props = new Properties()
    args.getStringValues.foreach{case(k,v) => props.put(k,v)}
    val out = new FileOutputStream(propertyFile)
    props.store(out, "")
    out.close()
  }
}