package com.blstream.jess
package core.score

import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.routing.{ ActorRefRoutee, AddRoutee, RemoveRoutee, Routee }

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
    case msg: Any => log.info(s"Got unknown message $msg")
  }

  private def broadcast[A](message: A) = {
    log.info(s"Participiants $participiants with get the message")
    participiants.foreach(_.send(message, sender))
  }

}

object ScoreRouter {
  trait IncommingMessage
  case class Score(score: Int) extends IncommingMessage
}
