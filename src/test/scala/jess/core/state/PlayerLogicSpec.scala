package com.blstream.jess
package core.state

import com.blstream.jess.core.{ LinkGenerator, ChallengeService }
import org.scalatest.FunSuite
import cats.data.State
import cats.scalatest.XorMatchers
import org.scalatest.Matchers._

class PlayerLogicSpec
    extends FunSuite
    with XorMatchers
    with PlayerLogic
    with ChallengeService
    with LinkGenerator
    with NickValidator {

  val ps = PlayerState(
    None,
    points = 0,
    attempts = 0,
    challenge = Challenge("title", "desc", "question", 0, "Answer", Some("abc123"))
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

  test("increment attempt") {
    val (newState, resp) = incrementAttempts.run(ps).value

    newState should have('attempts(1))
    resp should ===(1)

  }

  test("update challenge") {
    val (newState, challenge) = newChallenge.run(ps).value

    challenge should be(right)
  }
}
