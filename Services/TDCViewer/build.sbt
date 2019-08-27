name := "TDCViewer"
version := "0.1.0"
scalaVersion := "2.12.7"
organization := "com.hydra"
libraryDependencies += "com.hydra" %% "sydra" % "0.7.0"
libraryDependencies += "org.pegdown" % "pegdown" % "1.6.0"
libraryDependencies += "com.hwaipy" %% "hydrogen" % "0.3.1"
libraryDependencies += "org.scalafx" %% "scalafx" % "11-R16"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "org.python" % "jython" % "2.7.0"
scalacOptions ++= Seq("-feature")
scalacOptions ++= Seq("-deprecation")
scalacOptions ++= Seq("-Xlint")
fork := true
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
