package com.blstream.jess

import akka.http.scaladsl.server.Directives
import akka.util.Timeout
import api.{ GameRoute, HealthCheckRoute, AdminRoute, Websocket }

import scala.concurrent.ExecutionContext

trait JessHttpService {
  self: HealthCheckRoute with GameRoute with AdminRoute with Websocket =>

  import Directives._

  def route(implicit ec: ExecutionContext, timeout: Timeout, gameStateRef: GameStateRef, scoreRouterRef: ScoreRouterRef, scorePublisherRef: ScorePublisherRef) =
    healthCheckRoute ~
      gameRoute ~
      adminRoute ~
      wsRoute
}
