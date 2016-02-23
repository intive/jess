package com.blstream.jess
package core

trait ChallengeService {
  def nextChallenge(level: Int): (Challenge, Answer) =
    (Challenge(
      title = "Multiples of 3 and 5",
      description =
      """If we list all the natural numbers below 10 that are multiples of 3 or 5, we get 3, 5, 6 and 9.
The sum of these multiples is 23.""",
      assigment = "Find the sum of all the multiples of 3 or 5 below 1000."
    ), "233168")
}
