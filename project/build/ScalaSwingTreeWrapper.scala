import sbt._

class ScalaSwingTreeWrapper(info: ProjectInfo) extends DefaultProject(info) {
  lazy val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
  lazy val scalaSwing = "org.scala-lang" % "scala-swing" % "2.8.1.RC4"
  lazy val scalaTest = "org.scalatest" % "scalatest" % "1.2-for-scala-2.8.0.final-SNAPSHOT"
  
  override def consoleInit = """
    import scala.swing._;
    import Swing._;
    import Tree._;
    import example._;
  """
}