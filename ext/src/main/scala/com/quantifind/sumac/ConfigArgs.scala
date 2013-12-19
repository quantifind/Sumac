package com.quantifind.sumac

import scala.collection.Map
import com.typesafe.config.{ConfigFactory, Config}
import scala.util.Try

/**
 * A mixin to load fallback values for args from a typesafe config file. Arguments are filled in in this order:
 *   1- if it's provided on the command line, top priority
 *   2- if it's in the config file and not on the command line, use the config
 *   3- if nothing is provided, use default value
 *
 * By default, the ConfigArgs loads the configs with {{{ConfigFactory.load()}}}, so it will use the typesafe config
 *  default loading sequence (reference.conf, application.conf, ...), you can change this by either overiding the config
 *  field or by mixin in the ConfigSetter trait to get a setter for the config.
 *
 * Values are parsed by the Sumac parser and not by the typesafe config parser.
 *
 * User: andrews
 * Date: 12/18/13
 */
trait ConfigArgs extends ExternalConfig {
  self: Args =>

  /**
   * the prefix to use to search for values in the config file. The values will be searched with the key:
   *   configPrefix + '.' + argumentName
   *
   * @return the prefix to use. Should not include the trailing '.'
   */
  def configPrefix: String

  /**
   * the Config to use to lookup values
   * @return
   */
  def config: Config = ConfigFactory.load()


  abstract override def readArgs(originalArgs: Map[String, String]): Map[String, String] = {
    //find out the expected arguments
    val expected = self.getArgs("").map(_.getName).toSeq
    //find out the ones provided on the command line
    val originalNames = originalArgs.keys.toSeq
    //get the ones we are missing
    val missing = expected.diff(originalNames)
    val newArgs = originalArgs ++ missing.map {
      name =>
        //and find them in the config file
        val key = s"$configPrefix.$name"
        Try{ (name -> config.getString(key)) }.toOption //ignore missing values
    }.flatten
    super.readArgs(newArgs)
  }

}

/**
 * Use this mixin on a ConfigArgs to have access to a setter for the config to use to lookup fallback values.
 *
 * {{{
 *   class Test extends FieldArgs with ConfigArgs {
 *      val configPrefix = "aprefix.test"
 *
 *      var arg = "test"
 *   }
 *
 *   val parsed = new Test with ConfigSetter
 *   parsed.config = ConfigLoader.load("otherConfig")
 *   parsed.parse(args)
 *
 * }}}
 *
 */
trait ConfigSetter {
  self: ConfigArgs =>

  @Ignore
  protected var _config: Option[Config] = None

  abstract override def config = _config match {
    case Some(conf) => conf
    case None => ConfigFactory.load()
  }

  def config_=(conf: Config) {
    _config = Some(conf)
  }


}
