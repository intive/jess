package com.blstream.jess
package core.score

import akka.actor.{ ActorLogging, ActorRef }
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }
import akka.routing.{RemoveRoutee, ActorRefRoutee, AddRoutee}

case class Score(name: String, score: Int)

class ScorePublisher(router: ActorRef)
    extends ActorPublisher[Score]
    with ActorLogging {

  import scala.collection.mutable
  import ScorePublisher._

  var queue: mutable.Queue[Score] = mutable.Queue()

  // on startup, register with routee
  override def preStart(): Unit = {
    log.info("Prestart")
    router ! AddRoutee(ActorRefRoutee(self))

  }

  // cleanly remove this actor from the router. To
  // make sure our custom router only keeps track of
  // alive actors.
  override def postStop(): Unit = {
    log.info("Poststop")
    router ! RemoveRoutee(ActorRefRoutee(self))

  }

  def receive = {
    case score @ ScoreRouter.Score(_) =>
      log.info(s"got message $score")
      queue.enqueue(Score("Marcin", score.score))
      sendScore()
    case Request(_) =>
      sendScore()
    case Cancel => context.stop(self)
  }

  def sendScore() = {
    log.info(s"send score ${queue.nonEmpty} | $isActive | $totalDemand ")
    while (queue.nonEmpty && isActive && totalDemand > 0) {
      onNext(queue.dequeue)
    }
  }

}

object ScorePublisher {
  case class Publish(score: Score)
}

