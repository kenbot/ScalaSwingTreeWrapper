package au.ken.treeview

import javax.swing.event.TreeModelListener
import javax.swing.JTree
import javax.swing.event.TreeSelectionListener
import scala.collection.SeqView
import javax.swing.{Icon, JComponent}
import scala.swing.event._
import javax.swing.event.CellEditorListener
import scala.collection._
import scala.collection.mutable.{ListBuffer, Buffer, ArrayBuffer}
import scala.reflect.ClassManifest
import au.ken.treeview.event._
import scala.swing._
import scala.swing.event._
import javax.swing.table._
import java.util.EventObject
import java.io._
import Swing._
import javax.swing.tree.{TreePath, TreeNode, MutableTreeNode, TreeCellRenderer, DefaultTreeCellRenderer, 
                            TreeSelectionModel, TreeModel, DefaultTreeModel, TreeCellEditor => JTreeCellEditor, DefaultTreeCellEditor}
import javax.swing.DefaultCellEditor

sealed trait TreeEditors extends CellsCompanion {
  this: Tree.type => 

  protected override type Owner[A] = Tree[A]

  def objInfo(a: Any) = ":" + a.asInstanceOf[AnyRef].getClass.getName + ":" + a; // TODO removeMe

  
  object Editor extends CellEditorCompanion {
    case class Params(isSelected: Boolean = false, isExpanded: Boolean = false, isLeaf: Boolean = false, row: Int = 0)
    override val emptyParams = Params()
    override type Peer = JTreeCellEditor
  
    def wrap[A](e: JTreeCellEditor): Editor[A] = new Wrapped[A](e)
   
    /**
     * Wrapper for <code>javax.swing.tree.TreeCellEditor<code>s
     */
    class Wrapped[A](override val peer: JTreeCellEditor) extends Editor[A] {
      override def componentFor(tree: Tree[_], a: A, p: Params): Component = {
        Component.wrap(peer.getTreeCellEditorComponent(tree.peer, a, p.isSelected, p.isExpanded, p.isLeaf, p.row).asInstanceOf[JComponent])
      }
      def value = peer.getCellEditorValue.asInstanceOf[A]
    }
   
    /**
     * Returns an editor for items of type <code>A</code>. The given function
     * converts items of type <code>A</code> to items of type <code>B</code>
     * for which an editor is implicitly given. This allows chaining of
     * editors, e.g.:
     *
     * <code>
     * case class Person(name: String, subordinates: List[Person])
     * val persons = Person("John", List(Person("Jack", Nil), Person("Jill", List(Person("Betty", Nil)))))
     * val tree = new Tree[Person](persons, _.subordinates) {
     *   editor = Editor(_.name, s => Person(s, Nil))
     * }
     * </code>
     */
    def apply[A, B](toB: A => B, toA: B => A)(implicit editor: Editor[B]): Editor[A] = new Editor[A] {
      chainToPeer(editor.peer)
      def componentFor(tree: Tree[_], a: A, p: Params): Component = editor.componentFor(tree, toB(a), p)
      def value = toA(editor.value)
    }
  }

  /**
   * A tree cell editor.
   * @see javax.swing.tree.TreeCellEditor
   */
  abstract class Editor[A] extends CellEditor[A] {
    import Editor._
    val companion = Editor
    
    protected class TreeEditorPeer extends EditorPeer with JTreeCellEditor {
      override def getTreeCellEditorComponent(tree: JTree, value: Any, isSelected: Boolean, isExpanded: Boolean, isLeaf: Boolean, row: Int) = {
        def treeWrapper(tree: JTree) = tree match {
          case t: JTreeMixin[A] => t.treeWrapper
          case _ => assert(false); null
        }
        componentFor(treeWrapper(tree), value.asInstanceOf[LazyNode[A]].userObject, Params(isSelected, isExpanded, isLeaf, row)).peer
      }
    }

    private[this] lazy val _peer: JTreeCellEditor = new TreeEditorPeer
    def peer = _peer // We can't use a lazy val directly, as Wrapped wouldn't be able to override with a non-lazy val.
  }
  
  

}


sealed trait TreeRenderers extends CellsCompanion {
  this: Tree.type =>
  import javax.swing.tree.{TreeCellRenderer => JTreeCellRenderer}
  
  protected override type Owner[A] = Tree[A]  
  
  object Renderer extends CellRendererCompanion {
      
    case class Params(isSelected: Boolean = false, isExpanded: Boolean = false, 
            isLeaf: Boolean = false, row: Int = 0, hasFocus: Boolean = false)
    override val emptyParams = Params()
    override type Peer = JTreeCellRenderer
            
    def wrap[A](r: Peer): Renderer[A] = new Wrapped[A](r)
   
    /**
     * Wrapper for <code>javax.swing.tree.TreeCellRenderer<code>s
     */
    class Wrapped[-A](override val peer: Peer) extends Renderer[A] {
      override def componentFor(tree: Tree[_], value: A, p: Params): Component = {
        Component.wrap(peer.getTreeCellRendererComponent(tree.peer, value, 
                p.isSelected, p.isExpanded, p.isLeaf, p.row, p.hasFocus).asInstanceOf[JComponent])
      }
    }

    def apply[A,B](f: A => B)(implicit renderer: Renderer[B]): Renderer[A] = new Renderer[A] {
      def componentFor(tree: Tree[_], value: A, p: Params): Component = {
        renderer.componentFor(tree, f(value), Params(p.isSelected, p.isExpanded, p.isLeaf, p.row, p.hasFocus))}
    }
  }
  
  /**
   * A tree item renderer.
   * @see javax.swing.tree.TreeCellRenderer
   */
  abstract class Renderer[-A] extends CellRenderer[A] {
    import Renderer._
    val companion = Renderer

    def peer: Peer = new JTreeCellRenderer {
      def getTreeCellRendererComponent(tree: JTree, value: AnyRef, isSelected: Boolean, isExpanded: Boolean, 
                                       isLeaf: Boolean, row: Int, hasFocus: Boolean) = {

        componentFor(tree match {
          case t: JTreeMixin[A] => t.treeWrapper
          case _ => assert(false); null
        }, value.asInstanceOf[LazyNode[A]].userObject, Params(isSelected, isExpanded, isLeaf, row, hasFocus)).peer
      }
    }
  }
 
  abstract class AbstractRenderer[-A, C<:Component](val component: C) extends Renderer[A] {
    import Renderer._
  
    // The renderer component is responsible for painting selection
    // backgrounds. Hence, make sure it is opaque to let it draw
    // the background.
    component.opaque = true

    /**
     * Standard preconfiguration that is commonly done for any component.
     */
    def preConfigure(tree: Tree[_], value: A, p: Params) {
        
    }
    /**
     * Configuration that is specific to the component and this renderer.
     */
    def configure(tree: Tree[_], value: A, p: Params)

    /**
     * Configures the component before returning it.
     */
    def componentFor(tree: Tree[_], value: A, p: Params): Component = {
      preConfigure(tree, value, p)
      configure(tree, value, p)
      component
    }
  }

  class LabelRenderer[A](convert: A => (Icon, String)) extends AbstractRenderer[A, Label](new Label) {
    def this() = this(a => (EmptyIcon, a.toString))
   
    def configure(tree: Tree[_], a: A, p: Renderer.Params) {
      val (icon, text) = convert(a)
      component.icon = icon
      component.text = text
    }
  }
  
  /**
   * A generic renderer that uses Swing's built-in renderers. If there is no
   * specific renderer for a type, this renderer falls back to a renderer
   * that renders the string returned from an item's <code>toString</code>.
   */
  implicit object GenericRenderer extends Renderer[Any] {
    override lazy val peer: TreeCellRenderer = new DefaultTreeCellRenderer
    override def componentFor(tree: Tree[_], a: Any, p: Renderer.Params): Component = {
      val c = peer.getTreeCellRendererComponent(tree.peer, a, p.isSelected, p.isExpanded, 
          p.isLeaf, p.row, p.hasFocus).asInstanceOf[JComponent]
      Component.wrap(c)
    }
  }
}

object Tree extends TreeRenderers with TreeEditors {

  val Path = List
  type Path[+A] = List[A]
  
  def apply[A](root: A)(children: A => Seq[A]) = new Tree(root, children)

  object SelectionMode extends Enumeration {
    val Contiguous = Value(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION)
    val Discontiguous = Value(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
    val Single = Value(TreeSelectionModel.SINGLE_TREE_SELECTION)
  }
  
  implicit def pathToTreePath[A](p: Path[A]) = if (p == null) null else {
    implicit val cm = ClassManifest.Object // Tells toArray() to result in an Array[AnyRef]
    val array = p.map(_.asInstanceOf[AnyRef]).toArray
    new TreePath(array)
  }
  implicit def treePathToPath[A](tp: TreePath): Path[A] = if (tp == null) null else (tp.getPath map (_.asInstanceOf[A])).toList
  

  private[treeview] sealed trait SwingTreeNode[A] extends MutableTreeNode {
    def childNodes: Seq[LazyNode[A]]
    
    def getChildAt(childIndex: Int): TreeNode = childNodes(childIndex)
    def getChildCount() = childNodes.size
    def getIndex(node: TreeNode) = childNodes indexOf node
    def getAllowsChildren() = true
    def isLeaf() = childNodes.isEmpty
    def children(): java.util.Enumeration[A] = new java.util.Enumeration[A] {
      val iterator = childNodes.iterator
      def hasMoreElements() = iterator.hasNext
      def nextElement = iterator.next.userObject
    }
    override def equals(o: Any) = o match {
      case r: AnyRef => r eq this
      case _ => false
    }
  }
  
  private class RootNode[A](roots: Seq[A], children: A => Seq[A]) extends SwingTreeNode[A] {
    lazy val childNodes = for (r <- roots.toList) yield new LazyNode[A](this, r, children)
    def getParent(): TreeNode = null
    private def notSupported = error("Not supported")
    def insert(child: MutableTreeNode, index: Int) = notSupported
    def remove(index: Int) = notSupported
    def remove(node: MutableTreeNode) = notSupported
    def removeFromParent() = notSupported
    def setParent(newParent: MutableTreeNode) = notSupported
    def setUserObject(obj: AnyRef) = notSupported
    override def toString() = "root"
  }
  
  private[treeview] class LazyNode[A](var parent: MutableTreeNode, var userObject: A, children: A => Seq[A]) extends SwingTreeNode[A] {
     var childNodes: Seq[LazyNode[A]] = 
        Seq().view ++ children(userObject).view.map(n => new LazyNode(this, n, children))

    def getParent(): TreeNode = parent
    def insert(child: MutableTreeNode, index: Int) {
      //childNodes.insert(index, child.asInstanceOf[LazyNode[A]])}
      val (before, after) = childNodes splitAt index
      childNodes = (before :+ child.asInstanceOf[LazyNode[A]]) ++ after
    }
    def remove(index: Int) {
      //childNodes remove index
      val (before, after) = childNodes splitAt index
      childNodes = before ++ after.tail
    }
    def remove(node: MutableTreeNode) {
      //childNodes -= node.asInstanceOf[LazyNode[A]]
      val (before, after) = childNodes splitAt childNodes.indexOf(node)
      childNodes = before ++ after.tail
    }
    def removeFromParent() {parent remove this}
    def setParent(newParent: MutableTreeNode) {parent = newParent.asInstanceOf[LazyNode[A]]}
    def setUserObject(obj: AnyRef) {userObject = obj.asInstanceOf[A]}
    override def toString() = userObject.toString
  }


  object TreeData {
    def empty[A] = TreeData[A](Seq.empty, _ => Seq.empty)
  }
  
  /**
   * Represents tree data as a sequence of root nodes, and a function that can retrieve child nodes.  
   */
  case class TreeData[A](roots: Seq[A], children: A => Seq[A]) extends Iterable[A] {
    self =>
    
    private lazy val rootNode = new RootNode(roots, children)
    lazy val peer: TreeModel = new DefaultTreeModel(rootNode)
    
    override def iterator: Iterator[A] = breadthFirstIterator

    private trait TreeIterator extends Iterator[A] {
      protected var openNodes: Iterator[LazyNode[A]] = rootNode.childNodes.iterator

      def pushChildren(item: LazyNode[A]): Unit
      def hasNext = openNodes.nonEmpty
      def next() = if (openNodes.hasNext) {
        val item = openNodes.next
        pushChildren(item)
        item.userObject
      }
      else error("No more items")
    }
    
    def breadthFirstIterator: Iterator[A] = new TreeIterator {
      override def pushChildren(item: LazyNode[A]) {openNodes ++= item.childNodes.iterator}
    }
    
    def depthFirstIterator: Iterator[A] = new TreeIterator {
      override def pushChildren(item: LazyNode[A]) {
        val open = openNodes
        openNodes = item.childNodes.iterator ++ open // ++ is by-name, and should not directly pass in a val
      }
    }
    
  }
  
  private[treeview] trait JTreeMixin[A] { def treeWrapper: Tree[A] }
}



/**
 * Wrapper for a JTree.  The tree model is represented by a 
 * lazy child expansion function that may or may not terminate in leaf nodes.
 * 
 * @see javax.swing.JTree
 */
class Tree[A] extends {private var treeDataModel = Tree.TreeData.empty[A]} with Component with Cells[A] with Scrollable.Wrapper { thisTree =>

  import Tree._  
  
  
  
  def this(tree: Tree.TreeData[A]) = {
    this()
    treeData = tree
  }
  
  //def this(roots: Seq[A], children: A => Seq[A]) = this(Tree.TreeData(roots, children))
  def this(root: A, children: A => Seq[A]) = this(Tree.TreeData(Seq(root), children))
  
  override val companion = Tree
  

 
  override lazy val peer: JTree = new JTree(treeData.peer) with SuperMixin with JTreeMixin[A] {
    def treeWrapper = thisTree
    
    // We keep the true root node as an invisible and empty value; the user's data will 
    // always sit visible beneath it.  This is so we can support multiple "root" nodes from the 
    // user's perspective, while maintaining type safety.
    setRootVisible(false)
  }
  
  protected def scrollablePeer = peer
  
  /**
   *  Implicit method to produce a generic editor.
   *  This lives in the Tree class rather than the companion object because it requires an actual javax.swing.JTree instance to be initialised.
   */
  implicit def GenericEditor[B] = new Editor[B] {
    override lazy val peer: JTreeCellEditor = new DefaultTreeCellEditor(thisTree.peer, new DefaultTreeCellRenderer) {
      listenToPeer(this)
    }
    override def componentFor(tree: Tree[_], a: B, p: Editor.Params): Component = {
      val c = peer.getTreeCellEditorComponent(tree.peer, a, p.isSelected, p.isExpanded, 
          p.isLeaf, p.row).asInstanceOf[java.awt.Component] // Is actually a Component, not a JComponent
      val jComp = new javax.swing.JPanel(new java.awt.GridLayout(1,1))
      jComp add c
      Component.wrap(jComp) // Needs to wrap JComponent
    }
    def value = peer.getCellEditorValue.asInstanceOf[B]
  }
  
  
  /**
   * Selection model for Tree
   */
  object selection extends Publisher {
    protected abstract class SelectionSet[S](a: => Seq[S]) extends mutable.Set[S] { 
      def -=(s: S): this.type 
      def +=(s: S): this.type
      def --=(ss: Seq[S]): this.type 
      def ++=(ss: Seq[S]): this.type
      override def size = a.length
      def contains(s: S) = a contains s
      def iterator = a.iterator
      def leadSelection: S
    }

    object rows extends SelectionSet(peer.getSelectionRows) {
      def -=(row: Int) = {peer.removeSelectionRow(row); this}
      def +=(row: Int) = {peer.addSelectionRow(row); this}
      def --=(rows: Seq[Int]) = {peer.removeSelectionRows(rows.toArray); this}
      def ++=(rows: Seq[Int]) = {peer.addSelectionRows(rows.toArray); this}
      def maxSelection = peer.getMaxSelectionRow
      def minSelection = peer.getMinSelectionRow
      def leadSelection = peer.getLeadSelectionRow
    }

    object paths extends SelectionSet[Path[A]](peer.getSelectionPaths map treePathToPath toSeq) {
      def -=(path: Path[A]) = { peer.removeSelectionPath(path); this }
      def +=(path: Path[A]) = { peer.addSelectionPath(path); this }
      def --=(paths: Seq[Path[A]]) = { peer.removeSelectionPaths(paths map pathToTreePath toArray); this }
      def ++=(paths: Seq[Path[A]]) = { peer.addSelectionPaths(paths map pathToTreePath toArray); this }
      def leadSelection = peer.getLeadSelectionPath
    }

    peer.getSelectionModel.addTreeSelectionListener(new TreeSelectionListener {
      def valueChanged(e: javax.swing.event.TreeSelectionEvent) {
        val (newPath, oldPath) = e.getPaths.map(treePathToPath).toList.partition(e.isAddedPath(_))
        publish(new TreePathSelected(thisTree, newPath, oldPath, 
                Option(e.getNewLeadSelectionPath: Path[A]), 
                Option(e.getOldLeadSelectionPath: Path[A])))
      }
    })
    
    def mode = SelectionMode(peer.getSelectionModel.getSelectionMode)
    def selectedNode: A = peer.getLastSelectedPathComponent.asInstanceOf[A]
    def empty = peer.isSelectionEmpty
    def count = peer.getSelectionCount

  }

  
  protected val modelListener = new TreeModelListener {
    override def treeStructureChanged(e: javax.swing.event.TreeModelEvent) {
      publish(TreeStructureChanged[A](Tree.this, e.getPath.asInstanceOf[Array[A]].toList, 
              e.getChildIndices.toList, e.getChildren.asInstanceOf[Array[A]].toList))
    }
    override def treeNodesInserted(e: javax.swing.event.TreeModelEvent) {
      publish(TreeNodesInserted[A](Tree.this, e.getPath.asInstanceOf[Array[A]].toList, 
              e.getChildIndices.toList, e.getChildren.asInstanceOf[Array[A]].toList))
    }
    override def treeNodesRemoved(e: javax.swing.event.TreeModelEvent) {
      publish(TreeNodesRemoved[A](Tree.this, e.getPath.asInstanceOf[Array[A]].toList, 
              e.getChildIndices.toList, e.getChildren.asInstanceOf[Array[A]].toList))
    }
    def treeNodesChanged(e: javax.swing.event.TreeModelEvent) {
      publish(TreeNodesChanged[A](Tree.this, e.getPath.asInstanceOf[Array[A]].toList, 
              e.getChildIndices.toList, e.getChildren.asInstanceOf[Array[A]].toList))
    }
  }
  
  def isVisible(path: Path[A]) = peer isVisible path

  def expandPath(path: Path[A]) {peer expandPath path}
  def expandRow(row: Int) {peer expandRow row}
  
  /**
   * Expands every row. Will not terminate if the tree is of infinite size.
   */
  def expandAll() {0 until rowCount foreach expandRow}
  
  def collapsePath(path: Path[A]) {peer collapsePath path}
  def collapseRow(row: Int) {peer collapseRow row}

  def treeData = treeDataModel
  def treeData_=(td: TreeData[A]) = {treeDataModel = td; peer setModel td.peer}
  
  override def cellValues: Iterator[A] = treeData.iterator
  
  /**
   * Collapses all visible rows.
   */
  def collapseAll() {0 until rowCount foreach collapseRow}
  
  def isExpanded(path: Path[A]) = peer isExpanded path
  def isCollapsed(path: Path[A]) = peer isCollapsed path
  
  def editable: Boolean = peer.isEditable
  def editable_=(b: Boolean) {peer setEditable b}
  
  def editor: Tree.Editor[A] =  Tree.Editor.wrap(peer.getCellEditor)
  def editor_=(r: Tree.Editor[A]) { peer.setCellEditor(r.peer) }
  
  def renderer: Tree.Renderer[A] = Tree.Renderer.wrap(peer.getCellRenderer)
  def renderer_=(r: Tree.Renderer[A]) { peer.setCellRenderer(r.peer) }
  
  def showsRootHandles = peer.getShowsRootHandles
  def showsRootHandles(b:Boolean) {peer.setShowsRootHandles(b)}
  
  //def rootVisible = peer.isRootVisible
  def rowCount = peer.getRowCount
  def rowHeight = peer.getRowHeight
  def largeModel = peer.isLargeModel
  def scrollableTracksViewportHeight = peer.getScrollableTracksViewportHeight
  def expandsSelectedPaths = peer.getExpandsSelectedPaths
  def expandsSelectedPaths_=(b: Boolean) { peer.setExpandsSelectedPaths(b) }
  def dragEnabled = peer.getDragEnabled
  def dragEnabled_=(b: Boolean) { peer.setDragEnabled(b) }

}

