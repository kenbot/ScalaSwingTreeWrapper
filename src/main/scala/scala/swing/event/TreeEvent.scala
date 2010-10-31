package scala.swing.event

import scala.swing.Tree
import scala.swing.event._

trait TreeEvent[A] extends ComponentEvent {
  val source: Tree[A]
}

case class TreePathSelected[A](source: Tree[A], 
        newPaths: List[Tree.Path[A]], 
        oldPaths: List[Tree.Path[A]], 
        newLeadSelectionPath: Option[Tree.Path[A]], 
        oldLeadSelectionPath: Option[Tree.Path[A]]) extends TreeEvent[A] with SelectionEvent

trait TreeExpansionEvent[A] extends TreeEvent[A] {
  val path: Tree.Path[A]
}
case class TreeCollapsed[A](source: Tree[A], path: Tree.Path[A]) extends TreeExpansionEvent[A]
case class TreeExpanded[A](source: Tree[A], path: Tree.Path[A]) extends TreeExpansionEvent[A]
case class TreeWillCollapse[A](source: Tree[A], path: Tree.Path[A]) extends TreeExpansionEvent[A]
case class TreeWillExpand[A](source: Tree[A], path: Tree.Path[A]) extends TreeExpansionEvent[A]

trait TreeModelEvent[A] extends TreeEvent[A] {
    val path: Tree.Path[A]
    val childIndices: List[Int]
    val children: List[A]
}
case class TreeNodesChanged[A](source: Tree[A], path: Tree.Path[A], childIndices: List[Int], children: List[A]) extends TreeModelEvent[A]
case class TreeNodesInserted[A](source: Tree[A], path: Tree.Path[A], childIndices: List[Int], children: List[A]) extends TreeModelEvent[A]
case class TreeNodesRemoved[A](source: Tree[A], path: Tree.Path[A], childIndices: List[Int], children: List[A]) extends TreeModelEvent[A]
case class TreeStructureChanged[A](source: Tree[A], path: Tree.Path[A], childIndices: List[Int], children: List[A]) extends TreeModelEvent[A]
