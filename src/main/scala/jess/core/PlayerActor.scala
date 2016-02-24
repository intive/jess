package com.blstream.jess
package core

import akka.persistence.PersistentActor
import cats.syntax.validated._
import core.state.{ Challenge, NickValidator, PlayerLogic, PlayerState, StateTransitionError }

case class PlayerStats(attempts: Int, time: Long)

sealed trait PlayerEvents

case class StateModified(ps: PlayerState) extends PlayerEvents

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

  var state: PlayerState = initGame.runS(PlayerState(nick = None, chans = nextChallenge(0))).value

  override def persistenceId: String = "player-actor"

  def readyToPlay: Receive = {
    case sg @ PlayerLogic.StartGame(_) =>
      val foo = for {
        start <- startGame(sg)
      } yield {
        val (newState, ch) = start.run(state).value
        state = newState
        persist(
          StateModified(state)
        )(ev => startGame1)
        ch
      }
      sender ! foo

    case _ => sender ! StateTransitionError("Start game first").invalidNel

  }

  def playing: Receive = {
    case PlayerLogic.StartGame => sender ! "Game already started"
    case PlayerLogic.Answer(lnk, answer) =>
      val resp = if (currentAnswer == answer) {
        points = points + 1
        attempts = attempts + 1
        persist(
          StateModified(state)
        )(ev => resolveChallenge)
        CorrectAnswer
      } else {
        persist(StateModified(state))(
          ev => increaseAttempts
        )
        IncorrectAnswer
      }
      sender ! resp
    case PlayerLogic.Next(lnk) =>
      val chans = nextChallenge(1)
      currentAnswer = chans.answer
      challenge = chans.challenge
      sender ! challenge
    case PlayerLogic.Stats =>
      sender ! PlayerStats(2, 300)
    case PlayerLogic.Current =>
      sender ! link
  }

  def gameFinished: Receive = {
    case _ => sender ! "Game finished"
  }

  override def receiveCommand: Receive = readyToPlay
  override def receiveRecover: Receive = { case _ => }

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
