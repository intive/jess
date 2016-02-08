package com.blstream.jess

import akka.actor.Actor
import api.HealthCheck

class HttpService extends Actor
    with HealthCheck {

  def actorRefFactory = context

  def receive = runRoute(
    healthCheckRoute
  )

}
