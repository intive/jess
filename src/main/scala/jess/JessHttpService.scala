package com.blstream.jess

import akka.http.scaladsl.server.Directives
import api.{ GameRoute, HealthCheckRoute, Websocket }

trait JessHttpService {
  self: HealthCheckRoute with GameRoute with Websocket =>

  import Directives._

  def route =
    healthCheckRoute ~
      gameRoute ~
      wsRoute
}
