package au.ken.treeview

import au.ken.treeview.event._
import scala.swing._
import javax.swing.{CellEditor => JCellEditor, AbstractCellEditor => JAbstractCellEditor}
import javax.swing.event.{CellEditorListener, ChangeEvent}

trait CellsCompanion {
  type Editor[A] <: CellEditor[A]
  type Renderer[A] <: CellRenderer[A]
  protected type Owner[A] <: Component with Cells[A]
  
  val Editor: CellEditorCompanion
  val Renderer: CellRendererCompanion

  trait CellRendererCompanion {
    type Peer // eg. javax.swing.table.TableCellRenderer, javax.swing.tree.TreeCellRenderer
    type Params
    val emptyParams: Params
    def wrap[A](r: Peer): Renderer[A]
    def apply[A,B](f: A => B)(implicit renderer: Renderer[B]): Renderer[A]
  }

  trait CellRenderer[-A] extends Publisher  { 
    val companion: CellRendererCompanion
    def peer: companion.Peer
    def componentFor(owner: Owner[_], value: A, params: companion.Params = companion.emptyParams): Component
  }

  trait CellEditorCompanion {
    type Peer <: JCellEditor
    type Params
    val emptyParams: Params
    def apply[A, B](toB: A => B, toA: B => A)(implicit editor: Editor[B]): Editor[A]
  }
  
  trait CellEditor[A] extends Publisher with au.ken.treeview.CellEditor[A] {
    val companion: CellEditorCompanion
    def peer: companion.Peer

    protected def addPeerListeners(p: JCellEditor) {
      p.addCellEditorListener(new CellEditorListener {
        override def editingCanceled(e: ChangeEvent) = publish(CellEditingStopped(CellEditor.this))
        override def editingStopped(e: ChangeEvent) = publish(CellEditingCancelled(CellEditor.this))
      })
    }
    
    trait DefaultPeer extends JAbstractCellEditor {
      def getCellEditorValue(): AnyRef = value.asInstanceOf[AnyRef]
      addPeerListeners(this)
    }

    def componentFor(owner: Owner[_], value: A, params: companion.Params = companion.emptyParams): Component
    
    def cellEditable = peer.isCellEditable(null)
    def shouldSelectCell = peer.shouldSelectCell(null)
    def cancelCellEditing() = peer.cancelCellEditing
    def stopCellEditing() = peer.stopCellEditing
  }
}

