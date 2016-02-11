package jess
package core

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.ask

object GameActor {
  sealed trait GameMessages
  case class Start(nick: Nick, token: String) extends GameMessages
}

class GameActor extends Actor {

  var players = Map[Nick, ActorRef]()

  override def receive: Receive = {

    case GameActor.Start(nick, token) => {

      //todo: add token validation
      if (token == "good-token") {
        val player = players.get(nick).getOrElse {
          val p = context.actorOf(Props[PlayerActor], nick)
          players += nick -> p
          p
        }
        player ! PlayerActor.Start
      } else {
        //todo: add proper error types
        sender ! "error: bad token"
      }

    }

  }

}
