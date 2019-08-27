name := "TDCParser"
version := "0.2.0"
scalaVersion := "2.12.8"
organization := "com.hydra"
libraryDependencies += "com.hydra" %% "sydra" % "0.7.0"
libraryDependencies += "org.pegdown" % "pegdown" % "1.6.0"
libraryDependencies += "com.hwaipy" %% "hydrogen" % "0.3.1"
libraryDependencies += "com.hydra" %% "tdc" % "0.2.0"
libraryDependencies += "org.scalafx" %% "scalafx" % "11-R16"
// Determine OS version of JavaFX binaries
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux") => "linux"
  case n if n.startsWith("Mac") => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}
lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
libraryDependencies ++= javaFXModules.map(m =>
  "org.openjfx" % s"javafx-$m" % "11" classifier osName
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "org.python" % "jython" % "2.7.0"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.4.2"
scalacOptions ++= Seq("-feature")
scalacOptions ++= Seq("-deprecation")
scalacOptions ++= Seq("-Xlint")
fork := true
