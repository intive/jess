val akkaHttpVersion = "2.0.3"
val akkaVersion = "2.4.1"
val monocleVersion = "1.2.0"

lazy val root = (project in file("."))
  .aggregate(jess, frontend)

lazy val commonSettings = Seq(
  scalaVersion := "2.11.7",
  name := "jess",
  version := "1.0-SNAPSHOT",
  scalacOptions ++= Seq(
  "-feature",
  "-language:postfixOps",
  "-language:higherKinds"
  )
)

lazy val jess = project.in(file("jess"))
  .settings(commonSettings: _*)
  .settings(
  libraryDependencies ++= Seq(
      "com.typesafe.akka" % "akka-stream-experimental_2.11" % akkaHttpVersion withSources(),
      "com.typesafe.akka" % "akka-http-core-experimental_2.11" % akkaHttpVersion withSources(),
      "com.typesafe.akka" % "akka-http-experimental_2.11" % akkaHttpVersion withSources(),
      "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaHttpVersion withSources(),
      "com.typesafe.akka" %% "akka-stream-experimental" % akkaHttpVersion withSources(),
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion withSources() withJavadoc(),
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
      "org.slf4j" % "slf4j-api" % "1.7.1",
      "ch.qos.logback" % "logback-classic" % "1.0.3",
      "org.typelevel" %% "cats" % "0.4.1",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test",
      "com.typesafe.akka" % "akka-http-testkit-experimental_2.11" % akkaHttpVersion % "test",
      "com.ironcorelabs" %% "cats-scalatest" % "1.1.2" % "test",
      "com.github.julien-truffaut"  %%  "monocle-core"    % monocleVersion,
      "com.github.julien-truffaut"  %%  "monocle-generic" % monocleVersion,
      "com.github.julien-truffaut"  %%  "monocle-macro"   % monocleVersion,
      "com.github.julien-truffaut"  %%  "monocle-state"   % monocleVersion,
      "com.github.julien-truffaut"  %%  "monocle-refined" % monocleVersion,
      "com.github.julien-truffaut"  %%  "monocle-law"     % monocleVersion % "test"
    ),
    (resourceGenerators in Compile) <+=
      (fastOptJS in Compile in frontend, packageScalaJSLauncher in Compile in frontend)
      .map((f1, f2) => {
        Seq(f1.data, f2.data)}),
    watchSources <++= (watchSources in frontend)
)

lazy val frontend =
  project.in(file("frontend"))
    .disablePlugins(SbtScalariform)
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings: _*)
    .settings(
    persistLauncher in Compile := true,
      persistLauncher in Test := false,
      jsDependencies += RuntimeDOM
  )
    .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.0"
  )


scalaJSStage in Global := FastOptStage

