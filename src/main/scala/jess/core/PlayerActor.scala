package com.blstream.jess
package core

import akka.persistence.PersistentActor

object PlayerActor {
  sealed trait PlayerMessages
  case object Start extends PlayerMessages
}

sealed trait PlayerEvents
case object GameStarted

class PlayerActor extends PersistentActor {

  override def persistenceId: String = "player-actor"
  var points: Long = 0

  override def receiveCommand: Receive = {
    case PlayerActor.Start => persist(GameStarted)(ev => startGame)
  }

  override def receiveRecover: Receive = {
    case GameStarted => startGame
  }

  private def startGame = points = 0

}
