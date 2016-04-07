package com.blstream
package jess.core

import cats.data.Xor
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
    with StartGameValidator {

  val link = "abc123"

  val ps = PlayerState(
    "luke",
    points = 0,
    attempts = 0,
    current = link,
    challenges = Map(link -> ChallengeWithAnswer("title", "desc", "question", level = 0, Some(link), 10, "Answer"))
  )

  test("update points") {
    val (newState, challenge) = updatePoints.run(ps).value

    newState should have('points(10))
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

    newState should have('attempts(1))
    resp should be(right)
    resp should ===(1.right)
  }

  test("new challenge") {
    val (newState, challenge) = newChallenge.run(ps).value

    challenge should be(right)
    newState.current !== ps.current
    newState.challenges.size === 2
  }

  test("answer challenge") {
    val ans = PlayerLogic.Answer(link, "Answer")
    val (newState, challenge) = answerChallenge(ans).run(ps).value

    challenge should be(right)
    newState should have('attempts(1))
    newState should have('points(10))
    newState.current !== ps.current
    newState.challenges.size === 2
  }

  test("wrong answer challenge") {
    val ans = PlayerLogic.Answer(link, "BadAnswer")
    val (newState, challenge) = answerChallenge(ans).run(ps).value

    challenge should be(Xor.Left(IncorrectAnswer))
    newState should have('attempts(1))
    newState should have('points(0))
    newState.current === ps.current
    newState.challenges.size === 1
  }

  test("answer challenge twice") {
    val ans = PlayerLogic.Answer(link, "Answer")
    val (newState, challenge) = answerChallenge(ans).run(ps).value
    val (newnewState, newChallenge) = answerChallenge(ans).run(newState).value

    newChallenge should be(left)
    newnewState should have('attempts(2))
    newnewState should have('points(10))
    newnewState.current !== ps.current
    newnewState.challenges.size === 2
  }
}
