resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")

//addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.98.4")

//addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "0.98.4")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.3")

