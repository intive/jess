package com.blstream.jess
package api

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import org.scalatest.{ Matchers, WordSpec }
import concurrent.duration._
import spray.json._

import core.{ LinkGenerator, ChallengeService }
import core.score.ScoreRouter
import core.state.{ StartGameValidator, PlayerLogic, ChallengeWithAnswer }

class AdminRouteSpec
    extends WordSpec
    with AdminRoute
    with GameService
    with PlayerLogic
    with ChallengeService
    with LinkGenerator
    with StartGameValidator
    with Matchers
    with ScalatestRouteTest {

  implicit val as = ActorSystem("test")
  implicit val timeout = Timeout(5 seconds)
  val scoreRouterRef = as.actorOf(Props[ScoreRouter], "router")

  "Admin route" should {
    "add challenge" in {
      import ChallengeWithAnswerFormat._
      val chans = ChallengeWithAnswer("title", "description", "assignment", 1, None, "42")
      Put("/admin/challenge/add", chans) ~> adminRoute ~> check {
        status === StatusCodes.OK
        responseAs[String] should equal(chans.toJson.prettyPrint)
      }
    }
  }

}
