package com.quantifind.sumac.scalatra

import com.quantifind.sumac.{StandardDateTimeParse, FieldArgs}
import com.quantifind.sumac.validation.{Positive, Required}
import org.joda.time.DateTime
import org.json4s._
import org.json4s.native.JsonParser
import org.scalatra._
import org.scalatra.swagger._
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatest.OptionValues._

class SumacSupportTest extends ScalatraFunSuite {


  val apiInfo = ApiInfo(
    title = "Swagger Sample App",
    description = "blah",
    termsOfServiceUrl = "http://helloreverb.com/terms/",
    contact = "apiteam@wordnik.com",
    license = "Apache 2.0",
    licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.html"
  )

  val swagger = new Swagger("1.2", "1.0.0", apiInfo)

  addServlet(new BlahServlet(swagger), "/blah/*")
  addServlet(new WakkaServlet(swagger), "/wakka/*")
  addServlet(new SwaggerResourcesServlet(swagger), "/api-docs/*")




  test("basic parsing") {
    get("/blah/foo/wakka?count=3") {
      status should be (200)
      body should be ("wakka,wakka,wakka")
    }


    get("/blah/foo/wakka?count=blahdiblah") {
      status should be (400)
    }
  }

  test("complex parsing") {
    get("/blah/required/blah") { status should be (400)}
    get("/blah/required/blah?req=-5") { status should be (400)}
    get("/blah/required/blah?req=-1") { status should be (400)}
    get("/blah/required/blah?req=0") { status should be (400)}
    get("/blah/required/blah?req=1") {
      status should be (200)
      body should be ("1\t")
    }

    get("/blah/required/blah?req=1&stuff=foobar") { status should be (400)}


    get("/blah/required/blah?req=1&stuff=2014-03-22") {
      status should be (200)
      body should be ("1\t2014-03-22T00:00:00.000Z")
    }

    get("/blah/required/blah?stuff=2014-03-22&req=1&stuff=2012-08-07") {
      status should be (200)
      body should be ("1\t2014-03-22T00:00:00.000Z,2012-08-07T00:00:00.000Z")
    }
  }


  test("swagger summary") {
    get("/api-docs") {
      val bd = JsonParser.parseOpt(body)
      bd.value shouldBe a [JObject]
      val j = bd.get
      j \ "apiVersion" should be (JString("1.0.0"))
      j \ "swaggerVersion" should be (JString("1.2"))
      val apis = (j \ "apis").asJArray
      apis.arr.length should be (2)
      val paths = apis.arr.map{a =>
        a \ "description" should be (JString("test api description"))
        (a \ "path").values
      }.toSet should be (Set("/blah", "/wakka"))
    }
  }

  test("swagger resource") {
    get("/api-docs/blah") {
      val bd = JsonParser.parseOpt(body)
      bd.value shouldBe a [JValue]
      val j = bd.get
      val apis = (j \ "apis").asJArray
      apis.arr.length should be (2)
      apis.arr.foreach{api =>
        val path = (api \ "path").asString
        val parameters = (api \ "operations" \ "parameters").asJArray
        parameters.arr.length should be (1)
        verifyParams(path, parameters.arr(0).asJArray)
      }
    }
  }

  def verifyParams(path: String, parameters: JArray) {
    val names = parameters.arr.map{v => (v \ "name").asString}.toSet
    path match {
      case "/blah/foo/{bar}" =>
        names should be (Set("bar", "count"))
      case "/blah/required/{bar}" =>
        names should be (Set("bar", "count", "req", "stuff", "options", "multi", "jEnum", "zimzam"))
    }
    parameters.arr.foreach{x => verifyParam(x.asJObject)}
  }

  def verifyParam(param: JObject) {
    val name = (param \ "name").asString
    withClue("for param \"" + param + "\":") {
      val required = (param \ "required").asBool
      name match {
        case "req"|"bar" =>
          required should be(true)
        case _ =>
          required should be(false)
      }
      val enums = (param \ "enum")
      name match {
        case "options" =>
          enums.asJArray.arr.map{_.asString} should be (List("a", "b", "c"))
        case "jEnum" =>
          enums.asJArray.arr.map{_.asString} should be (List(MyArgEnum.values: _*).map{_.name})
        case "multi" =>
          enums.asJArray.arr.map{_.asString} should be (List("d", "e", "f"))
        case _ =>
          enums should be (JNothing)
      }
      val desc = (param \ "description").asString
      name match {
        case "zimzam" => desc should be ("my favorite arg")
        case _ => desc should be (name)
      }

      val typ = (param \ "type").asString
      val format = (param \ "format")
      val items = (param \ "items")
      name match {
        case "count"|"zimzam"|"req" =>
          typ should be ("integer")
          format.asString should be ("int32")
          items should be (JNothing)
        case "stuff" =>
          typ should be ("array")
          (items \ "type").asString should be ("string")
          (items \ "format").asString should be ("date-time")
        case "multi" =>
          typ should be ("array")
          (items \ "type").asString should be ("string")
        case _ =>
          typ should be ("string")
          format should be (JNothing)
          items should be (JNothing)
      }
    }
  }

  implicit class UnsafeJson(j: JValue) {
    def asString: String = j.asInstanceOf[JString].s
    def asJArray: JArray = j.asInstanceOf[JArray]
    def asJObject: JObject = j.asInstanceOf[JObject]
    def asBool: Boolean = j.asInstanceOf[JBool].value
  }

}


abstract class SumacSupportServlet(name: String)(implicit val swagger: Swagger) extends ScalatraServlet with SumacSupport {

  override def applicationDescription = "test api description"
  override def applicationName = Some(name)
  protected implicit val jsonFormats: Formats = DefaultFormats

  getSimpleSwaggered[SomeArgs]("/foo/:bar") { args =>
    Ok((0 until args.count).map{x => args.bar}.toSeq.mkString(","))
  }

  getSimpleSwaggered[ComplexArgs]("/required/:bar") { args =>
    Ok(args.req + "\t" + args.stuff.mkString(","))
  }
}

//this weirdness is b/c scalatra won't generate swagger for 2 different instances of same class
class BlahServlet(_swagger: Swagger) extends SumacSupportServlet("blah")(_swagger)
class WakkaServlet(_swagger: Swagger) extends SumacSupportServlet("wakka")(_swagger)

class SwaggerResourcesServlet(val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase
