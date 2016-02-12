package jess
package api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import core.GameService

import scala.concurrent.ExecutionContext

trait StartGame {
  self: GameService =>
  def startGameRoute(implicit ec: ExecutionContext): Route =
    path("start" / Segment) { nick =>
      put {
        complete {
          startGame(nick)
        }
      }
    }
}
