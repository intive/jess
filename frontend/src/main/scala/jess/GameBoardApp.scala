package com.blstream.jess

import scala.scalajs.js
import org.scalajs.dom
import dom.document
import js.annotation.JSExport
import js.Dynamic.global

@JSExport
object GameBoard extends js.JSApp {

  @JSExport
  def main(): Unit = {
    appendPar(document.body, "Jess Game Board")
  }

  def appendPar(targetNode: dom.Node, text: String): Unit = {
    val parNode = document.createElement("p")
    val textNode = document.createTextNode(text)
    parNode.appendChild(textNode)
    targetNode.appendChild(parNode)
  }

}

