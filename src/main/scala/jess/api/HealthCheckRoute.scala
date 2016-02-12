package jess
package api

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

trait HealthCheckRoute {

  def healthCheckRoute: Route =
    path("health") {
      get {
        complete {
          OK
        }
      }
    }
}

