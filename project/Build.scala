import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtPgp.PgpKeys.publishSigned

import xerial.sbt.Sonatype.SonatypeKeys._

import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleaseStep
import sbtrelease.Utilities._
import sbtrelease.Vcs
import sbtrelease.releaseTask

import annotation.tailrec


object SumacBuild extends Build {
  
  lazy val core = Project("core", file("core"), settings = coreSettings).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
  lazy val ext = Project("ext", file("ext"), settings = extSettings).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*) dependsOn(core)
  lazy val extZk = Project("ext-zk", file("ext-zk"), settings = extZkSettings).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*) dependsOn(core)

  val qf = "http://repo.quantifind.com/content/repositories/"
  def sharedSettings = Defaults.defaultSettings ++ Seq(
    version := "0.3.1-d4a9a8d-SNAPSHOT",
    scalaVersion := "2.10.3",
    organization := "com.quantifind",
    scalacOptions := Seq("-deprecation", "-unchecked", "-optimize"),
    javacOptions ++= Seq("-target", "1.6", "-source", "1.6"),
    unmanagedJars in Compile <<= baseDirectory map { base => (base / "lib" ** "*.jar").classpath },
    retrieveManaged := true,
    transitiveClassifiers in Scope.GlobalScope := Seq("sources"),
    //publishTo <<= baseDirectory { base => Some(Resolver.file("Local", base / "target" / "maven" asFile)(Patterns(true, Resolver.mavenStyleBasePattern))) },
    publishTo <<= version {
      (v: String) =>
        Some("snapshots" at qf + "ext-snapshots")
    },
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.1.3" % "test"
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

  val ScalatraVersion = "2.3.0"

  def extSettings = sharedSettings ++ Seq(
    name := "Sumac-ext",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.0.2",
      "joda-time" % "joda-time" % "2.3",
      "org.joda" % "joda-convert" % "1.6",  //this is needed for joda to work w/ scala
      //scalatra section
      "org.scalatra" %% "scalatra" % ScalatraVersion,
      "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
      "org.scalatra" %% "scalatra-swagger" % ScalatraVersion,
      "org.json4s"   %% "json4s-jackson" % "3.2.10",
      "org.json4s"   %% "json4s-native" % "3.2.10",
      "org.eclipse.jetty" % "jetty-webapp" % "9.1.3.v20140225" % "provided",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
      //end scalatra section
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

  //////////////////////////////////////////////////////////////////////
  // signed publish
  //////////////////////////////////////////////////////////////////////

  lazy val publishSignedArtifacts = ReleaseStep(
    action = publishSignedArtifactsAction,
    check = st => {
      // getPublishTo fails if no publish repository is set up.
      val ex = st.extract
      val ref = ex.get(thisProjectRef)
      Classpaths.getPublishTo(ex.get(publishTo in Global in ref))
      st
    },
    enableCrossBuild = true
  )
  private lazy val publishSignedArtifactsAction = { st: State =>
    val extracted = st.extract
    val ref = extracted.get(thisProjectRef)
    extracted.runAggregated(publishSigned in Global in ref, st)
  }

}

object BranchReleaseKeys {
  //the name of the rel branch
  lazy val branchName = TaskKey[String]("release-branch-name")
  //used internaly to keep state
  lazy val branchKey = AttributeKey[String]("release-branch")
  lazy val previousBranchKey = AttributeKey[String]("current-branch")
}
