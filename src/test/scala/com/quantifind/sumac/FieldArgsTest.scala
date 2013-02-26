package com.quantifind.sumac

import types.{SelectInput,MultiSelectInput}
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.lang.reflect.Type

/**
 *
 */

class FieldArgsTest extends FunSuite with ShouldMatchers {

  test("parseStrings") {
    val o = new StringHolder(null, null) with FieldArgs
    o.parse(Array("--name", "hello"))
    o.name should be ("hello")
    o.parse(Array("--comment", "blah di blah blah"))
    o.name should be ("hello")
    o.comment should be ("blah di blah blah")
    o.parse(Array("--name", "ooga", "--comment", "stuff"))
    o.name should be ("ooga")
    o.comment should be ("stuff")
  }

  test("parseMixed") {
    val o = new MixedTypes(null, 0) with FieldArgs

    o.parse(Array("--name", "foo", "--count", "17"))
    o.name should be ("foo")
    o.count should be (17)
    o.parse(Array("--count", "-5"))
    o.name should be ("foo")
    o.count should be (-5)
  }

  test("subclass parsing") {
    val o = new Child(false, null, 0) with FieldArgs

    o.parse(Array("--flag", "true", "--name", "bugaloo"))
    o.name should be ("bugaloo")
    o.flag should be (true)
  }

  test("help message") {
    val o = new StringHolder(null, null) with FieldArgs
    val exc1 = evaluating {o.parse(Array("--xyz", "hello"))} should produce [ArgException]
    //the format is still ugly, but at least there is some info there
    "\\-\\-name\\s.*String".r.findFirstIn(exc1.getMessage()) should be ('defined)
    "\\-\\-comment\\s.*String".r.findFirstIn(exc1.getMessage()) should be ('defined)

    val o2 = new MixedTypes(null, 0) with FieldArgs
    val exc2 = evaluating {o2.parse(Array("--foo", "bar"))} should produce [ArgException]
    "\\-\\-name\\s.*String".r findFirstIn(exc2.getMessage) should be ('defined)
    "\\-\\-count\\s.*[Ii]nt".r findFirstIn(exc2.getMessage) should be ('defined)  //java or scala types, I'll take either for now

    val exc3 = evaluating {o2.parse(Array("--count", "ooga"))} should produce [ArgException]
    //this message really should be much better.  (a) the number format exception should come first and (b) should indicate that it was while processing the "count" argument
    "\\-\\-name\\s.*String".r findFirstIn(exc3.getMessage) should be ('defined)
    "\\-\\-count\\s.*[Ii]nt".r findFirstIn(exc3.getMessage) should be ('defined)  //java or scala types, I'll take either for now
  }

  test("error msg on unknown types") {
    val o = new SpecialTypes("", null) with FieldArgs

    o.parse(Array("--name", "ooga"))
    o.name should be ("ooga")
    o.funky should be (null)

    val exc = evaluating {o.parse(Array("--funky", "xyz"))} should produce [ArgException]
    //maybe sometime I should change the removal of unknown types to keep them around for error msgs ...
//    exc.cause.getMessage should include ("type")
//    exc.cause.getMessage should include ("MyFunkyType")
  }


  test("good error msg") {
    val o = new MixedTypes("", 0) with FieldArgs

    val exc1 = evaluating {o.parse(Array("--count", "hi"))} should produce [ArgException]
    //don't actually need the message to look *exactly* like this, but extremely useful for it to at least say what it was trying to parse
    exc1.getMessage should startWith ("""Error parsing "hi" into field "count" (type = int)""")
  }

  test("set args") {
    case class SetArgs(val set: Set[String]) extends FieldArgs
    val s = new SetArgs(null)
    s.parse(Array("--set", "a,b,c,def"))
    s.set should be (Set("a", "b", "c", "def"))
  }

  test("selectInput") {
    case class SelectInputArgs(val select: SelectInput[String] = SelectInput("a", "b", "c")) extends FieldArgs
    val s = new SelectInputArgs()
    val id = System.identityHashCode(s.select)
    s.parse(Array("--select", "b"))
    s.select.value should be (Some("b"))
    System.identityHashCode(s.select) should be (id)
    s.select.options should be (Set("a", "b", "c"))

    evaluating {s.parse(Array("--select", "q"))} should produce [ArgException]
  }

  test("selectInput order") {
    import util.Random._
    val max = 1000
    val orderedChoices = shuffle(1.to(max).map(_.toString))
    case class SelectInputArgs(val select: SelectInput[String] = SelectInput(orderedChoices:_*)) extends FieldArgs
    val s = new SelectInputArgs()
    val id = System.identityHashCode(s.select)
    
    val index = nextInt(max).toString
    s.parse(Array("--select", index))
    s.select.value should be (Some(index))
    System.identityHashCode(s.select) should be (id)
    s.select.options.toList should be (orderedChoices)

    evaluating {s.parse(Array("--select", "q"))} should produce [ArgException]
  }

  test("multiSelectInput") {
    case class MultiSelectInputArgs(val multiSelect: MultiSelectInput[String] = MultiSelectInput("a", "b", "c")) extends FieldArgs
    val s = new MultiSelectInputArgs()
    val id = System.identityHashCode(s.multiSelect)
    s.parse(Array("--multiSelect", "b"))
    s.multiSelect.value should be (Set("b"))
    System.identityHashCode(s.multiSelect) should be (id)
    s.multiSelect.options should be (Set("a", "b", "c"))

    s.parse(Array("--multiSelect", "b,c"))
    s.multiSelect.value should be (Set("b", "c"))

    evaluating {s.parse(Array("--multiSelect", "q"))} should produce [ArgException]
    evaluating {s.parse(Array("--multiSelect", "b,q"))} should produce [ArgException]
    evaluating {s.parse(Array("--multiSelect", "q,b"))} should produce [ArgException]

  }

  test("exclude scala helper fields") {

    {
      val m = new MixedTypes(null, 0) with FieldArgs
      val names = m.parser.nameToHolder.keySet
      names should be (Set("name", "count"))
    }


    {
      val s = new SomeApp()
      val names = s.getArgHolder.parser.nameToHolder.keySet
      names should be (Set("x", "y"))
    }

  }



  test("annotations") {
    val c = new ClassWithSomeAnnotations() with FieldArgs
    c.parser.nameToHolder.values.foreach { f =>
      f.getName match {
        case "foo" =>
          f.getDescription should be ("foo")
        case "ooga" =>
          f.getDescription should be ("this is an integer argument")
        case "" =>
          assert(false, "use variable name if no name given in annotation")
        case "x" => assert(false, "use name from annotation instead of variable name")
        case "y" =>
          f.getDescription should be ("another integer argument")
        case "z" =>
          assert(false, "use name from annotation instead of variable name")
        case "wakka" =>
          f.getDescription should be ("wakka")
      }
    }

    c.parse(Array("--foo", "hi", "--ooga", "17", "--y", "181", "--wakka", "1.81"))
    c.foo should be ("hi")
    c.x should be (17)
    c.y should be (181)
    c.z should be (1.81)

    evaluating {c.parse(Array("--x", "17"))} should produce [ArgException]
    evaluating {c.parse(Array("--z", "1"))} should produce [ArgException]
  }


  test("custom parsers") {
    val c = new ArgsWithCustomType()
    c.parse(Array("--x", "7", "--y", "hithere:345","--z", "oogabooga"))
    c.x should be (7)
    c.y should be (CustomType("hithere", 345))
    c.z should be ("oogabooga")
  }


}


case class StringHolder(val name: String, val comment: String)

case class MixedTypes(val name: String, val count: Int)

//is there an easier way to do this in scala?
class Child(val flag: Boolean, name: String, count: Int) extends MixedTypes(name, count)

case class SpecialTypes(val name: String, val funky: MyFunkyType)

case class MyFunkyType(val x: String)


class SomeApp extends ArgApp[SomeArgs] {
  def main(args: SomeArgs) {}
}

class SomeArgs extends FieldArgs {
  var x: Int = 0
  var y: String = "hello"
}



class ClassWithSomeAnnotations {
  var foo: String = _
  @Arg(name="ooga", description="this is an integer argument")
  var x: Int = _
  @Arg(description="another integer argument")
  var y: Int = _
  @Arg(name="wakka")
  var z: Double = _
}

case class CustomType(val name: String, val x: Int)

object CustomTypeParser extends Parser[CustomType] {
  def canParse(tpe:Type) = {
    ParseHelper.checkType(tpe, classOf[CustomType])
  }
  def parse(s: String, tpe: Type, currentVal: AnyRef) = {
    val parts = s.split(":")
    CustomType(parts(0), parts(1).toInt)
  }
}

class ArgsWithCustomType extends FieldArgs {
  registerParser(CustomTypeParser)
  var x: Int = _
  var y: CustomType = _
  var z: String = _
}
