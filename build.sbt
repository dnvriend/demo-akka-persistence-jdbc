name := "demo-akka-persistence-jdbc"

organization := "com.github.dnvriend"

version := "1.0.0"

scalaVersion := "2.11.7"

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

libraryDependencies ++= {
  val akkaVersion = "2.4.1"
  val akkaStreamAndHttpVersion = "2.0.2"
  val slickVersion = "3.1.1" 
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamAndHttpVersion,
    "com.github.dnvriend" %% "akka-persistence-jdbc" % "2.0.4",
    "org.postgresql" % "postgresql" % "9.4-1206-jdbc42",
    "com.typesafe.slick" %% "slick" % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,    
    "com.typesafe.akka" %% "akka-stream-testkit-experimental" % akkaStreamAndHttpVersion % Test,
    "org.scalatest" %% "scalatest" % "2.2.4" % Test
  )
}

fork in Test := true

javaOptions in Test ++= Seq("-Xms30m","-Xmx30m")

parallelExecution in Test := false

licenses +=("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

// enable scala code formatting //
import scalariform.formatter.preferences._

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
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

enablePlugins(AutomateHeaderPlugin)

import spray.revolver.RevolverPlugin.Revolver
Revolver.settings

Revolver.enableDebugging(port = 5050, suspend = false)

mainClass in Revolver.reStart := Some("com.github.dnvriend.Launch")