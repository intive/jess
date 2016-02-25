package com.blstream.jess
package core

import core.state.{ Challenge, ChallengeWithAnswer }

trait ChallengeService {
  def nextChallenge(level: Int): ChallengeWithAnswer = ChallengeWithAnswer(
    level = level,
    Challenge(
      title = "Multiples of 3 and 5",
      description =
      """If we list all the natural numbers below 10 that are multiples of 3 or 5, we get 3, 5, 6 and 9.
The sum of these multiples is 23.""",
      assignment = "Find the sum of all the multiples of 3 or 5 below 1000."
    ), "233168"
  )
}
