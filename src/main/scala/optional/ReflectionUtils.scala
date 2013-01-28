package optional

import annotation.tailrec
import java.lang.reflect.Field

object ReflectionUtils {

  @tailrec
  def getAllDeclaredFields(cls: Class[_], acc: Seq[Field] = Seq.empty): Seq[Field] = {
    val fields = acc ++ cls.getDeclaredFields
    Option(cls.getSuperclass) match {
      case Some(clazz) => getAllDeclaredFields(clazz, fields)
      case _ => fields
    }
  }
}
