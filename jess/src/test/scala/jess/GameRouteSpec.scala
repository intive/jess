package com.blstream.jess
package api

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import akka.util.Timeout
import org.scalatest.{ Matchers, WordSpec }
import concurrent.duration._

import core.{ ChallengeActor, GameActor, AdminActor }
import core.score.ScoreRouter

class GameRouteSpec
    extends WordSpec
    with GameRoute
    with Matchers
    with ScalatestRouteTest {

  implicit val routeTestTimeout = RouteTestTimeout(5 seconds)

  implicit val as = ActorSystem("test")
  implicit val timeout = Timeout(5.seconds)
  val scoreRouterRef = as.actorOf(Props[ScoreRouter], "router")
  val challengeActorRef = as.actorOf(Props[ChallengeActor], "challenge")
  val gameActorRef = as.actorOf(Props(classOf[GameActor], scoreRouterRef, challengeActorRef), "game")
  val adminActorRef = as.actorOf(Props(classOf[AdminActor], challengeActorRef), "admin")

  "Game route" should {
    "start game" in {
      Put("/game/marcin/start") ~> gameRoute ~> check {
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
