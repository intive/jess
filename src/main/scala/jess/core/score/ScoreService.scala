package com.blstream.jess
package core.score

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging
import concurrent.duration._

trait ScoreService extends LazyLogging {

  def system: ActorSystem
  def scoreRouter: ActorRef

  def scoreFlow: Flow[Message, Message, _] = {

    val actor = system.actorOf(Props(classOf[ScorePublisher], scoreRouter))
    val ap = ActorPublisher[ScoreRouter.IncommingMessage](actor)

    val toApi: Flow[ScoreRouter.IncommingMessage, ScoreService.Api, _] = Flow[ScoreRouter.IncommingMessage].map {
      case ScoreRouter.Join(nick) => ScoreService.PlayerHasJoined(nick)
      case ScoreRouter.Score(nick, score) => ScoreService.PlayerScores(nick, score)
    }

    val src = Source
      .fromPublisher(ap)
      .via(toApi)
      .keepAlive(10.seconds, () => ScoreService.Ping)

    Flow.fromSinkAndSource(Sink.ignore, src.map(api => TextMessage.Strict(api.message)))

  }

}

object ScoreService {

  trait Api { def message: String }

  case class PlayerHasJoined(nick: String) extends Api {
    val message: String = s"Player $nick has joined game"
  }

  case class PlayerScores(nick: String, score: Int) extends Api {
    def message: String = s"Player $nick has earned point. Total score $score"

  }

  case object Ping extends Api { val message: String = "" }

}
