organization := "jess"

name := "jess"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val akkaHttpVersion = "2.0.3"
  val akkaVersion = "2.4.1"
  Seq(
    "com.typesafe.akka" % "akka-stream-experimental_2.11" % akkaHttpVersion withSources(),
    "com.typesafe.akka" % "akka-http-core-experimental_2.11" % akkaHttpVersion withSources(),
    "com.typesafe.akka" % "akka-http-experimental_2.11" % akkaHttpVersion withSources(),
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaHttpVersion withSources(),
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion withSources() withJavadoc(),
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "org.slf4j" % "slf4j-api" % "1.7.1",
    "ch.qos.logback" % "logback-classic" % "1.0.3",
    "org.typelevel" %% "cats" % "0.4.1",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "com.typesafe.akka" % "akka-http-testkit-experimental_2.11" % akkaHttpVersion % "test",
    "com.ironcorelabs" %% "cats-scalatest" % "1.1.2" % "test"
  )
}

scalacOptions ++= Seq(
  "-feature",
  "-language:postfixOps"
)
