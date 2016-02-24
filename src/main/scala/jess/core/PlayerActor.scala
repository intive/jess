package com.blstream.jess
package core

import akka.persistence.PersistentActor
import cats.syntax.validated._
import core.state.{ Challenge, NickValidator, PlayerLogic, PlayerState, StateTransitionError }
import cats.syntax.xor._

case class PlayerStats(attempts: Int, time: Long)

sealed trait PlayerEvents

case class StateModified(ps: PlayerState) extends PlayerEvents

class PlayerActor
    extends PersistentActor
    with ChallengeService
    with LinkService
    with PlayerLogic
    with NickValidator {

  var state: PlayerState = initGame.runS(PlayerState(nick = None, chans = nextChallenge(0))).value

  override def persistenceId: String = "player-actor"

  def readyToPlay: Receive = {
    case sg @ PlayerLogic.StartGame(_) =>
      val foo = for {
        start <- startGame(sg)
      } yield {
        val (newState, ch) = start.run(state).value
        persist(
          StateModified(newState)
        )(ev => {
            state = newState
            context become playing
          })
        ch
      }
      sender ! foo

    case _ => sender ! StateTransitionError("Start game first").left

  }

  def playing: Receive = {
    case PlayerLogic.StartGame(_) => sender ! StateTransitionError("Game already started").left
    case ans @ PlayerLogic.Answer(_, _) =>
      val foo = for {
        answer <- answerChallenge(ans)
      } yield {
        val (newState, ch) = answer.run(state).value
        persist(
          StateModified(state)
        )(ev => {
            state = newState
          })
      }
      sender ! foo
    // case PlayerLogic.Next(lnk) =>
    //   val chans = nextChallenge(1)
    //   currentAnswer = chans.answer
    //   challenge = chans.challenge
    //   sender ! challenge
    // case PlayerLogic.Stats =>
    //   sender ! PlayerStats(2, 300)
    // case PlayerLogic.Current =>
    //   sender ! link
  }

  def gameFinished: Receive = {
    case _ => sender ! "Game finished"
  }

  override def receiveCommand: Receive = readyToPlay
  override def receiveRecover: Receive = { case _ => }

}
