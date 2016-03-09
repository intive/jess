package com.blstream.jess
package integrationtests

import java.util.UUID

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class PlayGameTest extends Simulation {

  val httpConf = http.baseURL("http://localhost:8090")
  val headers = Map("Content-Type" -> """application/json""")

  val nextUser = UUID.randomUUID().toString.replace("-", "")
  
  val feeder = Iterator.continually(Map("username" -> s"user-$nextUser"))

  val happyGame = scenario("start")
    .feed(feeder)
    .exec(
      http("start-game")
        .get("/game/${username}/start")
        .check(status.is(200))
        .check(jsonPath("$.challenge.link").saveAs("link"))
    ).exec(
      http("get-challenge")
        .get("/game/${username}/challenge/${link}")
        .check(status.is(200))
        .check(jsonPath("$.link").is("${link}"))
        .check(jsonPath("$.level").is("0"))
    )

  setUp(happyGame.inject(atOnceUsers(1))
    .protocols(httpConf))
    .assertions(
      global.failedRequests.count.is(0)
    )

}
