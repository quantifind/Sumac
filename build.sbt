
val scalatraVersion = "2.4.1"
val json4sVersion = "3.4.0"

lazy val commonSettings = Seq(
  // version is managed by sbt-release in version.sbt
  version := "0.3.1-qphi",
  scalaVersion := "2.10.6",
  crossScalaVersions := Seq("2.10.6", "2.11.8"),
  organization := "com.quantifind",
  scalacOptions := Seq("-deprecation", "-unchecked", "-optimize"),
  unmanagedJars in Compile <<= baseDirectory map { base => (base / "lib" ** "*.jar").classpath },
  retrieveManaged := true,
  transitiveClassifiers in Scope.GlobalScope := Seq("sources"),
  publishTo := Some("Artifactory Realm" at
    "http://c24-mtv-02-38.dev.quantifind.com:8081/artifactory/qtdn"),
  credentials += Credentials("Artifactory Realm",
    "c24-mtv-02-38.dev.quantifind.com", "qf", "APCLFTmSwbpC9gDT"),
  resolvers ++= Seq(
    "sonatype-snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
    "sonatype-releases"  at "http://oss.sonatype.org/content/repositories/releases",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    "JBoss Repository" at "http://repository.jboss.org/nexus/content/repositories/releases/"
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.1.3" % "test"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    // add scala-parser-combinators dependency when needed (for Scala 2.11 and newer) in a robust way
    // this mechanism supports cross-version publishing
    // taken from: http://github.com/scala/scala-module-dependency-sample
    // if scala 2.11+ is used, add dependency on scala-parser-combinators module
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1")
    case _ =>
      Nil //it's in 2.10 core
  })
)


lazy val core = project.in(file("core")).
  settings(commonSettings: _*).
  settings(
    name := "sumac-core"
  )

lazy val ext = project.in(file("ext")).dependsOn(core).
  settings(commonSettings: _*).
  settings(
    name := "sumac-ext",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.3.0",
      "joda-time" % "joda-time" % "2.9.4",
      "org.joda" % "joda-convert" % "1.8.1",  //this is needed for joda to work w/ scala
      //scalatra section
      "org.scalatra" %% "scalatra" % scalatraVersion,
      "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % "test",
      "org.scalatra" %% "scalatra-swagger" % scalatraVersion,
      "org.json4s"   %% "json4s-jackson" % json4sVersion,
      "org.json4s"   %% "json4s-native" % json4sVersion,
      "org.eclipse.jetty" % "jetty-webapp" % "9.3.12.v20160915",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
      //end scalatra section
    )      
  )

lazy val extZk = project.in(file("ext-zk")).dependsOn(core).
  settings(commonSettings: _*).
  settings(
    name := "sumac-ext-zk",
    resolvers ++= Seq(
      "Twitter Repo" at "http://maven.twttr.com/"
    ),
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-zk" % "6.34.0"
    )
  )

lazy val root = project.in(file(".")).
  settings(commonSettings: _*).
  aggregate(core, ext, extZk)
