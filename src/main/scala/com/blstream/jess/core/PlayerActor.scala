package com.blstream.jess
package core

import akka.actor.Actor

sealed trait PlayerMessages
case object Start extends PlayerMessages

//todo: make it persistent actor or store this somewhere
class PlayerActor extends Actor {

  var points: Long

  override def receive: Receive = {

    case Start => points = 0

  }

}
