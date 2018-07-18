organization := "com.quantifind"
     
scalaVersion in ThisBuild := "2.11.8"
     
sbtVersion := "0.13.13"
 
conflictWarning in ThisBuild := ConflictWarning.disable
 
lazy val core = project
 
lazy val ext = project.dependsOn(core)

lazy val extzk = Project(id="ext-zk", base = file("ext-zk")).dependsOn(core)

