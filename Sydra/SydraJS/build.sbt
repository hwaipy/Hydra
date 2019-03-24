enablePlugins(ScalaJSPlugin)

name := "sydra-js"
version := "0.1.0"
scalaVersion := "2.12.8"
organization := "com.hydra"
libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.12.8"
libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.12.8"
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.6"
libraryDependencies += "org.querki" %%% "jquery-facade" % "1.2"
scalacOptions ++= Seq("-feature")
scalacOptions ++= Seq("-deprecation")

scalaJSUseMainModuleInitializer := true
