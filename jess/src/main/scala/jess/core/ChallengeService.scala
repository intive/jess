package com.blstream.jess
package core

import java.util.UUID

import akka.persistence.PersistentActor
import akka.pattern.ask
import akka.util.Timeout
import cats.data.Xor
import cats.syntax.xor._
import com.blstream.jess.core.state.{ NoChallengesError, ChallengeWithAnswer }
import jess.core.AddChallenge

import scala.concurrent.Await
import scala.concurrent.duration._

sealed trait ChallengeServiceResponse
case class NextChallenge(challenge: ChallengeWithAnswer) extends ChallengeServiceResponse
case object LastChallengeSolved extends ChallengeServiceResponse

trait ChallengeService {
  private implicit val timeout = Timeout(5 second)

  def nextChallenge(level: Int): NoChallengesError.type Xor ChallengeServiceResponse = {
    val challengeF = ask(Main.challengeActorRef, NextChallengeCommand(level)).mapTo[NoChallengesError.type Xor ChallengeServiceResponse]
    Await.result(challengeF, 1 second)
  }
}

sealed trait ChallengeCommand
case class NextChallengeCommand(level: Int) extends ChallengeCommand

sealed trait ChallengeEvent
case class ChallengeAdded(chans: ChallengeWithAnswer) extends ChallengeEvent

class ChallengeActor extends PersistentActor {
  override def persistenceId: String = "challenge-actor"

  def nextLink = UUID.randomUUID().toString.replaceAll("-", "")

  private var challenges =
    Vector(
      ChallengeWithAnswer(
        level = 0,
        title = "The most important question",
        description = "This is a question about live",
        assignment = "What is the sense of live",
        answer = "42",
        link = None
      ),
      ChallengeWithAnswer(
        level = 1,
        title = "Multiply level number",
        description = "Level is complicated number, try multiplying it",
        assignment = "Multiply level (2) by factor 2",
        answer = "4",
        link = None
      ),
      ChallengeWithAnswer(
        level = 2,
        title = "Multiples of 3 and 5",
        description = """If we list all the natural numbers below 10 that are multiples of 3 or 5, we get 3, 5, 6 and 9. The sum of these multiples is 23.""",
        assignment = "Find the sum of all the multiples of 3 or 5 below 1000.",
        answer = "233168",
        link = None
      )
    )

  override def receiveCommand: Receive = {
    case NextChallengeCommand(level) => {
      val resp = if (challenges.isEmpty) NoChallengesError.left
      else if (level >= challenges.size) LastChallengeSolved.right
      else {
        val ch = challenges(level)
        NextChallenge(ChallengeWithAnswer(ch.title, ch.description, ch.assignment, ch.level, link = Some(nextLink), ch.answer)).right
      }
      sender ! resp
    }
    case AddChallenge(chans) => {
      persist(ChallengeAdded(chans))(ev => {
        challenges = challenges :+ chans
        sender ! chans
      })
    }
  }

  override def receiveRecover: Receive = {
    case ChallengeAdded(chans) => challenges = challenges :+ chans
  }

}