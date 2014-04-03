resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.98.0")

addSbtPlugin("com.sksamuel.scoverage" %% "sbt-coveralls" % "0.0.5")

