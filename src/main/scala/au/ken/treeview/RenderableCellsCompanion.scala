package au.ken.treeview

import au.ken.treeview.event._
import scala.swing._


trait RenderableCellsCompanion {
  type Renderer[A] <: CellRenderer[A]
  protected type Owner <: Component with CellView[_]
  
  val Renderer: CellRendererCompanion

  trait CellRendererCompanion {
    type Peer // eg. javax.swing.table.TableCellRenderer, javax.swing.tree.TreeCellRenderer
    type Params
    val emptyParams: Params
    def wrap[A](r: Peer): Renderer[A]
    def apply[A, B: Renderer](f: A => B): Renderer[A]
  }

  trait CellRenderer[-A] extends Publisher  { 
    val companion: CellRendererCompanion
    def peer: companion.Peer
    def componentFor(owner: Owner, value: A, params: companion.Params = companion.emptyParams): Component
  }
}

