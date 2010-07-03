package au.ken.treeview

import scala.swing.Component

trait Cells[A] {
  this: Component =>
  
  val companion: CellsCompanion

  def editable: Boolean 
  //def cellValues: Iterator[A]
  def renderer: companion.Renderer[A]
  def editor: companion.Editor[A]
}