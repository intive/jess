package com.blstream.jess
package core.score

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.typesafe.scalalogging.LazyLogging

trait ScoreService extends LazyLogging {

  def scorePublisherActor: ActorRef

  def system: ActorSystem
   

  def scoreFlow: Flow[Message, Message, _] = {
    val actor = system.actorOf(Props[ScorePublisher])
    val ap = ActorPublisher[Score](actor)
    
    val src = Source.fromPublisher(ap)

    Flow.fromSinkAndSource(Sink.ignore, src.map(score => TextMessage.Strict(s"$score")))
  }

 // def websocketFlow: Flow[Message, Message, _] = {

//}
}
