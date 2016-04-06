package com.blstream.jess
package api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import core.ChallengeWithAnswer

import scala.concurrent.ExecutionContext.Implicits.global

trait AdminRoute {

  gameService: GameService =>

  implicit val timeout: Timeout

  lazy val adminRoute: Route =
    pathPrefix("admin" / "challenge") {
      putAddChallenge
    }

  lazy val putAddChallenge: Route =
    put {
      path("add") {
        import ChallengeWithAnswerFormat._
        entity(as[ChallengeWithAnswer]) { chans =>
          complete {
            addChallengeIo(chans)
          }
        }
      }
    }

}
