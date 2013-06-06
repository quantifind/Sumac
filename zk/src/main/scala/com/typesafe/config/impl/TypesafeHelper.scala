package com.typesafe.config.impl

import com.typesafe.config.ConfigOrigin

/**
 * this is just a cheat to get around the access control of typesafes config objects
 */
object TypesafeHelper {

  def makeConfigString(origin: ConfigOrigin, value: String) = {
    new ConfigString(origin, value)
  }

  def makeConfigOrigin(description: String) = {
    SimpleConfigOrigin.newSimple(description)
  }
}
