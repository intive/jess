package jess

import akka.actor.ActorSystem
import akka.actor.Props
import akka.http.scaladsl._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import jess.api.GameRoute
import jess.api.HealthCheckRoute
import jess.core.GameActor
import jess.core.GameService
import scala.concurrent.duration._
import scala.io.StdIn
import com.typesafe.scalalogging.LazyLogging

object Main
  extends Main
  with App
  with JessHttpService
  with HealthCheckRoute
  with GameComponent

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
  logger.info(s"Starting server http://$interface:$port\nPress RETURN to stop...")
  StdIn.readLine()

  binding
    .flatMap(_.unbind())
    .onComplete(_ => system.shutdown)
}
