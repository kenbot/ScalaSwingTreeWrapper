package scala.swing
package test

import scala.swing.event._
import scala.swing.tree._
import Swing._
import java.net.URL
import GridPanel.Adapt
import java.io.IOException
import Tree._

object TreeTutorialDemo extends SimpleSwingApplication {
  def top = new MainFrame {
    title = "Scala Tree Tutorial Demo"
    contents = new TreeTutorialDemo
  }
}

class TreeTutorialDemo extends GridPanel(1, Adapt) {

  case class BookInfo(book: String, filename: String) {
    val bookURL = getURL(filename)
  }

  val data = "The Scala Series" -> List(
    "Books for Scala Programmers" -> List(
      BookInfo("Programming in Scala", "odersky.html"),
      BookInfo("Programming Scala (Payne/Wampler)", "payne.html"),
      BookInfo("Programming Scala (Subramaniam)", "subramaniam.html"),
      BookInfo("Beginning Scala", "pollak.html"),
      BookInfo("Scala", "braun.html"),
      BookInfo("Steps in Scala", "loverdos.html")
    ),
    "Books for Lift Developers" -> List(
      BookInfo("The Definitive Guide to Lift", "chenbecker.html"),
      BookInfo("Lift In Action", "perrett.html")
    )
  )

  val tree = new Tree[Any] {
    model = TreeModel[Any](data)(_ match {
      case (_, list: List[Any]) => list
      case _ => Nil
    })

    renderer = Renderer(_ match {
      case (category, _) => category
      case BookInfo(title, _) => title
    })
    expandAll
    selection.mode = Tree.SelectionMode.Single
  }
  
  // Listen to tree selection
  listenTo(tree.selection)
  reactions += {
    case TreeNodeSelected(node) => displayURL(node match {
      case b: BookInfo => b.bookURL
      case _ => helpURL
    })
  }
  
      
  val htmlPane: EditorPane = new EditorPane {
    editable = false
  }

  
  def displayURL(url: URL) {
    try {
      if (url != null)
        htmlPane.peer.setPage(url)
      else
        htmlPane.text = "File Not Found"
    } 
    catch {
      case e: IOException => println("Attempted to read a bad URL: " + url)
    }
  }

  val helpURL = getURL("TreeDemoHelp.html")
  displayURL(helpURL)
  
  private def getURL(s: String) = getClass.getResource("/scala/swing/test/" + s).ensuring(
      _ != null, "Couldn't find file: " + s).toURI.toURL 

  // Layout components   
  contents += new SplitPane {
    topComponent = new ScrollPane(tree) {
      minimumSize = (100,50): Dimension
    }
    bottomComponent = new ScrollPane(htmlPane) {
      minimumSize = (100,50): Dimension
    }
    dividerLocation = 100
    preferredSize = (500,300): Dimension
  }

}

