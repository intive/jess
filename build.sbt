organization := "jess"

name := "jess"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val sprayVersion = "1.3.3"
  Seq(
    "com.typesafe.akka" %% "akka-actor"     % "2.4.1"       withSources() withJavadoc(),
    "io.spray"          %% "spray-can"      % sprayVersion  withSources() withJavadoc(),
    "io.spray"          %% "spray-routing"  % sprayVersion  withSources() withJavadoc()
  )
}
