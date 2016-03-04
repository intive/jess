package com.blstream.jess
package core

import java.util.UUID

import state.Challenge

trait LinkGenerator {
  def nextLink = UUID.randomUUID().toString.replaceAll("-", "")
}

trait ChallengeService {
  linkGen: LinkGenerator =>

  private val challenges =
    Vector(
      Challenge(
        level = 0,
        title = "The most important question",
        description = "This is a question about live",
        assignment = "What is the sense of live",
        answer = "42",
        link = None
      ),
      Challenge(
        level = 1,
        title = "Multiply level number",
        description = "Level is complicated number, try multiplying it",
        assignment = "Multiply level (2) by factor 2",
        answer = "4",
        link = None
      ),
      Challenge(
        level = 2,
        title = "Multiples of 3 and 5",
        description = """If we list all the natural numbers below 10 that are multiples of 3 or 5, we get 3, 5, 6 and 9. The sum of these multiples is 23.""",
        assignment = "Find the sum of all the multiples of 3 or 5 below 1000.",
        answer = "233168",
        link = None
      )
    )

  def nextChallenge(level: Int): Option[Challenge] = challenges.lift(level).map(_.copy(link = Some(nextLink)))

}
