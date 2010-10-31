package scala.swing.example


import scala.xml.Node
import scala.swing._
import Swing._
import scala.swing.event._
import Tree._
import javax.swing.{Icon => JIcon}

object TreeMain extends SimpleSwingApplication {
  import Tree._
  import java.io._

  case class Person(var name: String, var subordinates: List[Person]) {
    override def toString() = name + (if (subordinates.nonEmpty) "(" + subordinates.mkString(", ") + ")" else "")
  }
  val persons = Person("Bob", Nil) :: Person("John", List(Person("Jack", Nil), Person("Jill", List(Person("Betty", Nil))))) :: Nil
  
  lazy val tree = new Tree[Person] { 
    
    treeData = new TreeModel[Person](persons, _.subordinates) updatableWith {
      (path, newValue) => val current = path.last
          current.name = newValue.name
          current
    } 

    renderer = Renderer(_.name)
    
    // import javax.swing.{DefaultCellEditor, JTextField}
    //editor = Editor.wrap(new DefaultCellEditor(new JTextField))
    editor = Editor((_:Person).name, new Person((_:String), Nil))
    
    listenTo(editor, selection, mouse.clicks)
    
    reactions += {
      case TreePathSelected(_, _, _, newSelection, _) => 
          val last = for (p <- newSelection; last <- p.lastOption) yield last
          println(last getOrElse "None found")
          
      case MouseClicked(tree, _, _, 2, _) => println("click!")
      case CellEditingStopped(_) => println("Stopped: " + persons)
      case CellEditingCancelled(_) => println("Cancelled: " + persons)
    }
    
    expandAll()
  }

  
  val tree2 = new Tree[File] {
    treeData = TreeModel(new File(".")) {f => 
      if (f.isDirectory) f.listFiles.toSeq 
      else Seq()
    }
    
    renderer = new LabelRenderer({f => 
      val iconFile = "examples/" + (if (f.isDirectory) "folder.png" else "file.png")
      (Icon(iconFile), f.getName)
    })
  }
  
  lazy val fileTree = new Tree[File] {
    treeData = TreeModel(new File(".")) {
      f => if (f.isDirectory) f.listFiles.toSeq else Seq()
    } updatableWith { 
      (path, newValue) => val existing = path.last
        val renamedFile = new File(existing.getParent + File.separator + newValue.getName)
        existing renameTo renamedFile
        renamedFile
    }
    renderer = Renderer(_.getName)
    editor = Editor((_:File).getName, new File(_:String))
    expandRow(0)
  }
  
  class CheckBoxRenderer[A](convert: A => (JIcon, Boolean)) extends Tree.AbstractRenderer[A, CheckBox](new CheckBox) {
    def this() = this(a => (null, Option(a).isDefined))
   
    override def configure(tree: Tree[_], a: A, p: Renderer.Params) {
      val (icon, selected) = convert(a)
      component.icon = icon
      component.selected = selected
    }
  }
  
  val javaTree = new javax.swing.JTree(Array[Object]("1", "2", "3", "4")) {
    import javax.swing.tree._
    setCellEditor(new DefaultTreeCellEditor(this, new DefaultTreeCellRenderer()))
    setEditable(true)
    
    setCellRenderer(new DefaultTreeCellRenderer() {
      override def getTreeCellRendererComponent(tree: javax.swing.JTree, value: AnyRef, isSelected: Boolean, isExpanded: Boolean, 
                                       isLeaf: Boolean, row: Int, hasFocus: Boolean) = {
                                       
        super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, row, hasFocus)
      }
    })
  }
  
  def top = new MainFrame {
    contents = tree2
    
    /* listenTo(tree.editor, tree.selection, tree.mouse.clicks)
      
    reactions += {
      case TreePathSelected(_, _, _, newSelection, _) => 
          val last = for (p <- newSelection; last <- p.lastOption) yield last
          println(last getOrElse "None found")
          
      case MouseClicked(tree, _, _, 2, _) => println("click!")
      
      case CellEditingStopped(source: CellEditor[_]) => println(source.value)
    }*/
  }

  def xmlSeqTree = new Tree[Node](
    <html>
      <body>
        <div id="title">Yurtle the Turtle</div>
        <div id="description">This is great for the kids.</div>
      </body>
    </html>, {_.child filterNot (_.text.trim.isEmpty)}) {
      
    renderer = Renderer(n => if (n.label.startsWith("#")) n.text.trim else n.label)
  }
  
  def files(f: File) = if (f.isDirectory) f.listFiles.toSeq else Seq.empty
  
  def filteredFiles(filter: File => Boolean)(f: File): Seq[File] = {
      if (f.isDirectory) f.listFiles(new FileFilter {
        def accept(file: File) = filter(file)
      }) 
      else Seq.empty
  }
  
  def products(node: Any) = node match {
    case s: Seq[_] => s
    case o => Seq.empty
  }
  
  def seqs(node: Any) = node match {
    case s: Seq[_] => s
    case o => Seq.empty
  }
  
  def nestedCaseClassesTree = {
    sealed trait Expr
    case class Plus(a: Expr, b: Expr) extends Expr {override def toString() = a + " + " + b}
    case class Minus(a: Expr, b: Expr) extends Expr {override def toString() = a + " - " + b}
    case class Const(value: Int) extends Expr {override def toString() = value.toString}
    
    new Tree(Seq(Plus(Const(5), Minus(Const(2), Const(1)))), products)
  }

  //def nestedTupleTree = new Tree(Titled("Foods", (Titled("Legumes", ("Lentils", "Chick peas")), "Fruit", "Chips")), products)
  //def nestedListTree = new Tree(Titled("Numbers", 1 :: 2 :: 3 :: Titled("Them", (4 :: 5 :: 6 :: Nil)) :: Nil), products)
  
  //def nestedSeqTree = new Tree(Titled("Numbers", Seq(1, 2, 3, Titled("Them", Seq(4, 5, 6)))), seqs)
  def infiniteFactorTree = new Tree[Int](Seq(10000), n => 1 to n filter (n % _ == 0))

  def filteredFileTree = new Tree[File] {
    treeData = TreeModel(new File(".")) {
      filteredFiles(f => f.isDirectory || f.getName.endsWith(".scala"))
    }
  }
}