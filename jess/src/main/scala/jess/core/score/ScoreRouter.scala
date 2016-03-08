package com.blstream.jess
package core.score

import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.routing.{ AddRoutee, RemoveRoutee, Routee }

class ScoreRouter
    extends Actor
    with ActorLogging {
  import ScoreRouter._

  var participiants = Set.empty[Routee]

  def receive = {
    case AddRoutee(ref) =>
      participiants += ref
      log.info(s"Adding routee. Current connections ${participiants.size}")
    case RemoveRoutee(ref) =>
      participiants -= ref
      log.info(s"Removing routee. Current connections ${participiants.size}")
    case msg: IncommingMessage =>
      log.info(s"Got messasge $msg")
      broadcast(msg)

  }

  private def broadcast[A](message: A) = {
    participiants.foreach { p =>
      log.info(s"Participiant $p will get the message $message")
      p.send(message, sender)
    }
  }

}

object ScoreRouter {
  trait IncommingMessage
  case class Score(name: String, score: Int) extends IncommingMessage
  case class Join(nick: String) extends IncommingMessage
}
