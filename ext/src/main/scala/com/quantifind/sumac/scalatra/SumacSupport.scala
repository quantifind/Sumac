package com.quantifind.sumac.scalatra

import java.lang.reflect.Type
import java.lang.reflect.ParameterizedType
import com.quantifind.sumac.types.SelectInput
import com.quantifind.sumac.{ReflectionUtils, Args}
import org.joda.time.DateTime
import org.scalatra._
import org.scalatra.swagger._
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger.reflect.{Reflector,ScalaType}
import org.scalatra.servlet.ServletBase
import scala.reflect._


trait SumacSupport extends SwaggerSupport {
  self: ServletBase =>


  def getSwaggered[T <: Args: ClassTag](path: String)(api: Seq[Parameter] => OperationBuilder)(action: T => Any): Route = {
    val op: RouteTransformer = {
      val args = ReflectionUtils.construct(implicitly[ClassTag[T]])
      val params = SumacSupport.params(path, args)
      operation(api(params))
    }

    getArged(path, op)(action)
  }

  def getSimpleSwaggered[T <: Args: ClassTag](path: String)(action: T => Any): Route = {
    getSwaggered(path){params =>
      apiOperation[String](path) parameters (params: _*)
    }(action)
  }

  def getArged[T <: Args: ClassTag](transformers: RouteTransformer*)(action: T => Any): Route = {
    get(transformers: _*) {
      try {
        val args = ReflectionUtils.construct[T](implicitly[ClassTag[T]])
        ScalatraArgs.parse(args,this)
        action(args)
      } catch {
        case iie: IllegalArgumentException =>
          BadRequest(iie.toString())
      }
    }
  }
}

object SumacSupport {
  def params(path: String, args: Args): Seq[Parameter] = {
    args.getArgs("").toSeq.map{arg =>
      val allowed = arg.allowedValues.map{vs => AllowableValues(vs.toSeq: _*)}.getOrElse{AllowableValues.AnyValue}
      val name = arg.getName
      val isPath = path.indexOf(":" + name) >= 0
      val paramType = if(isPath) ParamType.Path else ParamType.Query
      val p = Parameter(
        name = arg.getName,
        `type` = SumacSupport.toSwaggerType(arg.getType),
        description = Some(arg.getDescription),
        paramType = paramType,
        required = arg.required || isPath,
        allowableValues = allowed
      )
      p
    }
  }

  def toSwaggerType(typ: Type): DataType = {
    typ match {
      case cls: Class[_] =>
        if (cls.isEnum()) {
          DataType.String
        } else {
          DatatypeHelper.getDatatype(cls)
        }
      case pt: ParameterizedType =>
        val raw = ReflectionUtils.getRawClass(pt)
        if (raw == classOf[SelectInput[_]]) {
          DataType.String
        } else {
          val st = toScalaType(pt)
          DatatypeHelper.getDatatype(st)
        }
    }
  }

  def toScalaType(typ: Type): ScalaType = {
    typ match {
      case cls: Class[_] => Reflector.scalaTypeOf(cls)
      case pt: ParameterizedType => new ParameterizedTypeScalaType(pt)
    }
  }


  class ParameterizedTypeScalaType(val paramType: ParameterizedType) extends ScalaType {
    def erasure = ReflectionUtils.getRawClass(paramType)
    def isArray = erasure.isArray
    def rawFullName = erasure.getName
    def rawSimpleName = erasure.getSimpleName
    def isPrimitive = false
    def isMap = classOf[Map[_, _]].isAssignableFrom(erasure)
    def isCollection = erasure.isArray || classOf[Iterable[_]].isAssignableFrom(erasure)
    def isOption = classOf[Option[_]].isAssignableFrom(erasure)

    val subTypes: Seq[ScalaType] = paramType.getActualTypeArguments.map{toScalaType}
    def typeArgs = subTypes

    def canEqual(that: Any): Boolean = that.isInstanceOf[ParameterizedTypeScalaType]

    def >:>(that: ScalaType): Boolean = throw new RuntimeException(">:>")
    def <:<(that: ScalaType): Boolean = throw new RuntimeException("<:<")
    def copy(erasure: Class[_],typeArgs: Seq[org.scalatra.swagger.reflect.ScalaType],typeVars: Map[java.lang.reflect.TypeVariable[_],org.scalatra.swagger.reflect.ScalaType]): org.scalatra.swagger.reflect.ScalaType = ???
    def fullName: String = rawFullName
    def simpleName: String = rawSimpleName
    def typeVars: Map[java.lang.reflect.TypeVariable[_],org.scalatra.swagger.reflect.ScalaType] = {
      erasure.getTypeParameters.zip(subTypes).toMap
    }

  }
}
