package com.blstream.jess
package core.score

import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }

case class Score(name: String, score: Int)

class ScorePublisher
    extends ActorPublisher[Score]
    with ActorLogging {

  import scala.collection.mutable
  import ScorePublisher._

  var queue: mutable.Queue[Score] = mutable.Queue()

  def receive = {
    case Publish(score) =>
      log.info(s"got message $score")
      queue.enqueue(score)
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

