ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

lazy val root = (project in file("."))
  .settings(
    name := "Snapshot"
  )

lazy val AkkaVersion = "2.8.5"
lazy val scalatest = "3.2.18"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "org.scalatest" %% "scalatest" % scalatest % Test,
  "com.typesafe" % "config" % "1.4.3",
  "com.typesafe.play" %% "play-json" % "2.10.4",
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "org.slf4j" % "slf4j-api" % "2.0.12"
)