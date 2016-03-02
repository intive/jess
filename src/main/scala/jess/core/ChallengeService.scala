package com.blstream.jess
package core

import core.state.{ Challenge, ChallengeWithAnswer }

trait ChallengeService {

  private val challenges =
    Vector(
      ChallengeWithAnswer(
        level = 0,
        Challenge(
          title = "Multiples of 3 and 5",
          description = """If we list all the natural numbers below 10 that are multiples of 3 or 5, we get 3, 5, 6 and 9. The sum of these multiples is 23.""",
          assignment = "Find the sum of all the multiples of 3 or 5 below 1000."
        ), answer = "233168"
      ),
      ChallengeWithAnswer(
        level = 1,
        Challenge(
          title = "Multiply level number",
          description = "Level is complicated number, try multiplying it",
          assignment = "Multiply level (2) by factor 2"
        ), answer = "4"
      ),
      ChallengeWithAnswer(
        level = 2,
        Challenge(
          title = "The most important question",
          description = "This is a question about live",
          assignment = "What is the sense of live"
        ), answer = "42"
      )
    )

  def nextChallenge(level: Int): ChallengeWithAnswer = challenges(level)

}
