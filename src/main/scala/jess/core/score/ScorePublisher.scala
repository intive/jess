package com.blstream.jess
package core.score

import akka.actor.{ ActorLogging, ActorRef }
import akka.routing.{ ActorRefRoutee, AddRoutee, RemoveRoutee }
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }

case class Score(name: String, score: Int)

class ScorePublisher(router: ActorRef)
    extends ActorPublisher[ScoreRouter.IncommingMessage]
    with ActorLogging {

  import scala.collection.mutable

  var queue: mutable.Queue[ScoreRouter.IncommingMessage] = mutable.Queue()

  override def preStart(): Unit = {
    log.info("Adding Score Publisher")
    router ! AddRoutee(ActorRefRoutee(self))
  }

  override def postStop(): Unit = {
    log.info("Removing Score Publisher")
    router ! RemoveRoutee(ActorRefRoutee(self))

  }

  def receive = {
    case msg: ScoreRouter.IncommingMessage =>
      log.info(s"got message $msg")
      queue.enqueue(msg)
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

