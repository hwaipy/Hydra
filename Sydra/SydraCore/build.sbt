name := "sydra-core"
version := "0.1.0"
scalaVersion := "2.12.8"
organization := "com.hydra"
libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.12.8"
libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.12.8"
libraryDependencies += "org.msgpack" % "msgpack-core" % "0.8.16"
libraryDependencies += "org.msgpack" % "jackson-dataformat-msgpack" % "0.8.16"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.10"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % "0.10.0")
scalacOptions ++= Seq("-feature")
scalacOptions ++= Seq("-deprecation")