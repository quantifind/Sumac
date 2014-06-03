**Sumac** is a command line option parser and library.  It tries
to differentiate itself from other libraries by making it dead
simple to define arguments, removing boilerplate and repetition.  It
is a very small, lightweight scala library.

[![Build Status](https://travis-ci.org/quantifind/Sumac.svg?branch=master)](https://travis-ci.org/quantifind/Sumac)
[![Coverage Status](https://coveralls.io/repos/quantifind/Sumac/badge.png?branch=master)](https://coveralls.io/r/quantifind/Sumac?branch=master)

![Sumac Logo](https://raw.github.com/quantifind/Sumac/master/logo/Sumac.png)

It is available on maven central.  The last stable release is `0.3.0`.  An sbt dependency would look like:

    "com.quantifind" %% "sumac" % "0.3.0"

Integration with 3rd party libraries (in particular, [Joda-Time](http://www.joda.org/joda-time/) and [Typesafe Config](https://github.com/typesafehub/config))
is available through the `sumac-ext` package:

    "com.quantifind" %% "sumac-ext" % "0.3.0"

## Usage

Full usage can be found on the[wiki](https://github.com/quantifind/Sumac/wiki), but we can go over the basics quickly.
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

## More Info

Lots more details about using Sumac can be found on the [wiki](https://github.com/quantifind/Sumac/wiki).  

Sumac is open source, and we hope to get involvement from the community.  We'd love to get some pull requests.  Also, even if you don't
have a fix, feel free to report bugs or just request new features through the [github issue tracker](https://github.com/quantifind/Sumac/issues?state=open).

## Credits

This was inspired by the optional package from alexy, which in turn came from:

>Idea and prototype implementation: DRMacIver.

>Fleshing out and awesomification: paulp.

This is a total rewrite, though.
