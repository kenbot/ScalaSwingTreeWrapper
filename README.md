# Scala Swing Tree Wrapper 

Ken Scambler

This project provides a Scala wrapper for Java's JTree class, and aims to be suitable for eventual inclusion in the official Scala Swing library.
The design was initiated in the Scala debate forum in February 2010.

Most uses of JTree are vastly simpler -- all you need to provide is the root nodes, and a function that provides the children.

### A small, fixed tree 

<pre>
<code>
case class Node[A](value: A, children: Node[A]*)
val menuItems = Node("Hobbies", Node("Skateboarding"), //... etc
      
new Tree[Node] {
  model = TreeModel(menuItems)(_.children)
  renderer = Tree.Renderer(_.value)
}
</code>
</pre>

### An XML document

<pre>
<code>
val xml: scala.xml.Node = //...
new Tree(TreeModel(Seq(xml)) {_.child.filter(_.text.trim.nonEmpty)})
</code>
</pre>

### The file system

<pre>
<code>
new Tree[File] {
  model = TreeModel(new File(".")) {f => 
    if (f.isDirectory) f.listFiles.toSeq else Seq()
  }
}
</code>
</pre>

### Infinitely deep structure

<pre>
<code>
// All factors of 1000, and the factors' factors, etc
new Tree(TreeModel(1000) {n => 1 to n filter (n % _ == 0)})
</code>
</pre>

### A diverse object graph

With a custom renderer - they are a piece of cake too.

<pre>
<code>
case class Customer(id: Int, title: String, firstName: String, lastName: String)
case class Product(id: String, name: String, price: Double)
case class Order(id: Int, customer: Customer, product: Product, quantity: Int)
val orders: List[Order] = //...

new Tree[Any] {
  model = TreeModel[Any](orders: _*) {
    case Order(_, cust, prod, qty) => Seq(cust, prod, "Qty" -> qty)
    case Product(id, name, price) => Seq("ID" -> id, "Name" -> name, "Price" -> ("$" + price))
    case Customer(id, _, first, last) => Seq("ID" -> id, "First name" -> first, "Last name" -> last)
    case _ => Seq.empty
  }

  renderer = Tree.Renderer({
    case Order(id, _, _, qty) => "Order #" + id + " x " + qty
    case Product(id, _, _) => "Product " + id
    case Customer(_, title, first, last) => title + " " + first + " " + last
    case (field, value) => field + ": " + value
    case x => x.toString
  })
}
</code>
</pre>


### Add, edit and remove nodes on an internal model, copied from the original data.

<pre>
<code>
val child = Node("child")
val parent = Node("parent", child)
val root = Node("root", parent)

val tree = new Tree[Node] {
  model = InternalTreeModel(root)(_.children)
}

// Doesn't affect the original data, because the model is internally represented.
var child2 = Node("child2")
tree.model(Path(root, parent, child)) = child2
tree.model.remove(Path(root, parent, child2))
tree.model.insertUnder(Path(root, parent), child, 0)
tree.model.insertBefore(Path(root, parent, child), Node("before-child"))
tree.model.insertAfter(Path(root, parent, child), Node("after-child"))
</code>
</pre>

### Add, edit and remove nodes on an external model, consisting of original data.

<pre>
<code>
val child = Node("child")
val parent = Node("parent", child)
val root = Node("root", parent)

val tree = new Tree[Node] {
  model = ExternalTreeModel(root)(_.children).makeUpdatableWith {
    (pathOfFile, updatedFile) => 
      pathOfFile.last.name = updatedFile.name; pathOfFile.last
  }.makeInsertableWith {
    (parentPath, fileToInsert, index) => 
      parentPath.last.insertChild(index, fileToInsert); true
  }.makeRemovableWith {
    (pathToRemove) => 
      pathToRemove.delete(); true
  }
}

// The original data will be mutated by the given functions, because the model is externally represented.
var child2 = Node("child2")
tree.model(Path(root, parent, child)) = child2
tree.model.remove(Path(root, parent, child2))
tree.model.insertUnder(Path(root, parent), child, 0)
tree.model.insertBefore(Path(root, parent, child), Node("before-child"))
tree.model.insertAfter(Path(root, parent, child), Node("after-child"))
</code>
</pre>
