package au.ken.treeview

import scala.swing.Component

trait CellView[A] {
  this: Component =>
  def editable: Boolean 
  def cellValues: Iterator[A]
}

trait RenderableCells[A] {
  this: CellView[A] =>
  val companion: RenderableCellsCompanion
  def renderer: companion.Renderer[A]
}

trait EditableCells[A]  {
  this: CellView[A] =>
  val companion: EditableCellsCompanion
  def editor: companion.Editor[A]
}