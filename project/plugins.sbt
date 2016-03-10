
resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += "bintray-sbt-plugin-releases" at "http://dl.bintray.com/content/sbt/sbt-plugin-releases"

// to show a dependency graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.0")

// to format scala source code
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// enable updating file headers eg. for copyright
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "1.5.0")

// to shoot akka / the jvm in the head
addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

// generates Scala source from your build definitions //
// see:
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.5.0")

// enable compiling *.proto files
// see: http://trueaccord.github.io/ScalaPB/sbt-settings.html
// see: https://github.com/trueaccord/sbt-scalapb
addSbtPlugin("com.trueaccord.scalapb" % "sbt-scalapb" % "0.5.21")

// compiling *.proto files without protoc (for self contained builds)
// see: https://github.com/os72/protoc-jar
libraryDependencies ++= Seq(
  "com.github.os72" % "protoc-jar" % "3.0.0-b2.1"
)