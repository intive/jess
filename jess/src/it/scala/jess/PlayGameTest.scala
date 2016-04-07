package com.blstream.jess

import java.util.UUID

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class PlayGameTest extends Simulation {

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
      http("put first answer")
        .put("/game/${username}/challenge/${link}")
        .headers(jsonHeader)
        .body(StringBody("""{"answer":"42"}"""))
        .check(status.is(200))
        .check(jsonPath("$.answer.correct").is("true"))
        .check(jsonPath("$.answer.points").is("+10"))
        .check(jsonPath("$.gameStatus").is("playing"))
        .check(jsonPath("$.challenge.link").saveAs("link"))
    ).exec(
      http("put second answer")
        .put("/game/${username}/challenge/${link}")
        .headers(jsonHeader)
        .body(StringBody("""{"answer":"4"}"""))
        .check(status.is(200))
        .check(jsonPath("$.answer.correct").is("true"))
        .check(jsonPath("$.answer.points").is("+15"))
        .check(jsonPath("$.gameStatus").is("playing"))
        .check(jsonPath("$.challenge.link").saveAs("link"))
    ).exec(
      http("put third answer")
        .put("/game/${username}/challenge/${link}")
        .headers(jsonHeader)
        .body(StringBody("""{"answer":"233168"}"""))
        .check(status.is(200))
        .check(jsonPath("$.answer.correct").is("true"))
        .check(jsonPath("$.answer.points").is("+20"))
        .check(jsonPath("$.gameStatus").is("finished"))
    )

  setUp(happyGame.inject(atOnceUsers(1))
    .protocols(httpConf))
    .assertions(
      global.failedRequests.count.is(0)
    )

}
