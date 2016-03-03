package com.blstream.jess
package core

import akka.actor.ActorRef
import akka.persistence.PersistentActor
import score.ScoreRouter
import core.state.{ NickValidator, PlayerLogic, PlayerState, StateTransitionError }
import com.blstream.jess.core.state._
import cats.syntax.xor._

case class PlayerStatus(attempts: Int, time: Long, points: Long)

sealed trait PlayerEvents

case class StateModified(ps: PlayerState) extends PlayerEvents

class PlayerActor(scoreRouter: ActorRef)
    extends PersistentActor
    with ChallengeService
    with LinkGenerator
    with PlayerLogic
    with NickValidator {

  var state: PlayerState = initGame.runS(initialState(nextChallenge(0))).value

  override def persistenceId: String = "player-actor"

  def readyToPlay: Receive = {
    case sg @ PlayerLogic.StartGame(_) =>
      val maybeChallenge = for {
        start <- startGame(sg)
      } yield {
        val (newState, ch) = start.run(state).value
        persist(
          StateModified(newState)
        )(ev => {
            state = newState
            val nick = state.nick.getOrElse("Unknown")
            scoreRouter ! ScoreRouter.Join(nick)
            context become playing
          })
        ch
      }
      sender ! maybeChallenge

    case _ => sender ! StateTransitionError("Start game first").left
  }

  def playing: Receive = {
    case PlayerLogic.StartGame(_) => sender ! StateTransitionError("Game already started").left

    case ans @ PlayerLogic.Answer(_, _) =>
      val (nps, challenge) = answerChallenge(ans).run(state).value
      persist(
        StateModified(state)
      )(ev => {
          state = nps
          sender ! challenge
          val (nick, points) = (state.nick.getOrElse("Unknown"), state.points)
          scoreRouter ! ScoreRouter.Score(nick, points)
        })
    case PlayerLogic.GetChallenge(lnk) =>
      sender ! state.challenge
    case PlayerLogic.Stats =>
      sender ! PlayerStatus(state.attempts, 10, state.points)
    case PlayerLogic.Current =>
      sender ! state.challenge.link
  }

  def gameFinished: Receive = {
    case _ => sender ! "Game finished"
  }

  override def receiveCommand: Receive = readyToPlay
  override def receiveRecover: Receive = { case _ => }

}
