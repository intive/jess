package com.blstream.jess

import akka.actor.Actor
import api.{StartGame, HealthCheck}

class HttpService extends Actor
    with HealthCheck
    with StartGame {

  def actorRefFactory = context

  def receive = runRoute(
    healthCheckRoute ~
    startGameRoute
  )

}
