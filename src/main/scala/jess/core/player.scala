package com.blstream.jess
package core.state

import akka.actor.Actor
import cats.data.State

final case class PlayerState(
  nick: String,
  points: Int = 0,
  attempts: Int = 0,
  challenge: Challenge
)

final case class Challenge(
  level: Int = 0,
  question: String,
  answer: String
)

sealed trait PlayerAction
case class StartGame(nick: String) extends PlayerAction
case class Answer(answer: String) extends PlayerAction

trait PlayerLogic {
  challengeService: ChallengeService =>

  val startGame: StartGame => State[PlayerState, Challenge] =
    start =>
      State(
        ps => {
          val ch = next(ps.challenge.level)
          (ps.copy(nick = start.nick, challenge = ch), ch)
        }
      )

  val answerChallenge: Answer => State[PlayerState, Challenge] =
    answer => State(ps => (ps, ps.challenge))

}

trait ChallengeService {
  def next: Int => Challenge = ???
}

class PlayerActor
    extends Actor
    with PlayerLogic
    with ChallengeService {

  var state: PlayerState

  def receive = {
    case sg@StartGame(nick) => {
      val (st,ch) = startGame(sg).run(state).value
      state = st
      sender ! ch
    }
  }

}