name := """EventHub"""

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.10" % "2.2.3",
  "com.typesafe.akka" % "akka-testKit_2.10" % "2.2.3" % "test",
  "org.specs2" %% "specs2" % "2.3.7" % "test",
  "com.ning" % "async-http-client" % "1.8.1",
  "org.mongodb" % "casbah_2.10" % "2.6.5"
)

