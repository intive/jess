package com.blstream.jess
package api

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout

import core.state.ChallengeWithAnswer
import core.AddChallenge

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
            (adminActorRef ? AddChallenge(chans)).mapTo[ChallengeWithAnswer]
          }
        }
      }
    }

  def adminActorRef: ActorRef

}
