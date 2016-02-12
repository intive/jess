package com.blstream.jess

import akka.actor.ActorSystem
import akka.actor.Props
import akka.http.scaladsl._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import api.GameRoute
import api.{ HealthCheckRoute, Websocket }
import core.GameActor
import core.GameService
import scala.concurrent.duration._
import com.typesafe.scalalogging.LazyLogging

object Main
  extends Main
  with App
  with JessHttpService
  with HealthCheckRoute
  with GameComponent
  with Websocket

trait GameComponent
  extends GameRoute
  with GameService

abstract class Main extends LazyLogging {
  jess: JessHttpService =>

  implicit val system = ActorSystem("jess")
  implicit val flowMaterializer = ActorMaterializer()
  implicit val timeout = Timeout(5.seconds)
  import scala.concurrent.ExecutionContext.Implicits.global

  val interface = "0.0.0.0"
  val port = 8090

  system.actorOf(Props[GameActor], "game")

  val binding = Http().bindAndHandle(route, interface, port)
  logger.info(s"Starting server http://$interface:$port")

}
