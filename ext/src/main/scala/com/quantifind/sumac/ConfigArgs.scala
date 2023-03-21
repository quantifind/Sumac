package com.quantifind.sumac

import scala.collection.Map
import scala.util.Try

import com.typesafe.config.{Config, ConfigFactory}

/**
 * A mixin to load fallback values for args from a typesafe config file. Arguments are filled in in this order:
 * 1- if it's provided on the command line, top priority
 * 2- if it's in the config file and not on the command line, use the config
 * 3- if nothing is provided, use default value
 *
 * By default, the ConfigArgs loads the configs with {{{ConfigFactory.load()}}}, so it will use the typesafe config
 * default loading sequence (reference.conf, application.conf, ...), you can change this by either changing {{{configFiles}}}
 * or {{{useDefaultConfig}}}.
 *
 * Values are parsed by the Sumac parser and not by the typesafe config parser.
 *
 * User: andrews
 * Date: 12/18/13
 */
trait ConfigArgs extends ExternalConfig {
  self: Args =>

  protected val stripQuotes = """(^"|"$)""".r

  /**
   * the list of config to loads, the rightmost config will be the preferred one, the others will be used as fallback.
   * (in order from right to left)
   */
  var configFiles: List[String] = List()
  /**
   * should the default config from typesafe lib (i.e. the one loaded by {{{ConfigFactory.load()}}}) be the leftmost
   * fallback in the configFiles list.
   */
  var useDefaultConfig = true

  /**
   * the prefix to use to search for values in the config file. The values will be searched with the key:
   * configPrefix + '.' + argumentName
   *
   * @return the prefix to use. Should not include the trailing '.'
   */
  def configPrefix: String

  /**
   * the Config to use to lookup values
   * @return
   */
  def makeConfig(originalArgs: Map[String, String]): Config = {
    val startWith = if (useDefaultConfig) {
      ConfigFactory.load()
    } else {
      ConfigFactory.empty
    }
    configFiles.foldLeft(startWith) {
      case (conf, file) =>
        ConfigFactory.load(file).withFallback(conf)
    }
  }

  /**
   * add a config to the stack of files to load
   * @param filename
   */
  def addConfig(filename: String): Unit = {
    configFiles = configFiles :+ filename
  }

  abstract override def readArgs(originalArgs: Map[String, String]): Map[String, String] = {
    //figure out configFiles and useDefaultConfig
    originalArgs.get("configFiles") foreach {
      files =>
        configFiles = files.split(",").toList
    }
    originalArgs.get("useDefaultConfig") foreach {
      d =>
        useDefaultConfig = d.toBoolean
    }


    //find out the expected arguments
    val expected = self.getArgs("").map(_.getName).toSeq
    //find out the ones provided on the command line
    val originalNames = originalArgs.keys.toSeq
    //get the ones we are missing
    val missing = expected.diff(originalNames)
    val conf = makeConfig(originalArgs)
    val newArgs = originalArgs ++ missing.flatMap {
      name =>
      //and find them in the config file
        val key = s"$configPrefix.$name"
        Try {
          name -> stripQuotes.replaceAllIn(conf.getValue(key).render, "")
        }.toOption //ignore missing values
    }
    super.readArgs(newArgs)
  }

}

/**
 * this mixin trait allows you to create the name of the config file to load from an argument on the command line.
 * An application of this is to specify an environment on the command line and load different configurations based on
 * the environment (prod, test, dev, ...), could also change depending on the user, or the server used, etc.
 *
 * {{{
 *  class ArgsWithEnv with ConfigFromArg {

      var env: String = "dev"

      useDefaultConfig = false
      def makeConfigFilename(originalArgs: Map[String, String]): Option[String] = {
        originalArgs.get("env") match {
          case Some(e) => Some(e) //load $e.conf as configuration
          case _ => None
        }
      }
    }
 * }}}
 */
trait ConfigFromArg extends PreParse with ConfigArgs {
  self: Args =>

  /**
   * create the filename of the config file to load based on one, or multiple, command line argument(s).
   * Override it as a def or a lazy val, but DO NOT use a val as it would just fix it to the value of the default.
   *
   * @return maybe a name of a config file.
   */
  def configFilenameFromArg: Option[String]

  abstract override def makeConfig(originalArgs: Map[String, String]): Config = {
    configFilenameFromArg foreach {
      f =>
        addConfig(f)
    }
    super.makeConfig(originalArgs)
  }

}



