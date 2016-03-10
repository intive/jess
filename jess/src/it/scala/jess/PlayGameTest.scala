package com.blstream.jess
package integrationtests

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
        .get("/game/${username}/start")
        .check(status.is(200))
        .check(jsonPath("$.challenge.link").saveAs("link"))
    ).exec(
      http("get first challenge")
        .get("/game/${username}/challenge/${link}")
        .check(status.is(200))
        .check(jsonPath("$.link").is("${link}"))
        .check(jsonPath("$.level").is("0"))
    ).exec(
      http("post first answer")
        .post("/game/${username}/challenge/${link}")
        .headers(jsonHeader)
        .body(StringBody("""{"answer":"42"}"""))
        .check(status.is(200))
        .check(bodyString.is("Correct Answer"))
    ).exec(
      http("get stats after first")
        .get("/game/${username}/challenge")
        .check(status.is(200))
        .check(jsonPath("$.meta.link").saveAs("link"))
        .check(jsonPath("$.stats.points").is("10"))
        .check(jsonPath("$.stats.attempts").is("1"))
    ).exec(
      http("get second challenge")
        .get("/game/${username}/challenge/${link}")
        .check(status.is(200))
        .check(jsonPath("$.link").is("${link}"))
        .check(jsonPath("$.level").is("1"))
    ).exec(
      http("post second answer")
        .post("/game/${username}/challenge/${link}")
        .headers(jsonHeader)
        .body(StringBody("""{"answer":"4"}"""))
        .check(status.is(200))
        .check(bodyString.is("Correct Answer"))
    ).exec(
    http("get stats after second")
      .get("/game/${username}/challenge")
      .check(status.is(200))
      .check(jsonPath("$.meta.link").saveAs("link"))
      .check(jsonPath("$.stats.points").is("20"))
      .check(jsonPath("$.stats.attempts").is("2"))
  ).exec(
    http("get third challenge")
      .get("/game/${username}/challenge/${link}")
      .check(status.is(200))
      .check(jsonPath("$.link").is("${link}"))
      .check(jsonPath("$.level").is("2"))
  )
//    .exec(
//    http("post third answer")
//      .post("/game/${username}/challenge/${link}")
//      .headers(jsonHeader)
//      .body(StringBody("""{"answer":"233168"}"""))
//      .check(status.is(200))
//      .check(bodyString.is("Correct Answer"))
//  )

  setUp(happyGame.inject(atOnceUsers(1))
    .protocols(httpConf))
    .assertions(
      global.failedRequests.count.is(0)
    )

}
