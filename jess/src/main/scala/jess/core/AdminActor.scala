package com.blstream.jess
package core

import state.ChallengeWithAnswer

import akka.actor.ActorRef
import akka.actor.Actor
import akka.pattern.ask
import akka.pattern._
import akka.util.Timeout
import akka.util.Timeout

import scala.concurrent.duration._

sealed trait AdminCommands
case class AddChallenge(challenge: ChallengeWithAnswer) extends AdminCommands

class AdminActor(challengeActorRef: ActorRef) extends Actor {

  import context._
  implicit val timeout = Timeout(5 second)

  override def receive: Receive = {
    case ac @ AddChallenge(chans) => (challengeActorRef ? ac) pipeTo sender
  }

}
