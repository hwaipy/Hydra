name := "sydra"
version := "0.7.0"
scalaVersion := "2.12.7"
organization := "com.hydra"
libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.12.7"
libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.12.7"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.1.1"
libraryDependencies += "org.msgpack" % "msgpack-core" % "0.8.16"
libraryDependencies += "org.msgpack" % "jackson-dataformat-msgpack" % "0.8.16"
libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.11.1"
libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.11.1"
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.11.1"
libraryDependencies += "io.netty" % "netty-all" % "4.1.31.Final"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.10"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % "0.10.0")
libraryDependencies += "org.pegdown" % "pegdown" % "1.6.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
scalacOptions ++= Seq("-feature")
scalacOptions ++= Seq("-deprecation")