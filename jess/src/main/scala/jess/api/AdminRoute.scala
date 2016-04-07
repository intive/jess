package com.blstream.jess
package api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import core.ChallengeWithAnswer

import scala.concurrent.ExecutionContext

trait AdminRoute {

  gameService: GameService =>

  def adminRoute(implicit ec: ExecutionContext, timeout: Timeout): Route =
    pathPrefix("admin" / "challenge") {
      putAddChallenge
    }

  private def putAddChallenge(implicit ec: ExecutionContext, timeout: Timeout): Route =
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
