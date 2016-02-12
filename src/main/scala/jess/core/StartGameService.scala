package com.blstream.jess
package core

import java.util.UUID

import akka.actor.Actor
import akka.actor.Actor.Receive

import scala.concurrent.{ Promise, Future }

trait StartGameService extends Actor {

  def startGame: String => Future[JessLink] =
    nick => {
      Promise[String]().success(UUID.randomUUID().toString).future
    }

  override def receive: Receive = ???
}
