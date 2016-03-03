package com.blstream.jess
package core

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }

import akka.pattern._
import akka.util.Timeout

import state._

import scala.concurrent.duration._

case class Stats(attempts: Int, time: Long, points: Long)

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

class GameActor(scoreRouter: ActorRef)
    extends Actor
    with ChallengeService
    with Cache
    with ActorLogging {

  import context._

  implicit val timeout = Timeout(5 second)

  override def receive = {
    case GameActor.Join(nick) =>
      (getRef(nick, scoreRouter) ? PlayerLogic.StartGame(nick)) pipeTo sender
    case GameActor.GetChallenge(nick, link) =>
      (getRef(nick, scoreRouter) ? PlayerLogic.Next(link)) pipeTo sender
    case GameActor.PostChallenge(nick, link, answer) =>
      (getRef(nick, scoreRouter) ? PlayerLogic.Answer(link, answer)) pipeTo sender
    case GameActor.Stats(nick) =>
      //TODO when state transition error comes then class cast exception is thrown
      val playerStats = (getRef(nick, scoreRouter) ? PlayerLogic.Stats).mapTo[PlayerStats]
      val stats = playerStats map (ps => Stats(ps.attempts, ps.time, ps.points))
      stats pipeTo sender
    case GameActor.Current(nick) =>
      (getRef(nick, scoreRouter) ? PlayerLogic.Current) pipeTo sender
  }
}

trait Cache {
  self: Actor =>
  var cache: collection.mutable.Map[Nick, ActorRef] = collection.mutable.Map.empty

  def getRef(nick: Nick, scoreRouter: ActorRef) = {
    if (cache.contains(nick)) {
      cache(nick)
    } else {
      val ref = context.actorOf(Props(classOf[PlayerActor], scoreRouter), nick)
      cache += (nick -> ref)
      ref
    }
  }
}
