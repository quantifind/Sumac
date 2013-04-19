package com.quantifind.sumac

import org.scalatest.FunSuite
import java.io.{PrintWriter, File}
import java.util.Properties
import org.scalatest.matchers.ShouldMatchers

class PropertiesConfigTest extends FunSuite with ShouldMatchers {

  val testOutDir = new File("test_output/" + getClass.getSimpleName)
  testOutDir.mkdirs()

  test("load properties") {
    val propFile = new File(testOutDir, "load_properties_test.properties")
    val p = new Properties()
    p.put("x", "98")
    p.put("blah", "ooga booga")
    val out = new PrintWriter(propFile)
    p.store(out,null)
    out.close()


    val args = new PropertyArgs()
    args.parse(Array("--propertyFile", propFile.getAbsolutePath))
    args.x should be (98)
    args.blah should be ("ooga booga")
  }

  test("roundtrip properties") {
    pending
  }


  class PropertyArgs extends FieldArgs with PropertiesConfig {
    var x: Int = _
    var blah: String = _
    var wakka: Float = _
  }

}


