package com.blstream.jess

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives
import akka.util.Timeout
import api.{ HealthCheckRoute, Websocket }

import scala.concurrent.ExecutionContext

trait JessHttpService {
  self: HealthCheckRoute with GameComponent with Websocket =>

  import Directives._

  def route()(implicit system: ActorSystem, timeout: Timeout) =
    healthCheckRoute ~
      startGameRoute ~
      challangeRoute ~
      wsRoute
}
