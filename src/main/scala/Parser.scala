package optional

import java.lang.reflect.{Type, ParameterizedType}

trait Parser[T] {
  def parse(s: String, tpe: Type): T

  /**
   * return true if this parser knows how to parse the given type
   * @param tpe
   * @return
   */
  def canParse(tpe: Type): Boolean
}

trait SimpleParser[T] extends Parser[T] {
  def getKnownTypes() : Set[Class[_]]
  def canParse(tpe: Type) = {
    if (tpe.isInstanceOf[Class[_]])
      getKnownTypes()(tpe.asInstanceOf[Class[_]])
    else
      false
  }
  def parse(s: String, tpe:Type) = parse(s)
  def parse(s:String) :T
}

trait CompoundParser[T] extends Parser[T]


object StringParser extends SimpleParser[String] {
  val knownTypes : Set[Class[_]] = Set(classOf[String])
  def getKnownTypes() = knownTypes
  def parse(s:String) = s
}

object IntParser extends SimpleParser[Int] {
  val knownTypes : Set[Class[_]] = Set(classOf[Int], classOf[java.lang.Integer])
  def getKnownTypes() = knownTypes
  def parse(s: String) = s.toInt
}

object DoubleParser extends SimpleParser[Double] {
  val knownTypes : Set[Class[_]] = Set(classOf[Double], classOf[java.lang.Double])
  def getKnownTypes() = knownTypes
  def parse(s: String) = s.toDouble
}

object ListParser extends CompoundParser[List[_]] {

  def canParse(tpe: Type) = {
    val clz = if (tpe.isInstanceOf[Class[_]])
      tpe.asInstanceOf[Class[_]]
    else if (tpe.isInstanceOf[ParameterizedType])
      tpe.asInstanceOf[ParameterizedType].getRawType.asInstanceOf[Class[_]]
    else
      classOf[Int]  //just need something that won't match
    classOf[List[_]].isAssignableFrom(clz)
  }

  def parse(s: String, tpe: Type) = {
    if (tpe.isInstanceOf[ParameterizedType]) {
      val ptpe = tpe.asInstanceOf[ParameterizedType]
      val subtype = ptpe.getActualTypeArguments()(0)
      val subParser = ParseHelper.findParser(subtype).get //TODO need to handle cases where its a list, but can't parse subtype
      val parts = s.split(",")
      parts.map{sub => subParser.parse(sub, subtype)}.toList
    }
    else
      List()
  }

}


object ParseHelper {
  val parsers = Seq(StringParser, IntParser, DoubleParser, ListParser)

  def findParser(tpe: Type) : Option[Parser[_]] = {
    for (p <- parsers) {
      if (p.canParse(tpe))
        return Some(p)
    }
    None
  }

  def parseInto[T](s: String, tpe: Type) : Option[ValueHolder[T]] = {
    //could change this to be a map, at least for the simple types
    findParser(tpe).map{parser => ValueHolder[T](parser.parse(s, tpe).asInstanceOf[T], tpe)}
  }
}

case class ValueHolder[T](value: T, tpe: Type)