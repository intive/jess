package com.blstream.jess
package api


import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import core.StartGameService

trait StartGame {
  self: StartGameService =>
  def startGameRoute(implicit ec: ExecutionContext): Route =
    path("start" / Segment) { nick =>
      put {
         complete {
          startGame(nick)
        }

      }
    }

}
