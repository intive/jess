package com.blstream.jess
package api

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.http.scaladsl.server.Directives

import akka.stream.scaladsl.Flow
import core.score.{ ScoreService, ScorePublisher }

trait Websocket {
  self: ScoreService =>
  import Directives._

  def wsRoute(implicit scorePublisherRef: ScorePublisherRef) = path("stream") {
    get {
      handleWebsocketMessages(scoreFlow)
    }

  }

  /*  private val echoService: Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(txt) => TextMessage(s"ECHO $txt")
    case _ => TextMessage("Message not supported")
  }
 */
}
