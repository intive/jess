package com.blstream
package jess

import akka.actor.{ Props, ActorRef, Actor }
import akka.persistence.PersistentActor
import akka.util.Timeout
import akka.pattern._
import cats.data.Xor
import cats.syntax.xor._
import core.score.ScoreRouter
import core.state.PlayerLogic.StartGame
import core._
import core.state._

import scala.concurrent.{ Future, ExecutionContext }

case class GameResponse(playerState: Option[PlayerState], resp: SomeError Xor ChallengeServiceResponse)

trait GameService {
  self: PlayerLogic =>

  import GameStateActor._

  def joinGameIo(nick: Nick)(implicit ec: ExecutionContext, timeout: Timeout, gameStateRef: GameStateRef, scoreRouterRef: ScoreRouterRef): Future[SomeError Xor ChallengeServiceResponse] =
    for {
      stateMaybe <- (gameStateRef.actor ? GetPlayerState(nick)).mapTo[Option[PlayerState]]
      gameResp <- joinGame(nick)(stateMaybe)
      _ <- gameStateRef.actor ? SetPlayerState(nick, gameResp.playerState)
    } yield {
      scoreRouterRef.actor ! ScoreRouter.Join(nick)
      gameResp.resp
    }

  def joinGame(nick: Nick)(stateMaybe: Option[PlayerState])(implicit ec: ExecutionContext, timeout: Timeout): Future[GameResponse] =
    for {
      (newStateMaybe, chOrErr) <- Future { startGame(StartGame(nick)).run(stateMaybe).value }
    } yield GameResponse(newStateMaybe, chOrErr)

  def getChallengeIo(nick: Nick, link: JessLink)(implicit ec: ExecutionContext, timeout: Timeout, gameStateRef: GameStateRef, scoreRouterRef: ScoreRouterRef): Future[SomeError Xor Challenge] =
    for {
      stateMaybe <- (gameStateRef.actor ? GetPlayerState(nick)).mapTo[Option[PlayerState]]
    } yield {
      stateMaybe match {
          //TODO fix for non existing link
        case Some(st) => st.challenges(link).withoutAnswer.right
        case None => StateNotInitialized.left
      }
    }
}

object GameStateActor {
  sealed trait GameStateActorCommands
  case class GetPlayerState(nick: Nick) extends GameStateActorCommands
  case class SetPlayerState(nick: Nick, state: Option[PlayerState]) extends GameStateActorCommands
}

class GameStateActor(implicit timeout: Timeout)
    extends Actor
    with PlayerCache {

  import GameStateActor._
  import context._

  override def receive: Receive = {
    case GetPlayerState(nick) => (getRef(nick) ? GetPlayerState(nick)) pipeTo sender
    case SetPlayerState(nick, state) => (getRef(nick) ? SetPlayerState(nick, state)) pipeTo sender
  }
}

trait PlayerCache {
  self: Actor =>
  var cache: collection.mutable.Map[Nick, ActorRef] = collection.mutable.Map.empty

  def getRef(nick: Nick) = {
    if (cache.contains(nick)) {
      cache(nick)
    } else {
      val ref = context.actorOf(Props(classOf[PlayerStateActor], nick), name = nick)
      cache += (nick -> ref)
      ref
    }
  }
}

class PlayerStateActor(nick: Nick) extends PersistentActor {
  import GameStateActor._
  override def persistenceId: String = s"player-$nick"

  var playerState: Option[PlayerState] = None

  override def receiveCommand: Receive = {
    case GetPlayerState(_) => sender ! playerState
    case SetPlayerState(nick, state) =>
      persist(state)(_ => {
        playerState = state
        sender ! "ok"
      })
  }

  override def receiveRecover: Receive = {
    case SetPlayerState(_, state) => playerState = state
  }
}
