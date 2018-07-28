name := "hydralocallauncher"
version := "0.1.0"
scalaVersion := "2.12.4"
organization := "com.hydra"
libraryDependencies += "com.hydra" %% "sydra" % "0.6.0"
libraryDependencies += "com.hwaipy" %% "hydrogen" % "0.3.0"
libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.102-R11"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true
scalacOptions ++= Seq("-feature")
scalacOptions ++= Seq("-deprecation")
