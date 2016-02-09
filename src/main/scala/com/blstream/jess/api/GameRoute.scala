package com.blstream.jess
package api

import akka.actor.ActorContext
import akka.actor.Props
import akka.util.Timeout
import scala.concurrent.duration._
import core.StartGameService
import scala.util.Failure
import scala.util.Success
import spray.routing.HttpService
import core.GameActor
import akka.pattern.ask
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

trait GameRoute
  extends HttpService
  with StartGameService {

  implicit val timeout = Timeout(5 seconds)

  def gameRoute(implicit context: ActorContext) =
    path("start" / Segment) { nick =>
      put {
        val game = context.actorOf(Props[GameActor], nick)
        val responseFuture = (game ? GameActor.Start(nick, "good-token")).mapTo[String] 
        onComplete(responseFuture) {
          case Success(link: String) => complete(link)
          case Failure(ex) => complete(ex)
        }
      }
    }

}
