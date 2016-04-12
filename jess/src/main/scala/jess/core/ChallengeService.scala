package com.blstream.jess
package core

import java.util.UUID

import cats.data.Xor
import cats.syntax.xor._

trait LinkGenerator {
  def nextLink = UUID.randomUUID().toString.replaceAll("-", "")
}

sealed trait ChallengeServiceResponse
case class NextChallenge(solved: Option[ChallengeWithAnswer], nextChallenge: Option[ChallengeWithAnswer]) extends ChallengeServiceResponse

trait ChallengeService {
  linkGen: LinkGenerator =>

  private var challenges =
    Vector(
      ChallengeWithAnswer(
        level = 0,
        title = "The most important question",
        description = "This is a question about live",
        assignment = "What is the sense of live",
        answer = "42",
        link = None,
        challengePoints = 10
      ),
      ChallengeWithAnswer(
        level = 1,
        title = "Multiply level number",
        description = "Level is complicated number, try multiplying it",
        assignment = "Multiply level (2) by factor 2",
        answer = "4",
        link = None,
        challengePoints = 15
      ),
      ChallengeWithAnswer(
        level = 2,
        title = "Multiples of 3 and 5",
        description = """If we list all the natural numbers below 10 that are multiples of 3 or 5, we get 3, 5, 6 and 9. The sum of these multiples is 23.""",
        assignment = "Find the sum of all the multiples of 3 or 5 below 1000.",
        answer = "233168",
        link = None,
        challengePoints = 20
      )
    )

  def nextChallenge(level: Int): NoChallengesError.type Xor ChallengeServiceResponse =
    if (challenges.isEmpty) NoChallengesError.left
    else {
      val solved = challenges.lift(level - 1).map(_.copy(link = Some(nextLink)))
      val next = challenges.lift(level).map(_.copy(link = Some(nextLink)))

      NextChallenge(solved, next).right
    }

  def addChallenge(chans: ChallengeWithAnswer) = challenges = challenges :+ chans
}
