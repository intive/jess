package com.blstream.jess
package api

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import com.blstream.jess.core.GameActor
import org.scalatest.{ Matchers, WordSpec }
import concurrent.duration._
class GameRouteSpec
    extends WordSpec
    with GameRoute
    with Matchers
    with ScalatestRouteTest {

  implicit val as = ActorSystem("test")
  implicit val timeout = Timeout(5 seconds)
  val gameActorRef = as.actorOf(Props[GameActor], "game")

  "Game route" should {
    "start game" in {
      Get("/game/marcin/start") ~> gameRoute ~> check {
        status === StatusCodes.OK
      }
    }
    "get current challenge" in {
      Get("/game/marcin/challenge/current") ~> gameRoute ~> check {
        status === StatusCodes.OK
      }
    }
    "get current challenge by uuid, current or passed" in {
      Get("/game/marcin/challenge/A0EBD3B3-F2C7-443A-8192-AB23EF846A3D") ~> gameRoute ~> check {
        status === StatusCodes.OK
      }
    }
    "get stats about game progress" in {
      Get("/game/marcin/challenge") ~> gameRoute ~> check {
        status === StatusCodes.OK
      }
    }
  }

}