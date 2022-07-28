// See README.md for license details.

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "com.github.bkoppelmann"

val chiselVersion = "3.5.1"

lazy val root = (project in file("."))
  .settings(
    name := "pyke",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.5.1" % "test",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.1.1",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.1.1",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.1.1",
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.1.1"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-P:chiselplugin:genBundleElements",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
  )

