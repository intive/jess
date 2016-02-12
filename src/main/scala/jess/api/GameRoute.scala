package jess
package api

import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import core.GameActor
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

trait GameRoute {

  def gameRoute(implicit system: ActorSystem, timeout: Timeout) =
    path("start" / Segment) { nick =>
      put {
        val gameActor = system.actorSelection("/user/game")
        val responseFuture = (gameActor ? GameActor.Start(nick, "good-token")).mapTo[String]
        complete { responseFuture }
      }
    }

}
