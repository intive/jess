package com.blstream.jess
package core.state

import core.{ LinkGenerator, ChallengeService }
import org.scalatest.FunSuite
import cats.scalatest.XorMatchers
import org.scalatest.Matchers._

class PlayerLogicSpec
    extends FunSuite
    with XorMatchers
    with PlayerLogic
    with ChallengeService
    with LinkGenerator
    with NickValidator {

  val link = "abc123"

  val ps = PlayerState(
    None,
    points = 0,
    attempts = 0,
    current = link,
    challenges = Map(link -> Challenge("title", "desc", "question", level = 0, "Answer", Some(link)))
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
    resp should ===(1)
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
}
