name := "ScalaSwingTreeWrapper"

version := "1.2"

scalaVersion := "2.10.0"

scalacOptions ++= Seq("-deprecation")

libraryDependencies <+= scalaVersion { sv =>
  "org.scala-lang" % "scala-swing" % sv
}

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "junit" % "junit" % "4.11" % "test"
)

retrieveManaged := true

initialCommands in console := """
  import scala.swing._
  import tree._
  import test._
  import Swing._
  import Tree._
"""
