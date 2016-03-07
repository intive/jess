package com.blstream.jess
package core.state

import core.{ LinkGenerator, ChallengeService }
import org.scalatest.FunSuite
import cats.scalatest.XorMatchers
import org.scalatest.Matchers._
import cats.syntax.xor._

class PlayerLogicSpec
    extends FunSuite
    with XorMatchers
    with PlayerLogic
    with ChallengeService
    with LinkGenerator
    with NickValidator {

  val link = "abc123"

  val ps = Some(PlayerState(
    "luke",
    points = 0,
    attempts = 0,
    current = link,
    challenges = Map(link -> ChallengeWithAnswer("title", "desc", "question", level = 0, Some(link), "Answer"))
  ))

  test("update points") {
    val (newState, challenge) = updatePoints.run(ps).value

    newState should not be (None)
    newState.get should have('points(10))
    challenge should be(right)

  }

  test("check answer which is correct") {
    val (newState, resp) = checkAnswer(PlayerLogic.Answer(link, "Answer")).run(ps).value

    newState should ===(ps)
    resp should be(right)
  }

  test("check answer which is incorrect") {
    val (newState, resp) = checkAnswer(PlayerLogic.Answer(link, "IncorrectAnswer")).run(ps).value

    newState should ===(ps)
    resp should be(left)
  }

  test("increment attempt") {
    val (newState, resp) = incrementAttempts.run(ps).value

    newState should not be None
    newState.get should have('attempts(1))
    resp should be(right)
    resp should ===(1.right)
  }

  test("new challenge") {
    val (newState, challenge) = newChallenge.run(ps).value

    challenge should be(right)
    newState.get.current !== ps.get.current
    newState.get.challenges.size === 2
  }

  test("answer challenge") {
    val ans = PlayerLogic.Answer(link, "Answer")
    val (newState, challenge) = answerChallenge(ans).run(ps).value

    challenge should be(right)
    newState should not be None
    newState.get should have('attempts(1))
    newState.get should have('points(10))
    newState.get.current !== ps.get.current
    newState.get.challenges.size === 2
  }
}