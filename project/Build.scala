import sbt._
import sbt.Keys._

import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleaseStep
import sbtrelease.Utilities._
import sbtrelease.Vcs

import annotation.tailrec


object SumacBuild extends Build {
  
  lazy val core = Project("core", file("core"), settings = coreSettings).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
  lazy val ext = Project("ext", file("ext"), settings = extSettings).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*) dependsOn(core)
  lazy val extZk = Project("ext-zk", file("ext-zk"), settings = extZkSettings).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*) dependsOn(core)

  def sharedSettings = Defaults.defaultSettings ++ ReleasePlugin.releaseSettings ++ BranchRelease.branchSettings ++ Seq(
    // version is managed by sbt-release in version.sbt
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

    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      BranchRelease.makeBranch,           //make a new rel/$version branch
      commitReleaseVersion,               // all changes happen here
      publishArtifacts,
      BranchRelease.pushBranch,
      BranchRelease.moveToPreviousBranch,
      setNextVersion,                     // bump to the next snapshot version
      commitNextVersion,
      tagRelease,                         //we tag where the next release starts from
      pushChanges
    ),

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
        <developer>
        <id>pierre</id>
        <name>Pierre Andrews</name>
        <url>http://github.com/Mortimerp9</url>
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


object BranchRelease {

  import BranchReleaseKeys._

  //> copy from sbtrelease some usefule private methods
  private def vcs(st: State): Vcs = {
    st.extract
      .get(versionControlSystem)
      .getOrElse(sys.error("Aborting release. Working directory is not a"+
        "repository of a recognized VCS."))
  }

  private lazy val initialVcsChecks = { st: State =>
    val status = (vcs(st).status !!).trim
    if (status.nonEmpty) {
      sys.error("Aborting release. Working directory is dirty.")
    }
    st
  }

  private lazy val checkUpstream = { st: State =>
    if (!vcs(st).hasUpstream) {
      sys.error("No tracking branch is set up. Either configure a remote tracking branch, or remove the pushChanges release part.")
    }

    st.log.info("Checking remote [%s] ..." format vcs(st).trackingRemote)
    if (vcs(st).checkRemote(vcs(st).trackingRemote) ! st.log != 0) {
      SimpleReader.readLine("Error while checking remote. Still continue (y/n)? [n] ") match {
        case Yes() => // do nothing
        case _ => sys.error("Aborting the release!")
      }
    }

    if (vcs(st).isBehindRemote) {
      SimpleReader.readLine("The upstream branch has unmerged commits. A subsequent push will fail! Continue (y/n)? [n] ") match {
        case Yes() => // do nothing
        case _ => sys.error("Merge the upstream commits and run `release` again.")
      }
    }
    st
  }
  //<copy from sbtrelease

  //////////////////////////////////////////////////////////////////////
  // a release step that branches from the current branch
  //////////////////////////////////////////////////////////////////////
  lazy val makeBranch = ReleaseStep(makeBranchAction, initialVcsChecks)

  private lazy val makeBranchAction = { st: State =>
    val vc = vcs(st)

    //store the current branch so we can come back here
    val nst = st.put(previousBranchKey, vc.currentBranch)

    //make sure that the new branch doesn't exist yet
    @tailrec
    def testBranch(branch: String): String = {
      def localBranchExists = {
       nst.log.info(s"checking if local $branch already exists")
        (vc.cmd("branch") !!).linesIterator.exists {
          _.endsWith(s" $branch")
        }
      }
      def remoteBranchExists = {
        nst.log.info(s"checking if remote $branch already exists")
        (vc.cmd("ls-remote", "--heads") !!).linesIterator.exists {
          _.endsWith(s"refs/heads/$branch")
        }
      }

      //if the branch is already defined, get a new name
      if(localBranchExists || remoteBranchExists) {
        SimpleReader
          .readLine(s"Branch [$branch] already exists, [a]bort or specify a new name: ") match {
          case Some("" | "a" | "A") =>
            sys.error(s"Branch [$branch] already exists. Aborting release!")
          case Some(newBranch) =>
            //test the entered name
            testBranch(newBranch)
          case None =>
            sys.error(s"Branch [$branch] already exists. Aborting release!")
        }
      } else branch
    }

    //get the branch name from config
    val (branchState, branch) = nst.extract.runTask(branchName, nst)
    val branchToUse = testBranch(branch)
    st.log.info("git branching sends its console output to standard error,"+
      "which will cause the next few lines to be marked as [error].")
    vc.cmd("checkout", "-b", branchToUse) !! branchState.log

    //store the new branch to push it later
    branchState.put(branchKey, branchToUse)
  }

  //////////////////////////////////////////////////////////////////////
  //  push the release branch to origin TODO config the remote
  //////////////////////////////////////////////////////////////////////
  lazy val pushBranch = ReleaseStep(pushBranchAction, checkUpstream)
  private lazy val pushBranchAction = { st: State =>
    val vc = vcs(st)

    val b = st.get(branchKey)
      .getOrElse(sys.error("no branch set, you have to run this step after makeBranch"))
    vc.cmd("push", "-u", "origin", b) !! st.log

    st
  }

  lazy val moveToPreviousBranch: ReleaseStep = { st: State =>
    val vc = vcs(st)

    val ba = st.get(previousBranchKey)
      .getOrElse(sys.error("no branch set, you have to run this step after makeBranch"))
    vc.cmd("checkout", ba) !! st.log

    st
  }

  lazy val branchSettings = Seq[Setting[_]](
    branchName <<= (version in ThisBuild) map ( v => s"rel/$v" )
  )

}

object BranchReleaseKeys {
  //the name of the rel branch
  lazy val branchName = TaskKey[String]("release-branch-name")
  //used internaly to keep state
  lazy val branchKey = AttributeKey[String]("release-branch")
  lazy val previousBranchKey = AttributeKey[String]("current-branch")
}
