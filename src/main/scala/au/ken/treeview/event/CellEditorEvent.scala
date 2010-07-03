package au.ken.treeview.event

import au.ken.treeview._
import scala.swing.event._

import javax.swing.event.CellEditorListener

trait CellEditorEvent[A] extends Event {
  val source: CellEditor[A]
}
case class CellEditingStopped[A](source: CellEditor[A]) extends CellEditorEvent[A]
case class CellEditingCancelled[A](source: CellEditor[A]) extends CellEditorEvent[A]
