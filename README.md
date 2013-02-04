#Note: moved to scalaswingcontrib
This project going forward will be maintained as part of [scalaswingcontrib](https://github.com/benhutchison/ScalaSwingContrib) and not this repo.

You can include in your Scala 2.10 project it by adding this to your build.sbt:
 ```scala libraryDependencies += "com.github.benhutchison" % "scalaswingcontrib" % "1.4" ```

 or for 2.9: 
 ```scala libraryDependencies += "com.github.benhutchison" % "scalaswingcontrib" % "1.3" ```

-------------------------------------

This project provides a Scala wrapper for Java's JTree class.

Most uses of JTree are vastly simpler -- all you need to provide is the root nodes, and a function that provides the children.

### A small, fixed tree 

```scala
case class Node[A](value: A, children: Node[A]*)
val menuItems = Node("Hobbies", Node("Skateboarding"), //... etc
      
new Tree[Node] {
  model = TreeModel(menuItems)(_.children)
  renderer = Tree.Renderer(_.value)
}
```

### An XML document

```scala
val xml: scala.xml.Node = //...
new Tree(TreeModel(Seq(xml)) {_.child.filter(_.text.trim.nonEmpty)})
```

### The file system

```scala
new Tree[File] {
  model = TreeModel(new File(".")) {f => 
    if (f.isDirectory) f.listFiles.toSeq else Seq()
  }
}
```

### Infinitely deep structure

```scala
// All factors of 1000, and the factors' factors, etc
new Tree(TreeModel(1000) {n => 1 to n filter (n % _ == 0)})
```

### A diverse object graph

With a custom renderer - they are a piece of cake too.

```scala
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
```


### Add, edit and remove nodes on an internal model, copied from the original data.

```scala
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
```

### Add, edit and remove nodes on an external model, consisting of original data.

```scala
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
```
