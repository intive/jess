package com.blstream.jess

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props, UnhandledMessage }
import akka.http.scaladsl._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import api.{ GameRoute, HealthCheckRoute, Websocket }
import core.score.{ ScoreService, ScorePublisher, ScoreRouter }
import com.typesafe.scalalogging.LazyLogging
import com.blstream.jess.core.{ ChallengeActor, GameActor }
import jess.core.AdminActor

import scala.concurrent.duration._

object Main
    extends Main
    with App
    with JessHttpService
    with GameRoute
    with HealthCheckRoute
    with Websocket
    with ScoreService {

  lazy val scoreRouter = system.actorOf(Props[ScoreRouter], "ScoreRouter")
  lazy val scorePublisherActor = system.actorOf(Props[ScorePublisher], "ScorePublisher")
  lazy val gameActorRef = system.actorOf(Props(classOf[GameActor], scoreRouter), "GameActor")
  lazy val challengeActorRef = system.actorOf(Props(classOf[ChallengeActor]), "ChallengeActor")
  lazy val adminActorRef = system.actorOf(Props(classOf[AdminActor]), "AdminActor")
}

abstract class Main
    extends LazyLogging {
  jess: JessHttpService =>

  implicit val system: ActorSystem = ActorSystem("jess")
  val listener = system.actorOf(Props(new UnhandledMessageListener()))
  system.eventStream.subscribe(listener, classOf[UnhandledMessage])
  implicit val flowMaterializer = ActorMaterializer()
  implicit val timeout = Timeout(5.seconds)

  val interface = "0.0.0.0"
  val port = 8090

  val binding = Http().bindAndHandle(route, interface, port)
}

class UnhandledMessageListener extends Actor with ActorLogging {

  override def receive = {
    case message: UnhandledMessage =>
      log.error(s"CRITICAL! No actors found for message ${message.getMessage}")
      log.error("Shutting application down")
      System.exit(-1)
  }

}
