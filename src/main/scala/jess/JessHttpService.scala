package jess

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives
import api.HealthCheckRoute

import scala.concurrent.ExecutionContext

trait JessHttpService {
  self: HealthCheckRoute with GameComponent =>

  import Directives._

  def route()(implicit ec: ExecutionContext, context: ActorSystem) =
    healthCheckRoute ~
      gameRoute
}
