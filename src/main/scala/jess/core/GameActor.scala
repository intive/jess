package com.blstream.jess
package core

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }

import akka.pattern._
import akka.util.Timeout
import core.state.Challenge

import state._

import scala.concurrent.duration._

case class Stats(attempts: Int, time: Long)

sealed trait ResponseAnswer

case object CorrectAnswer extends ResponseAnswer

case object IncorrectAnswer extends ResponseAnswer

object GameActor {

  sealed trait GameMessages

  case class Join(nick: Nick) extends GameMessages

  case class GetChallenge(nick: Nick, link: JessLink) extends GameMessages

  case class PostChallenge(nick: Nick, link: JessLink, answer: String) extends GameMessages

  case class Stats(nick: Nick) extends GameMessages

  case class Current(nick: Nick) extends GameMessages

}

class GameActor
    extends Actor
    with ChallengeService
    with Cache
    with ActorLogging {

  import context._

  implicit val timeout = Timeout(5 second)

  override def receive = {
    case GameActor.Join(nick) =>
      (getRef(nick) ? PlayerLogic.StartGame(nick)) pipeTo sender
    case GameActor.GetChallenge(nick, link) =>
      (getRef(nick) ? PlayerLogic.Next(link)).mapTo[Challenge] pipeTo sender
    case GameActor.PostChallenge(nick, link, answer) =>
      (getRef(nick) ? PlayerLogic.Answer(link, answer)) pipeTo sender
    case GameActor.Stats(nick) =>
      val playerStats = (getRef(nick) ? PlayerLogic.Stats).mapTo[PlayerStats]
      val stats = playerStats map (ps => Stats(ps.attempts, ps.time))
      stats pipeTo sender
    case GameActor.Current(nick) =>
      (getRef(nick) ? PlayerLogic.Current).mapTo[JessLink] pipeTo sender
  }
}

trait Cache {
  self: Actor =>
  var cache: collection.mutable.Map[Nick, ActorRef] = collection.mutable.Map.empty

  def getRef(nick: Nick) = {
    if (cache.contains(nick)) {
      cache(nick)
    } else {
      val ref = context.actorOf(Props[PlayerActor], nick)
      cache += (nick → ref)
      ref
    }
  }
}
