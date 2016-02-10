package com.blstream.jess

import akka.http.scaladsl.server.Directives
import api.{ StartGame, HealthCheck }
import com.blstream.jess.api.StartGame
import core._

import scala.concurrent.ExecutionContext

trait HttpServiceActor
    extends HealthCheck
    with StartGame with StartGameService {

  import Directives._

  def route()(implicit ec: ExecutionContext) = healthCheckRoute ~ startGameRoute
}
