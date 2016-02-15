package com.blstream.jess
package core

import akka.persistence.PersistentActor

object PlayerActor {
  sealed trait PlayerMessages
  case object Start extends PlayerMessages
  case class Challenge(answer: String) extends PlayerMessages
}

sealed trait PlayerEvents
case object GameStarted extends PlayerEvents
case class ChallengeGenerated(link: JessLink) extends PlayerEvents
case class ChallengeSubmitted(answear: String) extends PlayerEvents
case object ChallengeResolved extends PlayerEvents

class PlayerActor extends PersistentActor with GameService {
  override def persistenceId: String = "player-actor"

  var started = false
  var points: Long = 0
  var attempts: Long = 0

  override def receiveCommand: Receive = {
    case PlayerActor.Start => {
      if (!started) {
        val jessLink = generateJessLink
        persist(Seq(
          GameStarted,
          ChallengeGenerated(jessLink)
        ))(ev => startGame)
        sender ! jessLink
      } else {
        sender ! "Game already started"
      }
    }

    case PlayerActor.Challenge(answear) => {
      if (answear == "42") {
        val jessLink = generateJessLink
        persist(Seq(
          ChallengeSubmitted(answear),
          ChallengeResolved,
          ChallengeGenerated(jessLink)
        ))(ev => resolveChallenge)
        sender ! jessLink
      } else {
        persist(ChallengeSubmitted(answear))(ev => increaseAttempts)
        sender ! "Challange failed"
      }
    }
  }

  override def receiveRecover: Receive = {
    case GameStarted => startGame
    case ChallengeGenerated(link) =>
    case ChallengeSubmitted(answer) => increaseAttempts
    case ChallengeResolved => increasePoints
  }

  private def startGame = {
    points = 0
    started = true
  }
  private def increaseAttempts = attempts += 1
  private def increasePoints = points += 100
  private def resolveChallenge = {
    increaseAttempts
    increasePoints
  }

}
