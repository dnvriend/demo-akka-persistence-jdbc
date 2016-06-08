
resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += "bintray-sbt-plugin-releases" at "http://dl.bintray.com/content/sbt/sbt-plugin-releases"

// to show a dependency graph
// https://github.com/jrudolph/sbt-dependency-graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

// to format scala source code
// https://github.com/sbt/sbt-scalariform
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// enable updating file headers eg. for copyright
// https://github.com/sbt/sbt-header
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.1")

// to shoot akka / the jvm in the head
// https://github.com/spray/sbt-revolver
addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")

// generates Scala source from your build definitions //
// https://github.com/sbt/sbt-buildinfo
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

// enable compiling *.proto files
// http://trueaccord.github.io/ScalaPB/sbt-settings.html
// https://github.com/trueaccord/sbt-scalapb
addSbtPlugin("com.trueaccord.scalapb" % "sbt-scalapb" % "0.5.29")

// compiling *.proto files without protoc (for self contained builds)
// https://github.com/os72/protoc-jar
libraryDependencies ++= Seq(
  "com.github.os72" % "protoc-jar" % "3.0.0-b2.1"
)