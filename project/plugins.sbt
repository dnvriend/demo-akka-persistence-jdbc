
resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += "bintray-sbt-plugin-releases" at "https://dl.bintray.com/content/sbt/sbt-plugin-releases"

// to format scala source code
// https://github.com/sbt/sbt-scalariform
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")

// enable updating file headers eg. for copyright
// https://github.com/sbt/sbt-header
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.6.0")

// to shoot akka / the jvm in the head
// https://github.com/spray/sbt-revolver
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

// generates Scala source from your build definitions //
// https://github.com/sbt/sbt-buildinfo
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

// enable compiling *.proto files
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.25")


libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.9.0"
