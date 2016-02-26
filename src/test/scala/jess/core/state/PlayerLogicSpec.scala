package com.blstream.jess
package core.state

import core.ChallengeService
import org.scalatest.FunSuite
import cats.data.State
import cats.scalatest.XorMatchers
import org.scalatest.Matchers._

class PlayerLogicSpec
    extends FunSuite
    with XorMatchers
    with PlayerLogic
    with ChallengeService
    with NickValidator {

  val ps = PlayerState(
    None,
    points = 0,
    attempts = 0,
    chans = ChallengeWithAnswer(0, Challenge("title", "desc", "question"), "Answer")
  )

  test("update points") {
    val (newState, challenge) = updatePoints.run(ps).value

    newState should have('points(10))
    challenge should be(right)

  }

  test("check answer which is correct") {
    val (newState, resp) = checkAnswer(PlayerLogic.Answer("link", "Answer")).run(ps).value

    newState should ===(ps)
    resp should be(right)

  }
  test("check answer which is incorrect") {
    val (newState, resp) = checkAnswer(PlayerLogic.Answer("link", "IncorrectAnswer")).run(ps).value

    newState should ===(ps)
    resp should be(left)

  }

}
