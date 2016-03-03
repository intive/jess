package com.blstream.jess
package core.score

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging

trait ScoreService extends LazyLogging {

  def system: ActorSystem
  def scoreRouter: ActorRef

  def scoreFlow: Flow[Message, Message, _] = {

    val actor = system.actorOf(Props(classOf[ScorePublisher], scoreRouter))
    val ap = ActorPublisher[Score](actor)

    val src = Source.fromPublisher(ap)

    Flow.fromSinkAndSource(Sink.ignore, src.map(score => TextMessage.Strict(s"$score")))

    /*    val eventSrc = Source.actorRef[IncommingMessage](bufferSize = 5, OverflowStrategy.dropNew)

    val in = Flow[Message].map[String] {
      case TextMessage.Strict(txt) => txt
    }

    val out = in.zipMat(eventSrc)((_, m) => m).map {
      case (x, y) => TextMessage("dupa")
    }

    out
 */
  }

  /*  RunnableGraph.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[Unit] =>

      val from = builder.add(Flow[Message].collect {
        case TextMessage.Strict(txt) => txt

      })

      val to = builder.add(Flow[Message].collect {
        case _ => TextMessage("dupa")

      })

    FlowShape(from.in, to.out)
  })
 */
  /*
   val actor = system.actorOf(Props[ScoreActor])
    val ap = ActorPublisher[Score](actor)

    val src = Source.fromPublisher(ap)

    Flow.fromSinkAndSource(Sink.ignore, src.map(score => TextMessage.Strict(s"$score")))
 */

}
