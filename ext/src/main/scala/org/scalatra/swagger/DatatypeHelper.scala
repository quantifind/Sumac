package org.scalatra.swagger

import org.scalatra.swagger.reflect.ScalaType

/**
 * just to access some package-protected fields in swagger ...
 */
object DatatypeHelper {
  def getDatatype(cls: Class[_]): DataType = DataType.fromClass(cls)
  def getDatatype(st: ScalaType): DataType = DataType.fromScalaType(st)
}
