package com.blstream.jess
package core

import akka.actor._
import akka.persistence._
import score.ScoreRouter
import core.state._
import cats.syntax.xor._

case class PlayerStatus(attempts: Int, time: Long, points: Long)

sealed trait PlayerEvents
case class StateModified(ps: PlayerState) extends PlayerEvents
case object BecomePlaying

class PlayerActor(scoreRouter: ActorRef, nick: String)
    extends PersistentActor
    with ChallengeService
    with LinkGenerator
    with PlayerLogic
    with NickValidator {

  var stateMaybe: Option[PlayerState] = None

  override def persistenceId: String = s"player-$nick"

  def readyToPlay: Receive = {
    case sg @ PlayerLogic.StartGame(_) => {
      val (newStateMaybe, chOrErr) = startGame(sg).run(stateMaybe).value
      newStateMaybe match {
        case Some(someNewState) =>
          persist(
            List(
              StateModified(someNewState),
              BecomePlaying
            )
          )(ev => {
              stateMaybe = Some(someNewState)
              sender ! chOrErr
              scoreRouter ! ScoreRouter.Join(someNewState.nick)
              context become playing
            })
        case None => sender ! chOrErr
      }
    }

    case _ => sender ! StateTransitionError("Start game first").left
  }

  def playing: Receive = {
    case PlayerLogic.StartGame(_) => sender ! StateTransitionError("Game already started").left

    case ans @ PlayerLogic.Answer(_, _) =>
      stateMaybe match {
        case Some(state) => {
          val (nps, chOrErr) = answerChallenge(ans).run(state).value
          persist(
            StateModified(nps)
          )(ev => {
              stateMaybe = Some(nps)
              sender ! chOrErr
              val (nick, points) = (state.nick, state.points)
              scoreRouter ! ScoreRouter.Score(nick, points)
            })
        }
        case None => sender ! StateNotInitialized.left
      }
    case PlayerLogic.GetChallenge(link) =>
      stateMaybe match {
        case Some(st) => sender ! st.challenges(link).withoutAnswer
        case None => sender ! StateNotInitialized
      }
    case PlayerLogic.Stats =>
      stateMaybe match {
        case Some(st) => sender ! PlayerStatus(st.attempts, 10, st.points)
        case None => sender ! StateNotInitialized
      }
    case PlayerLogic.Current =>
      stateMaybe match {
        case Some(st) => sender ! st.challenge.link
        case None => sender ! StateNotInitialized
      }
  }
  //
  // def gameFinished: Receive = {
  //   case _ => sender ! "Game finished"
  // }

  override def receiveCommand: Receive = readyToPlay

  override val receiveRecover: Receive = {
    case StateModified(playerState) => stateMaybe = Some(playerState)
    case BecomePlaying => context become playing
    // case SnapshotOffer(_, snapshot: PlayerState) => stateMaybe = Some(snapshot)
  }

}
