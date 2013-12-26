import sbt._
import Keys._

object SumacBuild extends Build {
  lazy val core = Project("core", file("core"), settings = coreSettings)
  lazy val ext = Project("ext", file("ext"), settings = extSettings) dependsOn(core)

  val qf = "http://repo.quantifind.com/content/repositories/"
  def sharedSettings = Defaults.defaultSettings ++ Seq(
    version := "0.2.3263340-SNAPSHOT",
    scalaVersion := "2.10.3",
    organization := "com.quantifind",
    scalacOptions := Seq("-deprecation", "-unchecked", "-optimize"), 
    unmanagedJars in Compile <<= baseDirectory map { base => (base / "lib" ** "*.jar").classpath },
    retrieveManaged := true,
    transitiveClassifiers in Scope.GlobalScope := Seq("sources"),
    //publishTo <<= baseDirectory { base => Some(Resolver.file("Local", base / "target" / "maven" asFile)(Patterns(true, Resolver.mavenStyleBasePattern))) },
    publishTo <<= version {
      (v: String) =>
        Some("snapshots" at qf + "ext-snapshots")
    },
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "1.9.1" % "test"
    ),
    resolvers ++= Seq(
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "JBoss Repository" at "http://repository.jboss.org/nexus/content/repositories/releases/"
    )
  )

  val slf4jVersion = "1.6.1"

  def coreSettings = sharedSettings ++ Seq(
    name := "Sumac"
  )
  
  def extSettings = sharedSettings ++ Seq(
    name := "Sumac-ext",
    resolvers ++= Seq(
      "Twitter Repo" at "http://maven.twttr.com/"
    ),
    libraryDependencies ++= Seq(
      "com.twitter"   % "util-zk"   % "5.3.10",
      "com.typesafe" % "config" % "1.0.2",
      "joda-time" % "joda-time" % "2.3",
      "org.joda" % "joda-convert" % "1.2"  //this is needed for joda to work w/ scala
    )
  )
}
