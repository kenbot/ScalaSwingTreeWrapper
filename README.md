Scala Swing Tree Wrapper Design
by Ken Scambler

This project provides a Scala wrapper for Java's JTree class, and aims to be suitable for eventual inclusion in the official Scala Swing library.
The design was [[initiated|http://scala-programming-language.1934581.n4.nabble.com/scala-swing-JTree-wrapper-design-td2004809.html#a2004809]] in the Scala debate forum in February 2010.

Most uses of JTree are vastly simpler -- all you need to provide is the root nodes, and a function that provides the children.

h3. A small, fixed tree 

```scala
case class Node[A](value: A, children: Node[A]*)
val menuItems = Node("Hobbies", Node("Skateboarding"), //... etc
      
new Tree[TreeNode[String]] {
  treeData = TreeModel(menuItems)(_.children)
  renderer = Tree.Renderer(_.value)
}
```

h3. An XML document

```scala
val xml: scala.xml.Node = //...
new Tree[Node](Seq(xml), _.child filter (_.text.trim.nonEmpty))
```

h3. The file system

```scala
new Tree[File] {
  treeData = TreeModel(new File(".")) {f => 
    if (f.isDirectory) f.listFiles.toSeq else Seq()
  }
}
```

h3. Infinitely deep structure

```scala
// All factors of 1000, and the factors' factors, etc
new Tree(TreeModel(1000) {n => 1 to n filter (n % _ == 0)})
```

h3. A diverse object graph

With a custom renderer - they are a piece of cake too.

```scala
case class Customer(id: Int, title: String, firstName: String, lastName: String)
case class Product(id: String, name: String, price: Double)
case class Order(id: Int, customer: Customer, product: Product, quantity: Int)
val orders: List[Order] = //...

new Tree[Any] {
  treeData = TreeModel[Any](orders: _*)({
    case Order(_, cust, prod, qty) => Seq(cust, prod, "Qty" -> qty)
    case Product(id, name, price) => Seq("ID" -> id, "Name" -> name, "Price" -> ("$" + price))
    case Customer(id, _, first, last) => Seq("ID" -> id, "First name" -> first, "Last name" -> last)
    case _ => Seq.empty
  })

  renderer = Tree.Renderer({
    case Order(id, _, _, qty) => "Order #" + id + " x " + qty
    case Product(id, _, _) => "Product " + id
    case Customer(_, title, first, last) => title + " " + first + " " + last
    case (field, value) => field + ": " + value
    case x => x.toString
  })
}
```