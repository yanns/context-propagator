name := """context-propagator"""

version := "1.0"

scalaVersion := "2.11.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.6" % "test"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.3"
)

