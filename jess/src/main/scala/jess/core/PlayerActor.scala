package com.blstream.jess
package core

import akka.actor.ActorRef
import akka.persistence.PersistentActor
import score.ScoreRouter
import core.state._
import cats.syntax.xor._

case class PlayerStatus(attempts: Int, time: Long, points: Long)

sealed trait PlayerEvents

case class StateModified(ps: Option[PlayerState]) extends PlayerEvents

class PlayerActor(scoreRouter: ActorRef)
    extends PersistentActor
    with ChallengeService
    with LinkGenerator
    with PlayerLogic
    with NickValidator {

  var state: Option[PlayerState] = None

  override def persistenceId: String = "player-actor"

  def readyToPlay: Receive = {
    case sg @ PlayerLogic.StartGame(_) => {
      val (newStateMaybe, chOrErr) = startGame(sg).run(state).value
      newStateMaybe match {
        case someNewState @ Some(_) => {
          persist(
            StateModified(someNewState)
          )(ev => {
              state = someNewState
              val nick = state.get.nick
              scoreRouter ! ScoreRouter.Join(nick)
              context become playing
              sender ! chOrErr.map(_.withoutAnswer)
            })
        }
        case None => sender ! chOrErr
      }
    }

    case _ => sender ! StateTransitionError("Start game first").left
  }

  def playing: Receive = {
    case PlayerLogic.StartGame(_) => sender ! StateTransitionError("Game already started").left

    case ans @ PlayerLogic.Answer(_, _) =>
      val (nps, chOrErr) = answerChallenge(ans).run(state).value
      nps match {
        case someNewState @ Some(_) => {
          persist(
            StateModified(someNewState)
          )(ev => {
              state = someNewState
              sender ! chOrErr
              val (nick, points) = (state.get.nick, state.get.points)
              scoreRouter ! ScoreRouter.Score(nick, points)
            })
        }
        case None => sender ! chOrErr
      }
    case PlayerLogic.GetChallenge(link) =>
      state match {
        case Some(st) => sender ! st.challenges(link).withoutAnswer
        case None => sender ! StateNotInitialized
      }
    case PlayerLogic.Stats =>
      state match {
        case Some(st) => sender ! PlayerStatus(st.attempts, 10, st.points)
        case None => sender ! StateNotInitialized
      }
    case PlayerLogic.Current =>
      state match {
        case Some(st) => sender ! st.challenge.link
        case None => sender ! StateNotInitialized
      }
  }

  def gameFinished: Receive = {
    case _ => sender ! "Game finished"
  }

  override def receiveCommand: Receive = readyToPlay
  override def receiveRecover: Receive = { case _ => }

}
