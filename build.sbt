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
name := "demo-akka-persistence-jdbc"

organization := "com.github.dnvriend"

version := "1.0.0"

scalaVersion := "2.11.8"

// the akka-persistence-jdbc plugin lives here
resolvers += Resolver.jcenterRepo

// the slick-extension library (which is used by akka-persistence-jdbc) lives here
resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/maven-releases/"

libraryDependencies ++= {
  val akkaVersion = "2.4.7"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
    "com.twitter" %% "chill-akka" % "0.8.0",
    "com.github.dnvriend" %% "akka-persistence-jdbc" % "2.2.24" changing(),
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "org.postgresql" % "postgresql" % "9.4.1208.jre7",
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "2.2.4" % Test
  )
}

fork in Test := true

parallelExecution in Test := false

scalacOptions ++= Seq("-feature", "-language:higherKinds", "-language:implicitConversions", "-deprecation", "-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8")

licenses +=("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

// enable scala code formatting //
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform

// Scalariform settings
SbtScalariform.autoImport.scalariformPreferences := SbtScalariform.autoImport.scalariformPreferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)

// enable updating file headers //
import de.heikoseeberger.sbtheader.license.Apache2_0

headers := Map(
  "scala" -> Apache2_0("2016", "Dennis Vriend"),
  "conf" -> Apache2_0("2016", "Dennis Vriend", "#")
)

// enable sbt-revolver
Revolver.settings ++ Seq(
  Revolver.enableDebugging(port = 5050, suspend = false),
  mainClass in reStart := Some("com.github.dnvriend.Counter")
)

// enable protobuf plugin //
// https://trueaccord.github.io/ScalaPB/sbt-settings.html
import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

PB.protobufSettings

// protoc-jar which is on the sbt classpath //
// https://github.com/os72/protoc-jar
PB.runProtoc in PB.protobufConfig := (args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray))

// build info configuration //
buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

buildInfoPackage := "com.github.dnvriend"

// enable plugins
enablePlugins(AutomateHeaderPlugin, BuildInfoPlugin)
