package jess
package api

import org.scalatest.{ Matchers, WordSpec }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest

class HealthRouteSpec
    extends WordSpec
    with HealthCheckRoute
    with Matchers with ScalatestRouteTest {

  "Route service" should {
    "return health message for GET request " in {
      Get("/health") ~> healthCheckRoute ~> check {
        status === StatusCodes.OK
      }
    }
  }
}
