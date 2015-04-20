name := "MT3825BA Characterization"

version := "1.0"

scalaVersion := "2.11.6"

unmanagedBase := baseDirectory.value / "lib"

unmanagedJars ++= {
  val base = baseDirectory.value
  val baseDirectories = base / "lib"
  val customJars = baseDirectories ** "*.jar"
  customJars.classpath
}

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
  "org.scalafx" % "scalafx_2.11" % "8.0.0-R4",
  "org.spire-math" % "spire_2.11" % "0.9.1",
  "gov.nih.imagej" % "imagej" % "1.47"//,
  //"net.imagej" % "ij" % "1.47e"
)
    