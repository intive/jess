package com.blstream.jess
package api

import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.Flow
import core.score.ScoreService

trait Websocket {
  self: ScoreService =>
  import Directives._
  def wsRoute = path("stream") {
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
