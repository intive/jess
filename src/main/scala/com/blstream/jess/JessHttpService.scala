package com.blstream.jess

import akka.actor.Actor
import api.{GameRoute, HealthCheck}

class JessHttpService extends Actor
    with HealthCheck
    with GameRoute {

  implicit def actorRefFactory = context

  def receive = runRoute(
    healthCheckRoute ~
    gameRoute
  )

}
