package jess

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives
import akka.util.Timeout
import api.HealthCheckRoute

import scala.concurrent.ExecutionContext

trait JessHttpService {
  self: HealthCheckRoute with GameComponent =>

  import Directives._

  def route()(implicit system: ActorSystem, timeout: Timeout) =
    healthCheckRoute ~
      gameRoute
}
