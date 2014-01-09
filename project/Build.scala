import sbt._
import Keys._

object SumacBuild extends Build {
  lazy val core = Project("core", file("core"), settings = coreSettings).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
  lazy val ext = Project("ext", file("ext"), settings = extSettings).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*) dependsOn(core)

  def sharedSettings = Defaults.defaultSettings ++ Seq(
    version := "0.2.3",
    scalaVersion := "2.10.3",
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
      "org.scalatest" %% "scalatest" % "1.9.1" % "test"
    ),

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
      "com.twitter"   % "util-zk_2.10"   % "6.10.0",
      "com.typesafe" % "config" % "1.0.2",
      "joda-time" % "joda-time" % "2.3",
      "org.joda" % "joda-convert" % "1.2"  //this is needed for joda to work w/ scala
    )
  )
}
