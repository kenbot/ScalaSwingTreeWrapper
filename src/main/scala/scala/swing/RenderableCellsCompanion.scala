package scala.swing

import scala.swing.event._


trait RenderableCellsCompanion {
  type Renderer[A] <: CellRenderer[A]
  protected type Owner <: Component with CellView[_]
  
  val Renderer: CellRendererCompanion

  trait CellRendererCompanion {
    type Peer // eg. javax.swing.table.TableCellRenderer, javax.swing.tree.TreeCellRenderer
    type CellInfo
    val emptyCellInfo: CellInfo
    def wrap[A](r: Peer): Renderer[A]
    def apply[A, B: Renderer](f: A => B): Renderer[A]
  }

  trait CellRenderer[-A] extends Publisher  { 
    val companion: CellRendererCompanion
    def peer: companion.Peer
    def componentFor(owner: Owner, value: A, cellInfo: companion.CellInfo = companion.emptyCellInfo): Component
  }
}

