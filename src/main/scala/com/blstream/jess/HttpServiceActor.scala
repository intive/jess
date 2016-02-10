package com.blstream.jess

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives

import scala.concurrent.ExecutionContext

import api.{ StartGame, HealthCheck }

trait HttpServiceActor
    extends HealthCheck
    with StartGame with core.StartGameService {

  import Directives._

  def route()(implicit ec: ExecutionContext) = healthCheckRoute ~ startGameRoute
}
