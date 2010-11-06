package scala.swing.test

import org.scalatest._
import matchers._
import scala.swing._
import scala.swing.event._


class TreeSpec extends Spec with ShouldMatchers  {

  case class Node[A](var value: A, var children: Node[A]*) {
    override def toString() = value.toString
  }

  val dataTree = 
    Node("Hobbies", 
      Node("Skateboarding"), 
      Node("Indoor", 
        Node("Chess", 
          Node("Chinese"), 
          Node("International")),
        Node("Draughts")),
      Node("Spelunking"))
  
  val (hobbies, skateboarding, indoor, chess, chinese, international, draughts, spelunking) = 
    dataTree match {
      case a @ Node("Hobbies", 
        b @ Node("Skateboarding"), 
        c @ Node("Indoor", 
          d @ Node("Chess", 
            e @ Node("Chinese"), 
            f @ Node("International")),
          g @ Node("Draughts")),
        h @ Node("Spelunking")) => (a, b, c, d, e, f, g, h)
    }
  
  def createTreeView() = new Tree[Node[String]] {
    treeData = TreeModel(dataTree)(_.children) updatableWith {(path, newValue) => path.last.value = newValue.value; newValue}
    selection.mode = Tree.SelectionMode.Contiguous
    editor = Tree.Editor(_.value, (str: String) => Node(str))
  }
               

  describe("TreeData") {
    it("should contain appropriate data") {
      val treeData = TreeModel(dataTree)(_.children)
      treeData should have size (8)
    }
  
    it("should contain appropriate data with the root/childFunc constructor") {
      val tree = new Tree[Node[String]](Seq(dataTree), _.children)
      tree.treeData should have size (8)
    }
    it("should traverse breadth first correctly") {
      val tree = createTreeView()
      tree.treeData.breadthFirstIterator.map(_.value).toList should equal (
          List("Hobbies", "Skateboarding", "Indoor", "Spelunking", "Chess", "Draughts", "Chinese", "International"))
    }
    
    it("should traverse depth first correctly") {
      val tree = createTreeView()
      tree.treeData.depthFirstIterator.map(_.value).toList should equal (
          List("Hobbies", "Skateboarding", "Indoor", "Chess", "Chinese", "International", "Draughts", "Spelunking"))
    }
  }
  
  describe("Row selection") {
    it("should reflect the rows selected") {
      val tree = createTreeView()
      tree.expandAll()
      tree.selectRows(1,2,3)
      tree.selection.rows should equal (Set(1,2,3))
    }
  }
  
  describe("Path selection") {
    it("should reflect the paths selected") {
      val tree = createTreeView()
      tree.selection.mode = Tree.SelectionMode.Discontiguous
      
      val path1 = Tree.Path(hobbies, indoor, chess, chinese)
      val path2 = Tree.Path(hobbies, indoor, chess, international)
      tree.selectPaths(path1, path2)
      tree.selection.paths.toSet should equal (Set(path1, path2))
    }
  }
  
  describe("Tree Path") {
    it("should contain the correct elements") {
      val tree = createTreeView()
      tree.selectRows(0)
      tree.selection.paths.toSet should equal (List(Tree.Path(hobbies)).toSet)
    }
  }
  
  describe("Default Renderer") {
    it("Should provide an expected result") {
      val tree = createTreeView()
      tree.renderer = Tree.Renderer("$" + _.value)
      
      tree.treeData should have size (8)
    }
  
  }
  
  describe("Editor") {
    it("should start editing when asked") {
      val tree = createTreeView()
      tree.startEditingAtPath(hobbies :: indoor :: chess :: Nil)
      tree.isEditing should be (true)
    }
    /*
    it("should update tree model when stopped") {
      val tree = createTreeView()
      tree.startEditingAtPath(hobbies :: indoor :: chess :: Nil)
      tree.editor.isEditing should be (true)
      
      tree.editor.peer.asInstanceOf[javax.swing.tree.DefaultTreeCellEditor].
    }
    
    it("should leave tree model unchanged when cancelled") {
      val tree = createTreeView()
      tree.startEditingAtPath(hobbies :: indoor :: chess :: Nil)
      tree.editor.isEditing should be (true)
    }*/
  }
}