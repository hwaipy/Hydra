name := "MongoDB"
version := "0.1.0"
scalaVersion := "2.12.7"
organization := "com.hydra"
libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.12.7"
libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.12.7"
libraryDependencies += "org.apache.logging.log4j" % "log4j" % "2.11.1"
libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.11.1"
libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.11.1"
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.11.1"
libraryDependencies += "com.hydra" %% "sydra" % "0.6.0"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.4.2"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
scalacOptions ++= Seq("-feature")
scalacOptions ++= Seq("-deprecation")