package com.blstream.jess
package core

import akka.persistence.PersistentActor
import core.state.{ Challenge, PlayerState, NickValidator, PlayerLogic }

object PlayerActor {

  sealed trait PlayerMessages

  case class Next(link: JessLink) extends PlayerMessages

  case class Answer(link: JessLink, answer: String) extends PlayerMessages

  case object Start extends PlayerMessages

  case object Current extends PlayerMessages

  case object Stats extends PlayerMessages

}

sealed trait PlayerEvents

case object GameStarted extends PlayerEvents

case class ChallengePrepared(challenge: Challenge) extends PlayerEvents

case class ChallengeSubmitted(answer: String) extends PlayerEvents

case object ChallengeResolved extends PlayerEvents

case class PlayerStats(attempts: Int, time: Long) extends PlayerEvents

class PlayerActor
    extends PersistentActor
    with ChallengeService
    with LinkService
    with PlayerLogic
    with NickValidator {
  var points: Long = 0
  var currentAnswer: String = _
  var challenge: Challenge = _
  var attempts: Int = 0
  var link: JessLink = _

  val state: PlayerState = initGame.runS(PlayerState(nick = None, chans = nextChallenge(0))).value

  override def persistenceId: String = "player-actor"

  def readyToPlay: Receive = {
    case PlayerActor.Start =>
      val chans = nextChallenge(1)
      challenge = chans.challenge
      currentAnswer = chans.answer
      link = genLink

      persist(Seq(
        GameStarted,
        ChallengePrepared(challenge)
      ))(ev => startGame1)
      sender ! (challenge, link)
      context become playing
    case _ => sender ! "Start game first"

  }

  def playing: Receive = {
    case PlayerActor.Start => sender ! "Game already started"
    case PlayerActor.Answer(lnk, answer) =>
      val resp = if (currentAnswer == answer) {
        points = points + 1
        attempts = attempts + 1
        persist(Seq(
          ChallengeSubmitted(answer),
          ChallengeResolved,
          ChallengePrepared(challenge)
        ))(ev => resolveChallenge)
        CorrectAnswer
      } else {
        persist(ChallengeSubmitted(answer))(
          ev => increaseAttempts
        )
        IncorrectAnswer
      }
      sender ! resp
    case PlayerActor.Next(lnk) =>
      val chans = nextChallenge(1)
      currentAnswer = chans.answer
      challenge = chans.challenge
      sender ! challenge
    case PlayerActor.Stats =>
      sender ! PlayerStats(2, 300)
    case PlayerActor.Current =>
      sender ! link
  }

  def gameFinished: Receive = {
    case _ => sender ! "Game finished"
  }

  override def receiveCommand: Receive = readyToPlay

  override def receiveRecover: Receive = {
    case GameStarted => startGame1
    case ChallengePrepared(challenge) =>
    case ChallengeSubmitted(answer) => increaseAttempts
    case ChallengeResolved => resolveChallenge
  }

  private def startGame1 = {
    points = 0
    context become playing
  }

  private def increaseAttempts = attempts += 1

  private def increasePoints = points += 100

  private def resolveChallenge = {
    increaseAttempts
    increasePoints
    challenge
  }

}
