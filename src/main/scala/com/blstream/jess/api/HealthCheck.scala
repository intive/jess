package com.blstream.jess
package api


import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes._
import concurrent.ExecutionContext


trait HealthCheck {

  def healthCheckRoute(implicit ec: ExecutionContext): Route = path("health") {
      get {
        complete {
          OK
        }
      }
    }
}

