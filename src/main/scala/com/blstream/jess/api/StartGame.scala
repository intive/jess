package com.blstream.jess
package api

import core.StartGameService
import spray.routing.HttpService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import ExecutionContext.Implicits.global

trait StartGame
  extends HttpService
  with StartGameService {

  lazy val startGameRoute =
    path("start" / Segment) { nick =>
      put {
        onComplete(startGame(nick)) {
          case Success(link) => complete(link)
          case Failure(ex) => complete(s"Error: $ex")
        }
      }
    }

}
