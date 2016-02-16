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
case class ChallengePrepared(challenge: Challenge) extends PlayerEvents
case class ChallengeSubmitted(answear: String) extends PlayerEvents
case object ChallengeResolved extends PlayerEvents

class PlayerActor extends PersistentActor {
  override def persistenceId: String = "player-actor"

  var points: Long = 0
  var attempts: Long = 0
  val challenges = GameService.getChallenges
  var currentChallenge: Int = 0

  def readyToPlay: Receive = {
    case PlayerActor.Start => {
      val challenge = challenges(currentChallenge)
      persist(Seq(
        GameStarted,
        ChallengePrepared(challenge)
      ))(ev => startGame)
      sender ! challenge.question
      context become playing
    }
    case PlayerActor.Challenge(_) => sender ! "Start game first"
  }

  def playing: Receive = {
    case PlayerActor.Start => sender ! "Game already started"
    case PlayerActor.Challenge(answear) => {
      if (answear == challenges(currentChallenge).answer) {
        if (currentChallenge < challenges.size - 1) {
          val challenge = challenges(currentChallenge + 1)
          persist(Seq(
            ChallengeSubmitted(answear),
            ChallengeResolved,
            ChallengePrepared(challenge)
          ))(ev => resolveChallenge)
          sender ! challenge.question
        } else {
          sender ! "Game finished"
          context become gameFinished
        }
      } else {
        persist(ChallengeSubmitted(answear))(ev => increaseAttempts)
        sender ! s"$answear is a wrong answear"
      }
    }
  }

  def gameFinished: Receive = {
    case PlayerActor.Start => sender ! "Game finished"
    case PlayerActor.Challenge(_) => sender ! "Game finished"
  }

  override def receiveCommand: Receive = readyToPlay

  override def receiveRecover: Receive = {
    case GameStarted => startGame
    case ChallengePrepared(challenge) =>
    case ChallengeSubmitted(answer) => increaseAttempts
    case ChallengeResolved => resolveChallenge
  }

  private def startGame = {
    points = 0
    context become playing
  }
  private def increaseAttempts = attempts += 1
  private def increasePoints = points += 100
  private def nextChallenge = currentChallenge += 1
  private def resolveChallenge = {
    increaseAttempts
    increasePoints
    nextChallenge
  }

}
