package jess
package api

import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import scala.concurrent.duration._
import core.{ StartGameService, GameActor }
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

trait GameRoute {
  self: StartGameService =>

  def gameRoute(implicit ec: ExecutionContext, system: ActorSystem) =
    path("start" / Segment) { nick =>
      put {
        implicit val timeout = Timeout(5 seconds)
        val game = system.actorOf(Props[GameActor], nick)
        val responseFuture = (game ? GameActor.Start(nick, "good-token")).mapTo[String]
        onComplete(responseFuture) {
          case Success(link: String) => complete(link)
          case Failure(ex) => complete(ex)
        }
      }
    }

}
