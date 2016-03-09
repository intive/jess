package com.blstream.jess
package integrationtests

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class PlayGameTest extends Simulation {

  val httpConf = http.baseURL("http://localhost:8090")
  val headers = Map("Content-Type" -> """application/json""")
  val scn = scenario("test").exec(
    http("ex1")
      .get("/health")
  )

  val start = scenario("start").exec(
    http("foo")
      .get("/game/luke/start")
      .headers(headers)
  )

  setUp(start.inject(atOnceUsers(1))
    .protocols(httpConf))
    .assertions(
      global.failedRequests.count.is(0)
    )

}