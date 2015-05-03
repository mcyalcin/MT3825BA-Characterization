name := "MT3825BA Characterization"

version := "1.0"

scalaVersion := "2.11.6"

unmanagedBase := baseDirectory.value / "lib"

unmanagedJars in Compile ++= {
  val base = baseDirectory.value
  val baseDirectories = base / "lib"
  val customJars = baseDirectories ** "*.jar"
  customJars.classpath
}

resolvers += "Cloudera's CDH3 Maven repo" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
  "org.scalafx" % "scalafx_2.11" % "8.0.0-R4",
  "org.spire-math" % "spire_2.11" % "0.9.1",
  "gov.nih.imagej" % "imagej" % "1.47",
  "org.controlsfx" % "controlsfx" % "8.0.6_20",
  "com.github.jai-imageio" % "jai-imageio-core" % "1.3.0",
  "com.lambdaworks" % "jacks_2.11" % "2.3.3",
  "commons-io" % "commons-io" % "2.4"
)
    