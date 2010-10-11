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
import java.util.EventObject
import java.io._
import Swing._
import javax.swing.tree.{TreePath, TreeNode, MutableTreeNode, TreeCellRenderer, DefaultTreeCellRenderer, 
                            TreeSelectionModel, TreeModel => JTreeModel, DefaultTreeModel, TreeCellEditor => JTreeCellEditor, DefaultTreeCellEditor}
import javax.swing.DefaultCellEditor

sealed trait TreeEditors extends EditableCellsCompanion {
  this: Tree.type => 

  protected override type Owner = Tree[_]

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
        componentFor(treeWrapper(tree), value.asInstanceOf[A] /* .asInstanceOf[TreeModel.LazyNode[A]].userObject*/, Params(isSelected, isExpanded, isLeaf, row)).peer
      }
    }

    private[this] lazy val _peer: JTreeCellEditor = new TreeEditorPeer
    def peer = _peer // We can't use a lazy val directly, as Wrapped wouldn't be able to override with a non-lazy val.
  }
}


sealed trait TreeRenderers extends RenderableCellsCompanion {
  this: Tree.type =>
  import javax.swing.tree.{TreeCellRenderer => JTreeCellRenderer}
  
  protected override type Owner = Tree[_]
  
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
        renderer.componentFor(tree, f(value), Params(p.isSelected, p.isExpanded, p.isLeaf, p.row, p.hasFocus))
      }
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
        }, value.asInstanceOf[A] /* .asInstanceOf[TreeModel.LazyNode[A]].userObject*/, Params(isSelected, isExpanded, isLeaf, row, hasFocus)).peer
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
  
  object SelectionMode extends Enumeration {
    val Contiguous = Value(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION)
    val Discontiguous = Value(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
    val Single = Value(TreeSelectionModel.SINGLE_TREE_SELECTION)
  }

  private[treeview] trait JTreeMixin[A] { def treeWrapper: Tree[A] }
}



/**
 * Wrapper for a JTree.  The tree model is represented by a 
 * lazy child expansion function that may or may not terminate in leaf nodes.
 * 
 * @see javax.swing.JTree
 */
class Tree[A](private var treeDataModel: TreeModel[A] = TreeModel.empty[A]) 
    extends Component 
    with CellView[A] 
    with EditableCells[A] 
    with RenderableCells[A] 
    with Scrollable.Wrapper { thisTree =>

  import Tree._  

  def this(roots: Seq[A], children: A => Seq[A]) = this(new TreeModel(roots, children))
  
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
   * Implicitly converts Tree.Path[A] lists to TreePath objects understood by the underlying peer JTree. 
   * In addition to the objects in the list, the JTree's hidden root node must be prepended.
   */
  implicit def pathToTreePath(p: Path[A]): TreePath = treeDataModel pathToTreePath p
 
  /**
   * Implicitly converts javax.swing.tree.TreePath objects to Tree.Path[A] lists recognised in Scala Swing.  
   * TreePaths will include the underlying JTree's hidden root node, which is omitted for Tree.Paths.
   */
  implicit def treePathToPath(tp: TreePath): Path[A] = treeDataModel treePathToPath tp
  
  //private def withRoot(tp: TreePath): TreePath = new TreePath(treeDataModel.peer.getRoot +: tp.getPath)
  //private def withoutRoot(tp: TreePath) = new TreePath(tp.getPath.tail)
  
  /**
   *  Implicit method to produce a generic editor.
   *  This lives in the Tree class rather than the companion object because it requires an actual javax.swing.JTree instance to be initialised.
   */
  implicit def genericEditor[B] = new Editor[B] {
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
      override def size = nonNullOrEmpty(a).length
      def contains(s: S) = nonNullOrEmpty(a) contains s
      def iterator = nonNullOrEmpty(a).iterator
      def leadSelection: S

      private[this] def nonNullOrEmpty[A](s: Seq[A]) = if (s != null) s else Seq.empty
    }

    object rows extends SelectionSet(peer.getSelectionRows) {
      def -=(r: Int) = {peer.removeSelectionRow(r); this}
      def +=(r: Int) = {peer.addSelectionRow(r); this}
      def --=(rs: Seq[Int]) = {peer.removeSelectionRows(rs.toArray); this}
      def ++=(rs: Seq[Int]) = {peer.addSelectionRows(rs.toArray); this}
      def maxSelection = peer.getMaxSelectionRow
      def minSelection = peer.getMinSelectionRow
      def leadSelection = peer.getLeadSelectionRow
    }

    object paths extends SelectionSet[Path[A]](peer.getSelectionPaths map treePathToPath toSeq) {
      def -=(p: Path[A]) = { peer.removeSelectionPath(p); this }
      def +=(p: Path[A]) = { peer.addSelectionPath(p); this }
      def --=(ps: Seq[Path[A]]) = { peer.removeSelectionPaths(ps map pathToTreePath toArray); this }
      def ++=(ps: Seq[Path[A]]) = { peer.addSelectionPaths(ps map pathToTreePath toArray); this }
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
    
    def mode = Tree.SelectionMode(peer.getSelectionModel.getSelectionMode)
    def mode_=(m: Tree.SelectionMode.Value) {peer.getSelectionModel.setSelectionMode(m.id)}
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
   * Expands every row. Will not terminate if the tree is of infinite depth.
   */
  def expandAll() {
    var i = 0
    while (i < rowCount) {
      expandRow(i)
      i += 1
    }
  } 
  
  def collapsePath(path: Path[A]) {peer collapsePath path}
  def collapseRow(row: Int) {peer collapseRow row}

  def treeData = treeDataModel
  def treeData_=(tm: TreeModel[A]) = {treeDataModel = tm; peer setModel tm.peer}
  
  override def cellValues: Iterator[A] = treeData.iterator
  
  /**
   * Collapses all visible rows.
   */
  def collapseAll() {0 until rowCount foreach collapseRow}
  
  def isExpanded(path: Path[A]) = peer isExpanded path
  def isCollapsed(path: Path[A]) = peer isCollapsed path
  
  def isEditing() = peer.isEditing()
  
  def editable: Boolean = peer.isEditable
  def editable_=(b: Boolean) {peer setEditable b}
  
  def editor: Editor[A] =  Editor.wrap(peer.getCellEditor)
  def editor_=(r: Tree.Editor[A]) { peer.setCellEditor(r.peer); editable = true }
  
  def renderer: Renderer[A] = Renderer.wrap(peer.getCellRenderer)
  def renderer_=(r: Tree.Renderer[A]) { peer.setCellRenderer(r.peer) }
  
  def showsRootHandles = peer.getShowsRootHandles
  def showsRootHandles(b:Boolean) {peer.setShowsRootHandles(b)}
  
  def startEditingAtPath(path: Path[A]) { peer.startEditingAtPath(pathToTreePath(path)) }

  def getClosestPathForLocation(x: Int, y: Int): Path[A] = peer.getClosestPathForLocation(x, y)
  def getClosestRowForLocation(x: Int, y: Int): Int = peer.getClosestRowForLocation(x, y)
  
  // Follows the naming convention of ListView.selectIndices()
  def selectRows(rows: Int*)  { peer.setSelectionRows(rows.toArray) }
  def selectPaths(paths: Path[A]*) { peer.setSelectionPaths(paths map pathToTreePath toArray) }
  def selectInterval(first: Int, last: Int) { peer.setSelectionInterval(first, last) }
  
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

