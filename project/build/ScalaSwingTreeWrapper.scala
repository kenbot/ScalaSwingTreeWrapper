import sbt._

class ScalaSwingTreeWrapper(info: ProjectInfo) extends DefaultProject(info) {
  lazy val scalaSwing = "org.scala-lang" % "scala-swing" % "2.8.0"
}