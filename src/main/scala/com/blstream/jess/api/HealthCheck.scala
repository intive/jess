package com.blstream.jess
package api

import spray.http.StatusCodes.OK
import spray.routing.HttpService

trait HealthCheck extends HttpService {
  lazy val healthCheckRoute =
    path("health") {
      get {
        complete {
            OK
        }
      }
    }
}

