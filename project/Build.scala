import sbt._
import Keys._

object SumacBuild extends Build {
  lazy val core = Project("core", file("core"), settings = coreSettings)
  lazy val zk = Project("zk", file("zk"), settings = zkSettings) dependsOn(core)

  def sharedSettings = Defaults.defaultSettings ++ Seq(
    version := "0.2-SNAPSHOT",
    scalaVersion := "2.9.3",
    organization := "com.quantifind",
    scalacOptions := Seq(/*"-deprecation",*/ "-unchecked", "-optimize"), // -deprecation is too noisy due to usage of old Hadoop API, enable it once that's no longer an issue
    unmanagedJars in Compile <<= baseDirectory map { base => (base / "lib" ** "*.jar").classpath },
    retrieveManaged := true,
    transitiveClassifiers in Scope.GlobalScope := Seq("sources"),
    publishTo <<= baseDirectory { base => Some(Resolver.file("Local", base / "target" / "maven" asFile)(Patterns(true, Resolver.mavenStyleBasePattern))) },
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      "log4j" % "log4j" % "1.2.16",
      "org.slf4j" % "slf4j-api" % slf4jVersion,
      "org.slf4j" % "slf4j-log4j12" % slf4jVersion
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
  
  def zkSettings = sharedSettings ++ Seq(
    name := "Sumac-zk",
    resolvers ++= Seq(
      "Twitter Repo" at "http://maven.twttr.com/"
    ),
    libraryDependencies ++= Seq(
      "com.twitter"   % "util-zk"   % "5.3.10"
    )
  )

}
