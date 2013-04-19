package com.quantifind.sumac

import collection._
import java.util.Properties
import java.io.{File, FileInputStream, BufferedInputStream}

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
    super.readArgs(originalArgs ++ props.asScala)
  }

}