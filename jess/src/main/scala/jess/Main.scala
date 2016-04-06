package com.blstream.jess

import akka.actor._
import akka.http.scaladsl._
import akka.stream.ActorMaterializer
import akka.util.Timeout

import api.{ GameRoute, HealthCheckRoute, AdminRoute, Websocket }
import core.state.{ StartGameValidator, PlayerLogic }
import com.blstream.jess.core._
import core.score.{ ScoreService, ScorePublisher, ScoreRouter }

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

object Main
    extends App
    with JessHttpService
    with GameRoute
    with GameService
    with PlayerLogic
    with ChallengeService
    with LinkGenerator
    with StartGameValidator
    with HealthCheckRoute
    with AdminRoute
    with Websocket
    with ScoreService {

  val config = if (args.nonEmpty && args(0).equals("persistence=inmem")) Some(ConfigFactory.load.getConfig("inmem")) else None

  implicit val timeout = Timeout(5 seconds)

  val acName = "jess"
  implicit val system: ActorSystem = config.fold(ActorSystem(acName))(c => ActorSystem(acName, c))

  lazy val scoreRouter = system.actorOf(Props[ScoreRouter], "ScoreRouter")
  lazy val scorePublisherActor = system.actorOf(Props[ScorePublisher], "ScorePublisher")
  lazy val gameStateActor = GameStateRef(system.actorOf(Props(classOf[GameStateActor], timeout), "GameStateActor"))

  val listener = system.actorOf(Props(new UnhandledMessageListener()))
  system.eventStream.subscribe(listener, classOf[UnhandledMessage])
  implicit val flowMaterializer = ActorMaterializer()

  val interface = "0.0.0.0"
  val port = 8090

  val binding = Http().bindAndHandle(route, interface, port)
}

case class GameStateRef(actor: ActorRef)
case class ScoreRouterRef(actor: ActorRef)

class UnhandledMessageListener extends Actor with ActorLogging {

  override def receive = {
    case message: UnhandledMessage =>
      log.error(s"CRITICAL! No actors found for message ${message.getMessage}")
      log.error("Shutting application down")
      System.exit(-1)
  }

}
