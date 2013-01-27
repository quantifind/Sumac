package optional

import scala.collection._
import annotation.tailrec
import java.lang.reflect.Field

/**
 *
 */

object ReflectionUtils {
  def getAllDeclaredFields(cls: Class[_]) : mutable.Buffer[Field] = {
    val fields = mutable.Buffer[Field]()
    getAllDeclaredFields(cls, fields)
    fields
  }

  @tailrec
  def getAllDeclaredFields(cls: Class[_], holder: mutable.Buffer[Field]) {
    holder ++= cls.getDeclaredFields
    val superCls = cls.getSuperclass
    if (superCls != null)
      getAllDeclaredFields(superCls, holder)
  }
}
