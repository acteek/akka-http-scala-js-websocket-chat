import sbt._
import Keys._

import org.scalajs.sbtplugin.ScalaJSPlugin
import ScalaJSPlugin._
import autoImport._
import sbtassembly.AssemblyKeys


object ChatBuild extends Build {
  lazy val scalaV = "2.11.8"
  lazy val akkaV = "2.4.16"
  lazy val akkaHttpV = "10.0.1"
  lazy val upickleV = "0.4.4"

  lazy val root =
    Project("root", file("."))
      .aggregate(frontend, backend, cli)

  // Scala-Js frontend
  lazy val frontend =
    Project("frontend", file("frontend"))
      .enablePlugins(ScalaJSPlugin)
      .settings(commonSettings: _*)
      .settings(
        persistLauncher in Compile := true,
        persistLauncher in Test := false,
        testFrameworks += new TestFramework("utest.runner.Framework"),
        libraryDependencies ++= Seq(
          "org.scala-js" %%% "scalajs-dom" % "0.9.1",
          "com.lihaoyi" %%% "upickle" % upickleV,
          "com.lihaoyi" %%% "utest" % "0.4.4" % "test"
        )
      )
      .dependsOn(sharedJs)

  // Akka Http based backend
  lazy val backend =
    Project("backend", file("backend"))
      .settings(commonSettings: _*)
      .settings(
        libraryDependencies ++= Seq(
          "com.typesafe.akka" %% "akka-http" % akkaHttpV,
          "org.specs2" %% "specs2-core" % "2.4.17" % "test",
          "com.lihaoyi" %% "upickle" % upickleV
        ),
        resourceGenerators in Compile += Def.task {
          val f1 = (fastOptJS in Compile in frontend).value
          val f2 = (packageScalaJSLauncher in Compile in frontend).value
          Seq(f1.data, f2.data)
        }.taskValue,
        watchSources ++= (watchSources in frontend).value
      )
      .dependsOn(sharedJvm)

  lazy val cli =
    Project("cli", file("cli"))
      .settings(commonSettings: _*)
      .settings(
        libraryDependencies ++= Seq(
          "com.typesafe.akka" %% "akka-http-core" % akkaHttpV,
          "org.specs2" %% "specs2-core" % "2.4.17" % "test",
          "com.lihaoyi" %% "upickle" % upickleV
        ),
        fork in run := true,
        connectInput in run := true
      )
      .dependsOn(sharedJvm)

  lazy val shared = 
    (crossProject.crossType(CrossType.Pure) in file ("shared"))
      .settings(
        scalaVersion := scalaV
      )


  lazy val sharedJvm= shared.jvm
  lazy val sharedJs= shared.js

  def commonSettings = Seq(
    scalaVersion := scalaV
  ) ++ ScalariformSupport.formatSettings
}
