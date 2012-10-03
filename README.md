optional is a command line option parser and library.

#### Example


    case class Arguments(val name: String, val count: Int)

    object MyApp {
      def main(args: Array[String]) {
        val myArgs = new Arguments(null,0) with FieldParsing
        myArgs.parse(args)
        ...
      }
    }

See the test cases for more examples


#### Credits

This was inspired by the optional package from alexy, which in turn came from:

>Idea and prototype implementation: DRMacIver.

>Fleshing out and awesomification: paulp.

This is a total rewrite, though.