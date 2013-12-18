package com.quantifind.sumac

import scala.collection.Map
import com.typesafe.config.{ConfigFactory, Config}
import scala.util.Try

/**
 * TODO DOC
 * User: andrews
 * Date: 12/18/13
 */
trait ConfigArgs extends ExternalConfig {
  self: Args =>

  def configPrefix: String

  def config: Config = ConfigFactory.load()


  abstract override def readArgs(originalArgs: Map[String, String]): Map[String, String] = {
    val expected = self.getArgs("").map(_.getName).toSeq
    val originalNames = originalArgs.keys.toSeq
    val missing = expected.diff(originalNames)
    val newArgs = originalArgs ++ missing.map {
      name =>
        val key = s"$configPrefix.$name"
        Try{ (name -> config.getString(key)) }.toOption
    }.flatten
    super.readArgs(newArgs)
  }

}

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
