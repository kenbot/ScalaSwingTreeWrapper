package scala.swing

trait ItemView[+A] {
  this: Component =>
  def itemValues: Iterator[A]
}