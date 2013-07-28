**Sumac** is a command line option parser and library.  It tries
to differentiate itself from other libraries by making it dead
simple to define arguments, removing boilerplate and repetition.  It
is a very small, lightweight scala library.

## Usage

Define a basic container object which extends `FieldArgs`.  Every field of the object
becomes a command line argument with the same name.  Then use `parse()` to process the command line arguments.

    import com.quantifind.sumac.FieldArgs

    class Arguments extends FieldArgs {
      var name: Option[String] = None
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


### ArgMain

You don't even have to call parse() yourself.  The arguments are automatically parsed for you
if you extend `ArgMain`

    import com.quantifind.sumac.{ArgMain, FieldArgs}

    class Arguments extends FieldArgs {
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

    import com.quantifind.sumac.FieldArgs

    trait DBConnectionArgs extends FieldArgs {
      var dbHost : String = _
      var dbPort : Int = 4000
      def getDbConnection = { ... }
    }

    trait ScreenResolutionArgs extends FieldArgs {
      var width: Int = 800
      var height: Int = 600
    }

    trait UsernameArgs extends FieldArgs {
      var username: String = _
    }

Then one application that needs a database connection and a user name could be written as:

    import com.quantifind.sumac.{ArgMain, FieldArgs}

    class AppNumberOneArgs extends DBConnectionArgs with UsernameArgs
    object AppNumberOne extends ArgMain[AppNumberOneArgs]{
      def main(args: AppNumberOneArgs) = {
        val db = args.getDbConnection()
        ...
      }
    }

And another application that needs a database connection and screen resolution:

    import com.quantifind.sumac.{ArgMain, FieldArgs}

    class AppNumeroDosArgs extends DBConnectionArgs with ScreenResolutionArgs
    object AppNumeroDos extends ArgMain[AppNumeroDosArgs]{
      def main(args: AppNumeroDosArgs) = {
        val db = args.getDbConnection()
        ...
      }
    }

Note that you are sharing the argument names and types, AND the definition of helper methods like `getDbConnection()`

## Builtin Help

Every set of arguments get support for "--help" added automatically.  If that appears anywhere in the list of arguments
you give when calling `FieldArgs.parse`, then you'll get a help message listing all options and their types

    bash-3.2$ java -cp core/target/scala-2.9.3/classes/:$SCALA_HOME/lib/scala-library.jar com.quantifind.sumac.examples.SimpleApp --count 1 --help
    Exception in thread "main" com.quantifind.sumac.ArgException: usage:
    --name  class java.lang.String  name

    --count int     count

## Validation

Every argument holder can ensure it received valid arguments via custom validation rules.

    import com.quantifind.sumac.{FieldArgs, ArgException}

    trait MyArgs extends FieldsArgs {
      var count: Int = 1
      addValidation{ if (count < 0) throw new ArgException("count must be >= 0")}
    }

## Status / TODO / Roadmap

We use this library heavily in production sytems.  Of course, that just means we're used to its
quirks :)  There are still a lot of things we'd like to add (and would love contributions from anyone!)

* Support for primitives as a type parameter (eg., List[Int] doesn't work now)
* Automatically support types with an apply(String) method.  (hint: http://stackoverflow.com/questions/9172775/get-companion-object-of-class-by-given-generic-type-scala)
* Nested objects, args named via "."
* turn scaladoc into description.  may require compiler plugin
* export to properties and typesafe config object. (This is already supported in master, but not thoroughly
    tested and the api may change.)

## Credits

This was inspired by the optional package from alexy, which in turn came from:

>Idea and prototype implementation: DRMacIver.

>Fleshing out and awesomification: paulp.

This is a total rewrite, though.
