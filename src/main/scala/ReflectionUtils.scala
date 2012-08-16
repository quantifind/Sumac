package optional

import scala.collection._
import java.lang.reflect.Field

/**
 *
 */

object ReflectionUtils {
  def getAllDeclaredFields(cls: Class[_]) : mutable.Buffer[Field]= {
    val fields = mutable.Buffer[Field]()
    getAllDeclaredFields(cls, fields)
    fields
  }

  def getAllDeclaredFields(cls: Class[_], holder: mutable.Buffer[Field]) : Unit = {
    holder ++= cls.getDeclaredFields
    val superCls = cls.getSuperclass
    if (superCls != null)
      getAllDeclaredFields(superCls, holder)
  }
}
