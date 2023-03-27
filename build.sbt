import sbt._
import sbt.Keys._

// We need to explicitly define the root project, so we can apply the settings for cross-compiling
lazy val root = (project in file("."))
  .aggregate(core, ext)
  .settings(sharedSettings)
lazy val core = Project(id = "core", base = file("core")).settings(coreSettings)
lazy val ext = Project(id = "ext", base = file("ext")).settings(extSettings).dependsOn(core)

def sharedSettings =
  Seq(
    // version is managed by sbt-release in version.sbt
    scalaVersion := "2.13.10",
    organization := "com.quantifind",
    scalacOptions := Seq("-deprecation", "-unchecked", "-optimize"),
    retrieveManaged := true,
    transitiveClassifiers in Scope.GlobalScope := Seq("sources"),
    resolvers ++= Seq(
      "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "sonatype-releases"  at "https://oss.sonatype.org/content/repositories/releases",
      "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
      "JBoss Repository" at "https://repository.jboss.org/nexus/content/repositories/releases/"
    ),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.5" % "test"
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      // add scala-parser-combinators dependency when needed (for Scala 2.11 and newer) in a robust way
      // this mechanism supports cross-version publishing
      // taken from: http://github.com/scala/scala-module-dependency-sample
      // if scala 2.11+ is used, add dependency on scala-parser-combinators module
      case Some((2, scalaMajor)) if scalaMajor >= 11 =>
        Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "2.2.0")
      case _ =>
        Nil //it's in 2.10 core
    }),

    crossScalaVersions := Seq("2.11.0", "2.12.8", "2.13.10"),

    publishMavenStyle := true,

    Global / useGpg := false,

    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },

    Test / publishArtifact := false,
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
          <developer>
            <id>pierre</id>
            <name>Pierre Andrews</name>
            <url>http://github.com/Mortimerp9</url>
          </developer>
        </developers>),
    javacOptions ++= Seq("-target", "1.6", "-source", "1.6")
  )

val slf4jVersion = "1.7.12"

def coreSettings = sharedSettings ++ Seq(
  name := "Sumac"
)

def extSettings = sharedSettings ++ Seq(
  name := "Sumac-ext",
  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.0.2",
    "joda-time" % "joda-time" % "2.3",
    "org.joda" % "joda-convert" % "1.6"  //this is needed for joda to work w/ scala
  )
)
