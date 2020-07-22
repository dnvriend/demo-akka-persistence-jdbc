/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
name := "demo-akka-persistence-postgres"

organization := "pl.mkubala"

version := "1.0.0"

scalaVersion := "2.12.12"

// the akka-persistence-postgres plugin lives here
resolvers += Resolver.mavenLocal

// the slick-extension library (which is used by akka-persistence-postgres) lives here
resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/maven-releases/"

libraryDependencies ++= {
  val akkaVersion = "2.6.8"
  val akkaPersistencePostgresVersion = "0.0.0+848-b191b39b+20200722-1240"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
    "com.lihaoyi" %% "pprint" % "0.5.6",
    "com.twitter" %% "chill-akka" % "0.9.5",
    "com.swissborg" %% "akka-persistence-postgres" % akkaPersistencePostgresVersion changing(),
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  )
}

fork in Test := true

parallelExecution in Test := false

scalacOptions ++= Seq("-feature", "-language:higherKinds", "-language:implicitConversions", "-deprecation", "-Ydelambdafy:method", "-target:jvm-1.8")

licenses +=("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

// enable scala code formatting //
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import de.heikoseeberger.sbtheader.LicenseStyle

// Scalariform settings
SbtScalariform.autoImport.scalariformPreferences := SbtScalariform.autoImport.scalariformPreferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)

// enable updating file headers //
headerLicense := Some(HeaderLicense.ALv2("2016", "Dennis Vriend", HeaderLicenseStyle.Detailed))

headerMappings := headerMappings.value +
  (HeaderFileType.scala -> HeaderCommentStyle.cStyleBlockComment) +
  (HeaderFileType.conf -> HeaderCommentStyle.hashLineComment)

// enable sbt-revolver
Revolver.settings ++ Seq(
  Revolver.enableDebugging(port = 5050, suspend = false),
  mainClass in reStart := Some("com.github.dnvriend.LaunchCounter")
)

// enable protobuf plugin //

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

//PB.protobufSettings

// protoc-jar which is on the sbt classpath //
// https://github.com/os72/protoc-jar
//PB.runProtoc in PB.protobufConfig := (args =>
//  com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray))

// build info configuration //
buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

buildInfoPackage := "com.github.dnvriend"

// enable plugins
enablePlugins(AutomateHeaderPlugin, BuildInfoPlugin)
