package com.blstream.jess
package core.score

import akka.actor.{ ActorSystem, Props }
import akka.stream.Materializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ Sink, Source }

trait ScoreService {

  implicit val system: ActorSystem
  implicit val materializer: Materializer

  val scorePublisherActor = system.actorOf(Props[ScorePublisher], name = "ScorePublisher")
  val scorePublisher = ActorPublisher[Score](scorePublisherActor)

  Source.actorPublisher(Props[ScorePublisher])

  Source.fromPublisher(scorePublisher).runWith(Sink.foreach(println))

}
