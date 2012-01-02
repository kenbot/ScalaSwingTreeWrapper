import sbt._

class ScalaSwingTreeWrapper(info: ProjectInfo) extends DefaultProject(info) {
  lazy val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
  lazy val scalaSwing = "org.scala-lang" % "scala-swing" % "2.9.1"
  lazy val scalaTest = "org.scalatest" % "scalatest_2.9.1" % "1.6.1"
  lazy val junit4 = "junit" % "junit" % "4.7"
  
  override def consoleInit = """
    import scala.swing._;
    import scala.swing.tree._;
    import scala.swing.test._;
    import Swing._;
    import Tree._;
  """
}