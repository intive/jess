package com.blstream.jess

import akka.http.scaladsl.server.Directives
import api.{ GameRoute, HealthCheckRoute, AdminRoute, Websocket }

trait JessHttpService {
  self: HealthCheckRoute with GameRoute with AdminRoute with Websocket =>

  import Directives._

  def route =
    healthCheckRoute ~
      gameRoute ~
      adminRoute ~
      wsRoute
}
