package com.quantifind.sumac

import com.typesafe.config.{ConfigFactory, Config}
import com.typesafe.config.impl.TypesafeHelper

/**
 * add this into an Arg trait to allow it to import & export from Typesafe Configs
 */
trait ConfigArgs extends ExternalConfig {
  self: Args =>

  def toTypesafeConfig(): Config = {
    val config = ConfigFactory.empty()
    val args = getStringValues
    val configOrigin = TypesafeHelper.makeConfigOrigin("Sumac Args")
    args.foldLeft(config){ case(prevConfig, (name, value)) =>
      prevConfig.withValue(name, TypesafeHelper.makeConfigString(configOrigin, value))
    }
  }
}
