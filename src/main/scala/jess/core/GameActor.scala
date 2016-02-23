package com.blstream.jess
package core

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.pattern._
import akka.util.Timeout
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._

case class Challenge(title: String, description: String, assigment: String)

case class Stats(attempts: Int, time: Long)

sealed trait ResponseAnswer

case object CorrectAnswer extends ResponseAnswer

case object IncorrectAnswer extends ResponseAnswer

object Challenge extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val format = jsonFormat3(Challenge.apply)
}

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
      (getRef(nick) ? PlayerActor.Start).mapTo[(Challenge, JessLink)] pipeTo sender
    case GameActor.GetChallenge(nick, link) ⇒
      (getRef(nick) ? PlayerActor.Next(link)).mapTo[Challenge] pipeTo sender
    case GameActor.PostChallenge(nick, link, answer) ⇒
      (getRef(nick) ? PlayerActor.Answer(link, answer)).mapTo[ResponseAnswer] pipeTo sender
    case GameActor.Stats(nick) ⇒
      val playerStats = (getRef(nick) ? PlayerActor.Stats).mapTo[PlayerStats]
      val stats = playerStats map (ps ⇒ Stats(ps.attempts, ps.time))
      stats pipeTo sender
    case GameActor.Current(nick) ⇒
      (getRef(nick) ? PlayerActor.Current).mapTo[JessLink] pipeTo sender
  }
}

trait Cache {
  self: Actor ⇒
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
