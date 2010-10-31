package scala.swing

trait CellEditor[A] extends Publisher {
  def peer: AnyRef
  def value: A
  def cellEditable: Boolean
  def shouldSelectCell: Boolean
  def cancelCellEditing(): Unit
  def stopCellEditing(): Boolean
}


