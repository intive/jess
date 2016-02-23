package com.blstream.jess
package core.state

import akka.actor.Actor

import cats.SemigroupK
import cats.data.NonEmptyList
import cats.data.State
import cats.data.Xor
import cats.data.ValidatedNel
import cats.data.Validated._
import cats.syntax.cartesian._
import cats.std.list._

final case class PlayerState(
  nick: Option[String],
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

sealed abstract class SomeError
final case object EmptyNickError extends SomeError
final case object AlreadyTakenNickError extends SomeError

trait NickValidator {

  implicit val nelSemigroup = SemigroupK[NonEmptyList].algebra[SomeError]

  val validate: StartGame => ValidatedNel[SomeError, StartGame] =
    start =>
      (notEmpty(start.nick) |@| unique(start.nick)) map {
        (_, _) => start
      }

  val notEmpty: String => ValidatedNel[SomeError, String] =
    nick =>
      if (nick.isEmpty) invalidNel(EmptyNickError)
      else valid(nick)

  val unique: String => ValidatedNel[SomeError, String] =
    nick =>
      valid(nick)
}

trait PlayerLogic {
  self: ChallengeService with NickValidator =>

  val initGame: PState[Unit] = State(ps => (ps, ()))

  type XorNel[E, A] = Xor[NonEmptyList[E], A]
  type PState[A] = State[PlayerState, A]

  val startGame: StartGame => XorNel[SomeError, PState[Challenge]] =
    validate(_).toXor.map { start =>
      State(
        ps => {
          val ch = next(ps.challenge.level)
          (ps.copy(nick = Some(start.nick), challenge = ch), ch)
        }
      )
    }

  val answerChallenge: Answer => PState[Challenge] =
    answer => State(ps => (ps, ps.challenge))

}

trait ChallengeService {
  def next: Int => Challenge = ???
}

class PlayerActor
    extends Actor
    with PlayerLogic
    with ChallengeService
    with NickValidator {

  var state: PlayerState = initGame.runS(PlayerState(nick = None, challenge = next(0))).value

  def receive = {
    case sg @ StartGame(_) =>
      (for {
        start <- startGame(sg)
      } yield {
        val (newState, ch) = start.run(state).value
        state = newState
        ch
      }).fold(
        err => sender ! err.unwrap,
        sender ! _
      )
  }

}
