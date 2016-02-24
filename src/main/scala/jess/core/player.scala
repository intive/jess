package com.blstream.jess
package core.state

import akka.actor.Actor

import cats.SemigroupK
import cats.data.NonEmptyList
import cats.data.State
import cats.data.Validated
import cats.data.Xor
import cats.data.ValidatedNel
import cats.data.Validated._
import cats.syntax.cartesian._
import cats.syntax.xor._
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

sealed trait SomeError
final case object EmptyNickError extends SomeError
final case object AlreadyTakenNickError extends SomeError

trait NickValidator {

  implicit val nelSemigroup = SemigroupK[NonEmptyList].algebra[SomeError]

  val validate: String => ValidatedNel[SomeError, String] =
    nick =>
      (notEmpty(nick).toValidated.toValidatedNel
        |@| unique(nick).toValidated.toValidatedNel) map {
          (_, _) => nick
        }

  private val notEmpty: String => Xor[SomeError, String] =
    nick =>
      if (nick.isEmpty) EmptyNickError.left
      else nick.right

  private val unique: String => Xor[SomeError, String] =
    nick =>
      nick.right
}

trait PlayerLogic {
  self: ChallengeService with NickValidator =>

  val initGame: State[PlayerState, Unit] = State(ps => (ps, ()))

  val startGame: StartGame => ValidatedNel[SomeError, State[PlayerState, Challenge]] =
    start =>
      for {
        nick <- validate(start.nick)
      } yield {
        State(
          ps => {
            val ch = next(ps.challenge.level)
            (ps.copy(nick = Some(nick), challenge = ch), ch)
          }
        )
      }

  val answerChallenge: Answer => State[PlayerState, Challenge] =
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
      val foo = for {
        start <- startGame(sg)
      } yield {
        val (newState, ch) = start.run(state).value
        state = newState
        ch
      }
      sender ! foo
  }

}
