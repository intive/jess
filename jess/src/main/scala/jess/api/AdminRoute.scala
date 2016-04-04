package com.blstream.jess
package api

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import core.GameActor.AddChallenge

import core.state.ChallengeWithAnswer

trait AdminRoute {

  implicit val timeout: Timeout

  lazy val adminRoute: Route =
    pathPrefix("admin" / "challenge") {
      addChallenge
    }

  lazy val addChallenge: Route =
    put {
      path("add") {
        import ChallengeWithAnswerFormat._
        entity(as[ChallengeWithAnswer]) { chans =>
          complete {
            (gameActorRef ? AddChallenge(chans)).mapTo[ChallengeWithAnswer]
          }
        }
      }
    }

  def gameActorRef: ActorRef

}
