package com.blstream.jess
package api

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

trait HealthCheck {

  def healthCheckRoute: Route = path("health") {
    get {
      complete {
        OK
      }
    }
  }
}

