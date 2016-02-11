package jess

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives
import api.{ GameRoute, HealthCheckRoute }
import core._

import scala.concurrent.ExecutionContext

trait JessHttpService
    extends HealthCheckRoute
    with GameRoute with StartGameService {

  import Directives._

  def route()(implicit ec: ExecutionContext, context: ActorSystem) =
    healthCheckRoute ~
      gameRoute
}
