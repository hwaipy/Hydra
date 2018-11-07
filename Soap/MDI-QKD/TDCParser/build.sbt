name := "TDCParser"
version := "0.2.0"
scalaVersion := "2.12.7"
organization := "com.hydra"
libraryDependencies += "com.hydra" %% "sydra" % "0.6.0"
libraryDependencies += "org.pegdown" % "pegdown" % "1.6.0"
libraryDependencies += "com.hwaipy" %% "hydrogen" % "0.3.0"
libraryDependencies += "com.hydra" %% "tdc" % "0.1.0"
libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.102-R11"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "org.python" % "jython" % "2.7.0"
scalacOptions ++= Seq("-feature")
scalacOptions ++= Seq("-deprecation")
scalacOptions ++= Seq("-Xlint")
fork := true
