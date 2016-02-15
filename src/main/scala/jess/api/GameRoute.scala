package com.blstream.jess
package api

import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import core.{ PlayerActor, PlayersMap }
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

trait GameRoute {

  def startGameRoute(implicit system: ActorSystem, timeout: Timeout) =
    path("game" / Segment / "start") { nick =>
      put {
        val playerActor = PlayersMap.getPlayer(nick)
        val responseFuture = (playerActor ? PlayerActor.Start).mapTo[String]
        complete { responseFuture }
      }
    }

  def challangeRoute(implicit system: ActorSystem, timeout: Timeout) =
    path("game" / Segment / "challange") { nick =>
      put {
        entity(as[String]) { answear =>
          val playerActor = PlayersMap.getPlayer(nick)
          val responseFuture = (playerActor ? PlayerActor.Challenge(answear)).mapTo[String]
          complete { responseFuture }
        }
      }
    }

}
