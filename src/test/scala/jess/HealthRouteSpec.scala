package com.blstream.jess
package api

import org.scalatest.{ Matchers, WordSpec }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._

class HealthRouteSpec
    extends WordSpec
    with HealthCheck
    with Matchers with ScalatestRouteTest {

  "Route service" should {
    "return health message for GET request " in {
      Get("/health") ~> healthCheckRoute ~> check {
        status === StatusCodes.OK
      }
    }
  }
}
