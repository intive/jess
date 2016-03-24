package com.blstream.jess
package api

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import akka.util.Timeout
import org.scalatest.{ Matchers, WordSpec }
import concurrent.duration._
import spray.json._

import core.{ ChallengeActor, GameActor, AdminActor }
import core.score.ScoreRouter
import core.state.ChallengeWithAnswer

class AdminRouteSpec
    extends WordSpec
    with AdminRoute
    with Matchers
    with ScalatestRouteTest {

  implicit val timeout = Timeout(5.seconds)
  //implicit val routeTestTimeout = RouteTestTimeout(5 seconds)

  implicit val as = ActorSystem("test")
  val challengeActorRef = as.actorOf(Props[ChallengeActor], "challenge")
  val adminActorRef = as.actorOf(Props(classOf[AdminActor], challengeActorRef), "admin")

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
