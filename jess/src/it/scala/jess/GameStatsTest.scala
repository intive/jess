package com.blstream.jess

import java.util.UUID

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class GameStatsTest extends Simulation {

  val httpConf = http.baseURL("http://localhost:8090")
  val jsonHeader = Map("Content-Type" -> "application/json")

  def nextUser = UUID.randomUUID().toString.replace("-", "")

  def feeder = Iterator.continually(Map("username" -> s"user-$nextUser"))

  val happyGame = scenario("game-happy-path")
    .feed(feeder)
    .exec(
      http("start game")
        .put("/game/${username}/start")
        .check(status.is(200))
        .check(jsonPath("$.challenge.link").saveAs("link"))
    ).exec(
        http("get stats first")
          .get("/game/${username}/challenge")
          .check(status.is(200))
          .check(jsonPath("$.stats.points").is("0"))
          .check(jsonPath("$.stats.attempts").is("0"))
      ).exec(
      http("put first answer")
        .put("/game/${username}/challenge/${link}")
        .headers(jsonHeader)
        .body(StringBody("""{"answer":"42"}"""))
        .check(status.is(200))
        .check(jsonPath("$.challenge.link").saveAs("link"))
    ).exec(
      http("get stats first")
        .get("/game/${username}/challenge")
        .check(status.is(200))
        .check(jsonPath("$.stats.points").is("10"))
        .check(jsonPath("$.stats.attempts").is("1"))
        .check(jsonPath("$.meta.link").saveAs("link"))
    ).exec(
      http("put second answer")
        .put("/game/${username}/challenge/${link}")
        .headers(jsonHeader)
        .body(StringBody("""{"answer":"4"}"""))
        .check(status.is(200))
        .check(jsonPath("$.challenge.link").saveAs("link"))
    ).exec(
    http("get stats after second")
      .get("/game/${username}/challenge")
      .check(status.is(200))
      .check(jsonPath("$.stats.points").is("20"))
      .check(jsonPath("$.stats.attempts").is("2"))
      .check(jsonPath("$.meta.link").saveAs("link"))
  ).exec(
    http("put third answer")
      .put("/game/${username}/challenge/${link}")
      .headers(jsonHeader)
      .body(StringBody("""{"answer":"233168"}"""))
      .check(status.is(200))
  ).exec(
    http("get stats after third")
      .get("/game/${username}/challenge")
      .check(status.is(200))
      .check(jsonPath("$.stats.points").is("30"))
      .check(jsonPath("$.stats.attempts").is("3"))
  )

  setUp(happyGame.inject(atOnceUsers(1))
    .protocols(httpConf))
    .assertions(
      global.failedRequests.count.is(0)
    )

}
