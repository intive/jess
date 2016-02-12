package jess

import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import jess.api.GameRoute
import jess.api.HealthCheckRoute
import jess.core.GameService
import scala.concurrent.duration._

object Main
  extends Main
  with App
  with JessHttpService
  with HealthCheckRoute
  with GameComponent

trait GameComponent
  extends GameRoute
  with GameService

abstract class Main {
  jess: JessHttpService =>

  implicit val system = ActorSystem("jess")
  implicit val flowMaterializer = ActorMaterializer()

  implicit val timeout = Timeout(5.seconds)

  import scala.concurrent.ExecutionContext.Implicits.global

  val binding = Http().bindAndHandle(route, "0.0.0.0", 8090)
}
