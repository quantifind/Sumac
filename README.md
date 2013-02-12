[![Build Status](https://secure.travis-ci.org/squito/optional.png?branch=master)](http://travis-ci.org/squito/optional)

optional is a command line option parser and library.  It tries
to differentiate itself from other libraries by making it dead
simple to define arguments, removing boilerplate and repitition.  It
is a very small, lightweight scala library.

## Usage

Define a basic container object which extends FieldParsing.  Every field of the object
becomes a command line argument with the same name.  You can use the parse method to parse
arguments.

    class Arguments with FieldParsing {
      var name: String = _
      var count: Int = _
    }

    object MyApp {
      def main(args: Array[String]) {
        val myArgs = new Arguments()
        myArgs.parse(args)
        ...
      }
    }

Now MyApp has two arguments, "name" and "count".  You can run it like:

    java -cp <path>/<to>/<compiled>/<code> MyApp --name foobar --count 17


### ArgApp and ArgMain

You don't even have to call parse() yourself.  The arguments are automatically parsed for you
if you extend ArgApp (for the scala "App" style way of creating a main method) or if you extend
ArgMain (for the main-method version, my personal perference).

With ArgApp:

    class Arguments with FieldParsing {
      var name: String = _
      var count: Int = _
    }

    object MyApp extends ArgApp[Arguments]{
      //this.argHolder is an Arguments object w/ the args already parsed
      println(this.argHolder.name)
    }

or with ArgMain:

    class Arguments with FieldParsing {
      var name: String = _
      var count: Int = _
    }

    object MyApp extends ArgMain[Arguments]{
       def main(args: Arguments) {
         //the cmd line arguments get parsed, and then passed into this function
         println(args.name)
       }
    }

you could then run these programs with

    java -cp <path>/<to>/<compiled>/<code> MyApp --name foobar --count 17

### Mixing In Multiple Traits

You can use traits to create "sets" of arguments that tend to go together.  Because you can mix in multiple traits into
one argument object, this lets you put together the arguments that want, without duplicating argument definitions.

For example, lets say that you have some set of arguments for a database connection, another set of arguments for a
screen resolution, and another set of arguments for the username.  You can define traits for each of these groups:

    trait DBConnectionArgs extends FieldParsing {
      var dbHost : String = _
      var dbPort : Int = 4000
      def getDbConnection = { ... }
    }

    trait ScreenResolutionArgs extends FieldParsing {
      var width: Int = 800
      var height: Int = 600
    }

    trait UsernameArgs extends FieldParsing {
      var username: String = _
    }

Then one application that needs a database connection and a user name could be written as:

    class AppNumberOneArgs extends DBConnectionArgs with UsernameArgs
    object AppNumberOne extends ArgMain[AppNumberOneArgs]{
      def main(args: AppNumberOneArgs) = {
        val db = args.getDbConnection()
        ...
      }
    }

And another application that needs a database connection and screen resolution:

    class AppNumeroDosArgs extends DBConnectionArgs with ScreenResolutionArgs
    object AppNumeroDos extends ArgMain[AppNumeroDosArgs]{
      def main(args: AppNumeroDosArgs) = {
        val db = args.getDbConnection()
        ...
      }
    }

Note that you are sharing the argument names and types, AND the definition of helper methods like getDbConnection()

## Status / TODO / Roadmap

I use this library heavily, so I think it is safe for others to use as well.  Of course, that just means I'm used to its
quirks :)  There are still a lot of things I'd like to add (and would love contributions from anyone!)

* Support for primitives as a type parameter (eg., List[Int] doesn't work now)
* Add support for user-defined parsers.  Not sure about the syntax here
* Automatically support types with an apply(String) method.  (hint: http://stackoverflow.com/questions/9172775/get-companion-object-of-class-by-given-generic-type-scala)
* Add support for user-defined validation.  again, not sure about the syntax.
* Nested objects, args named via "."
* turn scaladoc into description.  may require compiler plugin
* export to properties and typesafe config object

## Credits

This was inspired by the optional package from alexy, which in turn came from:

>Idea and prototype implementation: DRMacIver.

>Fleshing out and awesomification: paulp.

This is a total rewrite, though.
