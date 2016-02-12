package com.blstream.jess
package api

import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.Flow

trait Websocket {
  import Directives._
  def wsRoute = path("ws" / "scores") {
    get {
      handleWebsocketMessages(echoService)
    }
  }

  private val echoService: Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(txt) => TextMessage(s"ECHO $txt")
    case _ => TextMessage("Message not supported")
  }
}
