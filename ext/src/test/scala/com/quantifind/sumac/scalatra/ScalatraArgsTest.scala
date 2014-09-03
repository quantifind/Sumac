package com.quantifind.sumac.scalatra

import com.quantifind.sumac.types.{MultiSelectInput, SelectInput}
import com.quantifind.sumac.{Arg, StandardDateTimeParse, FieldArgs}
import com.quantifind.sumac.validation.{Positive, Required}
import org.joda.time.DateTime
import org.scalatra._
import org.scalatra.test.scalatest.ScalatraFunSuite

class ScalatraArgsTest extends ScalatraFunSuite {

  addServlet(classOf[DummyServlet], "/*")


  test("any") {
    get("/blah") {
      status should be (200)
    }
  }

  test("parse args") {

    get("/foo/wakka?count=3") {
      status should be (200)
      body should be ("wakka,wakka,wakka")
    }
  }

  test("validation") {

    get("/required/blah") { status should be (500)}
    get("/required/blah?req=-5") { status should be (500)}
    get("/required/blah?req=-1") { status should be (500)}
    get("/required/blah?req=0") { status should be (500)}
    get("/required/blah?req=1") {
      status should be (200)
      body should be ("1\t")
    }

    get("/required/blah?req=1&stuff=2014-03-22") {
      status should be (200)
      body should be ("1\t2014-03-22T00:00:00.000Z")
    }

    get("/required/blah?stuff=2014-03-22&req=1&stuff=2012-08-07") {
      status should be (200)
      body should be ("1\t2014-03-22T00:00:00.000Z,2012-08-07T00:00:00.000Z")
    }
  }

}

class DummyServlet extends ScalatraServlet {

  get("/blah") {
    Ok("hi")
  }

  get("/foo/:bar") {
    val args = new SomeArgs()
    args.parse(this)
    Ok((0 until args.count).map{x => args.bar}.toSeq.mkString(","))
  }

  get("/required/:bar") {
    val args = new ComplexArgs()
    args.parse(this)
    Ok(args.req + "\t" + args.stuff.mkString(","))
  }

}

class SomeArgs extends FieldArgs with ScalatraArgs {
  var bar: String = _
  var count: Int = 0
}

class ComplexArgs extends SomeArgs {
  registerParser(StandardDateTimeParse)
  var stuff: Seq[DateTime] = Seq()
  @Required
  @Positive
  var req: Int = -1

  var options = SelectInput("a", "b", "c")
  var multi = MultiSelectInput("d", "e", "f")

  var jEnum: MyArgEnum = _

  @Arg(name="zimzam", description="my favorite arg")
  var withArgAnnotation: Int = -1
}