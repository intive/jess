package com.blstream.jess
package core

case class Challenge(question: String, answer: String)

object GameService {
  private val challanges = List(
    Challenge("What is the meaning of life?", "42"),
    Challenge("What is the best editor", "emacs")
  )

  def getChallanges = challanges
}
