enablePlugins(ScalaJSPlugin)

name := "sydra-js"
version := "0.1.0"
scalaVersion := "2.12.8"
organization := "com.hydra"

skip in packageJSDependencies := false
jsDependencies += "org.webjars" % "jquery" % "3.3.1" / "3.3.1/jquery.js" minified "3.3.1/jquery.min.js"

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.12.8"
libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.12.8"
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.6"
libraryDependencies += "org.querki" %%% "jquery-facade" % "1.2"
libraryDependencies += "com.typesafe.play" %%% "play-json" % "2.7.0"
libraryDependencies += "com.hydra" %%% "sydra-core" % "0.1.0"
scalacOptions ++= Seq("-feature")
scalacOptions ++= Seq("-deprecation")

scalaJSUseMainModuleInitializer := true
jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()

libraryDependencies += "com.lihaoyi" %%% "utest" % "0.6.3" % "test"
testFrameworks += new TestFramework("utest.runner.Framework")