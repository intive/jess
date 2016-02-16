package com.blstream.jess
package core

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

object PlayersMap {
  private var players: Map[Nick, ActorRef] = Map()

  def getPlayer(nick: Nick)(implicit system: ActorSystem): ActorRef = {
    players.get(nick).getOrElse {
      val p = system.actorOf(Props[PlayerActor], nick)
      players += nick -> p
      p
    }
  }
}
