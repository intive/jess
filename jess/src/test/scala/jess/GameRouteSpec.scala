package com.blstream.jess
package api

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import core.GameActor
import core.score.ScoreRouter
import org.scalatest.{ Matchers, WordSpec }
import concurrent.duration._

class GameRouteSpec
    extends WordSpec
    with GameRoute
    with Matchers
    with ScalatestRouteTest {

  implicit val as = ActorSystem("test")
  implicit val timeout = Timeout(5 seconds)
  val scoreRouterRef = as.actorOf(Props[ScoreRouter], "router")
  val gameActorRef = as.actorOf(Props(classOf[GameActor], scoreRouterRef), "game")

  "Game route" should {
    "start game" in {
      Get("/game/marcin/start") ~> gameRoute ~> check {
        status === StatusCodes.OK
        responseAs[String] should include("meta")
        responseAs[String] should include regex """"/game/marcin/challenge/.+""""
      }
    }
    "get stats about game progress" in {
      Get("/game/marcin/challenge") ~> gameRoute ~> check {
        status === StatusCodes.OK
      }
    }
  }

}
