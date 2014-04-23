import sbt._
import Keys._

object SumacBuild extends Build {
  lazy val core = Project("core", file("core"), settings = coreSettings).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
  lazy val ext = Project("ext", file("ext"), settings = extSettings).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*) dependsOn(core)
  lazy val extZk = Project("ext-zk", file("ext-zk"), settings = extZkSettings).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*) dependsOn(core)

  def sharedSettings = Defaults.defaultSettings ++ Seq(
    version := "0.3.0-SNAPSHOT",
    scalaVersion := "2.11.0",
    organization := "com.quantifind",
    scalacOptions := Seq("-deprecation", "-unchecked", "-optimize"),
    unmanagedJars in Compile <<= baseDirectory map { base => (base / "lib" ** "*.jar").classpath },
    retrieveManaged := true,
    transitiveClassifiers in Scope.GlobalScope := Seq("sources"),
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
    }),

    crossScalaVersions := Seq("2.10.3", "2.11.0"),

    // Publishing configuration

    publishMavenStyle := true,

    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },

    publishArtifact in Test := false,

    pomIncludeRepository := { x => false },
    pomExtra := (
      <url>https://github.com/quantifind/Sumac</url>
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          <distribution>repo</distribution>
          <comments>A business-friendly OSS license</comments>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:quantifind/Sumac.git</url>
        <connection>scm:git:git@github.com:quantifind/Sumac.git</connection>
      </scm>
      <developers>
        <developer>
          <id>imran</id>
          <name>Imran Rashid</name>
          <url>http://github.com/squito</url>
        </developer>
        <developer>
          <id>ryan</id>
          <name>Ryan LeCompte</name>
          <url>http://github.com/ryanlecompte</url>
        </developer>
      </developers>),
      javacOptions ++= Seq("-target", "1.6", "-source", "1.6")
    ) //++ ScoverageSbtPlugin.instrumentSettings ++ CoverallsPlugin.coverallsSettings // waiting for 2.11.0 release

  val slf4jVersion = "1.6.1"

  def coreSettings = sharedSettings ++ Seq(
    name := "Sumac"
  )

  def extSettings = sharedSettings ++ Seq(
    name := "Sumac-ext",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.0.2",
      "joda-time" % "joda-time" % "2.3",
      "org.joda" % "joda-convert" % "1.2"  //this is needed for joda to work w/ scala
    )
  )
  def extZkSettings = sharedSettings ++ Seq(
    name := "Sumac-ext-zk",
    resolvers ++= Seq(
      "Twitter Repo" at "http://maven.twttr.com/"
    ),
    libraryDependencies ++= Seq(
      "com.twitter"   % "util-zk_2.10"   % "6.10.0"
    )
  )

}
